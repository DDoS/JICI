/*
 * This file is part of JICI, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015-2016 Aleksi Sapon <http://sapon.ca/jici/>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package ca.sapon.jici.evaluator.type;

import java.lang.reflect.GenericDeclaration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.sapon.jici.evaluator.Substitutions;
import ca.sapon.jici.util.IntegerCounter;
import ca.sapon.jici.util.StringUtil;

/**
 * A type that takes type arguments, such as {@code Set<T>}, {@code Map<? extends CharSequence, Integer>} or {@code Map.Entry<String, Integer>}.
 */
public class ParametrizedType extends LiteralReferenceType {
    private final ParametrizedType owner;
    private final LiteralReferenceType erased;
    private final List<TypeArgument> arguments;
    private final TypeVariable[] parameters;
    // Cache these to prevent them from being created every time
    private Substitutions substitutions = null;
    private ParametrizedType capture = null;
    private Class<?> baseComponentType = null;
    private int dimensions = -1;

    protected ParametrizedType(ParametrizedType owner, LiteralReferenceType raw, List<TypeArgument> arguments, TypeVariable[] parameters) {
        super(raw.getTypeClass());
        this.owner = owner;
        if (arguments.size() <= 0 && owner == null) {
            throw new IllegalArgumentException("Expected at least one type argument");
        }
        erased = raw;
        this.arguments = arguments;
        this.parameters = parameters;
    }

    public List<TypeArgument> getArguments() {
        return arguments;
    }

    public ParametrizedType getOwner() {
        return owner;
    }

    @Override
    public String getName() {
        if (baseComponentType == null) {
            baseComponentType = getTypeClass();
            dimensions = 0;
            while (baseComponentType.isArray()) {
                baseComponentType = baseComponentType.getComponentType();
                dimensions++;
            }
        }
        return (owner != null ? owner.getName() + '.' + baseComponentType.getSimpleName() : baseComponentType.getCanonicalName())
                + (arguments.isEmpty() ? "" : '<' + StringUtil.toString(arguments, ", ") + '>') + StringUtil.repeat("[]", dimensions);
    }

    @Override
    public boolean isReifiable() {
        for (TypeArgument argument : arguments) {
            if (!(argument instanceof WildcardType) || !((WildcardType) argument).isUnbounded()) {
                return false;
            }
        }
        return owner == null || owner.isReifiable();
    }

    @Override
    public boolean isRaw() {
        return false;
    }

    @Override
    public LiteralReferenceType getErasure() {
        return erased;
    }

    @Override
    public ParametrizedType getComponentType() {
        final ComponentType componentType = super.getComponentType();
        if (!(componentType instanceof LiteralReferenceType)) {
            throw new UnsupportedOperationException("Component type is not a literal reference type");
        }
        return new ParametrizedType(owner, (LiteralReferenceType) componentType, arguments, parameters);
    }

    @Override
    public ParametrizedType substituteTypeVariables(Substitutions substitution) {
        // Apply to owner if any
        final ParametrizedType substitutedOwner = owner != null ? owner.substituteTypeVariables(substitution) : null;
        final List<TypeArgument> newArguments = new ArrayList<>();
        for (TypeArgument type : arguments) {
            newArguments.add(type.substituteTypeVariables(substitution));
        }
        return new ParametrizedType(substitutedOwner, erased, newArguments, parameters);
    }

    @Override
    public Set<TypeVariable> getTypeVariables() {
        final Iterator<TypeArgument> iterator = arguments.iterator();
        if (!iterator.hasNext()) {
            return new HashSet<>();
        }
        final Set<TypeVariable> typeVariables = iterator.next().getTypeVariables();
        while (iterator.hasNext()) {
            typeVariables.addAll(iterator.next().getTypeVariables());
        }
        if (owner != null) {
            typeVariables.addAll(owner.getTypeVariables());
        }
        return typeVariables;
    }

    @Override
    public ParametrizedType asArray(int dimensions) {
        return new ParametrizedType(owner, super.asArray(dimensions), arguments, parameters);
    }

    @Override
    public boolean contains(TypeArgument other) {
        return equals(other);
    }

    public boolean argumentsContain(List<TypeArgument> otherArguments) {
        if (arguments.size() != otherArguments.size()) {
            return false;
        }
        for (int i = 0; i < arguments.size(); i++) {
            if (!arguments.get(i).contains(otherArguments.get(i))) {
                return false;
            }
        }
        return true;
    }

    public boolean provablyDistinct(ParametrizedType other) {
        // Distinct if the erasures are different (parametrizations of different types)
        if (!getErasure().equals(other.getErasure())) {
            return true;
        }
        // Else get the arguments from the captures of each
        final List<TypeArgument> thisArgs = capture().getArguments();
        final List<TypeArgument> otherArgs = other.capture().getArguments();
        // Compare arguments pairwise
        for (int i = 0; i < thisArgs.size(); i++) {
            final TypeArgument thisArg = thisArgs.get(i);
            final TypeArgument otherArg = otherArgs.get(i);
            // Must hold for any argument pair
            if (!(thisArg instanceof BoundedType) && !(otherArg instanceof BoundedType)) {
                // If neither are bounded (wildcard or type variable), they must not be the same
                if (!thisArg.equals(otherArg)) {
                    return true;
                }
            } else {
                // If either or both are bounded
                ReferenceType left;
                ReferenceType right;
                // Get the upper bound if bounded, else use the type itself
                if (thisArg instanceof BoundedType) {
                    left = ((BoundedType) thisArg).getUpperBound();
                } else {
                    left = (ReferenceType) thisArg;
                }
                if (otherArg instanceof BoundedType) {
                    right = ((BoundedType) otherArg).getUpperBound();
                } else {
                    right = (ReferenceType) otherArg;
                }
                // The erasures must not be subtypes of each other
                left = left.getErasure();
                right = right.getErasure();
                if (!left.convertibleTo(right) && !right.convertibleTo(left)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public LiteralReferenceType getDirectSuperClass() {
        final LiteralReferenceType superType = super.getDirectSuperClass();
        if (superType instanceof ParametrizedType) {
            return ((ParametrizedType) superType).substituteTypeVariables(capture().getSubstitutions());
        }
        return superType;
    }

    @Override
    public LiteralReferenceType[] getDirectlyImplementedInterfaces() {
        final LiteralReferenceType[] interfaces = super.getDirectlyImplementedInterfaces();
        final Substitutions substitutions = capture().getSubstitutions();
        for (int i = 0; i < interfaces.length; i++) {
            final LiteralReferenceType _interface = interfaces[i];
            if (_interface instanceof ParametrizedType) {
                interfaces[i] = ((ParametrizedType) _interface).substituteTypeVariables(substitutions);
            }
        }
        return interfaces;
    }

    @Override
    public ParametrizedType capture() {
        // Check the cache first
        if (capture != null) {
            return capture;
        }
        // Else compute it
        return capture(new IntegerCounter());
    }

    private ParametrizedType capture(IntegerCounter idCounter) {
        // Capture the owner first, if any
        final ParametrizedType ownerCapture = owner != null ? owner.capture(idCounter) : null;
        // The declaror of the capture type variables is this generic type
        final GenericDeclaration declaror = erased.getTypeClass();
        // Now we perform the actual capture conversion
        final List<TypeArgument> capturedArguments = new ArrayList<>();
        final Map<String, TypeArgument> namesToArguments = new HashMap<>();
        for (int i = 0; i < parameters.length; i++) {
            final TypeArgument argument = arguments.get(i);
            final String name = parameters[i].getDeclaredName();
            if (argument instanceof WildcardType) {
                // Capture of a wildcard type replaces it with a unique type variable and merges the bounds from the declaration
                final WildcardType wildcard = (WildcardType) argument;
                final Set<SingleReferenceType> upperBound = new HashSet<>(parameters[i].getUpperBound().getTypes());
                upperBound.addAll(wildcard.getUpperBound().getTypes());
                // Create the capture argument
                final TypeVariable capturedArgument = TypeVariable.of("CAP#" + idCounter.nextValue(), wildcard.getLowerBound(), IntersectionType.of(upperBound), declaror, true);
                capturedArguments.add(capturedArgument);
                // Update the substitutions, unless we have a cycle, so we preserve the cycle-breaking substitution added earlier
                namesToArguments.put(name, capturedArgument);
            } else {
                // Capture of any other type does nothing
                capturedArguments.add(argument);
                // Update the substitution
                namesToArguments.put(name, argument);
            }
        }
        // Generate substitutions for the parameters, so we can get the dependency ordering
        final Map<String, TypeArgument> namesToParameters = new HashMap<>();
        for (TypeVariable parameter : parameters) {
            namesToParameters.put(parameter.getDeclaredName(), parameter.getUpperBound());
        }
        final List<String> ordering = new Substitutions(namesToParameters).getOrder();
        // Add the owner capture results if any, since the inner type might be dependent
        if (ownerCapture != null) {
            namesToArguments.putAll(ownerCapture.getSubstitutions().getMap());
        }
        // If a name isn't in the ordering then it is cyclical, so add it without bounds if it is type variable (to break the cycles)
        for (int i = 0; i < parameters.length; i++) {
            final TypeVariable parameter = parameters[i];
            final String name = parameter.getDeclaredName();
            final TypeArgument argument = capturedArguments.get(i);
            if (!ordering.contains(name) && argument instanceof TypeVariable) {
                namesToArguments.put(name, ((TypeVariable) argument).withoutBounds());
            }
        }
        // Now perform the substitutions in the generated bounds to remove all traces of the parameters
        final Substitutions substitutions = new Substitutions(namesToArguments);
        for (int i = 0; i < capturedArguments.size(); i++) {
            final TypeArgument argument = capturedArguments.get(i);
            final TypeArgument substitutedArgument = argument.substituteTypeVariables(substitutions);
            capturedArguments.set(i, substitutedArgument);
            if (substitutedArgument instanceof TypeVariable) {
                ((TypeVariable) substitutedArgument).checkBounds();
            }
        }
        // Create a new parametrized type with the new arguments and cache it
        return capture = new ParametrizedType(ownerCapture, erased, capturedArguments, this.parameters);
    }

    @Override
    public Substitutions getSubstitutions() {
        if (this.substitutions == null) {
            final Map<String, TypeArgument> namesToArguments = new HashMap<>();
            for (int i = 0; i < parameters.length; i++) {
                namesToArguments.put(parameters[i].getDeclaredName(), arguments.get(i));
            }
            if (owner != null) {
                namesToArguments.putAll(owner.getSubstitutions().getMap());
            }
            this.substitutions = new Substitutions(namesToArguments);
        }
        return this.substitutions;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || super.equals(other) && other instanceof ParametrizedType
                && arguments.equals(((ParametrizedType) other).arguments);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + arguments.hashCode();
    }

    public static ParametrizedType of(Class<?> raw, List<TypeArgument> arguments) {
        return of(null, raw, arguments);
    }

    public static ParametrizedType of(LiteralReferenceType raw, List<TypeArgument> arguments) {
        return of(null, raw, arguments);
    }

    public static ParametrizedType of(ParametrizedType owner, Class<?> raw, List<TypeArgument> arguments) {
        return of(owner, LiteralReferenceType.of(raw), arguments);
    }

    public static ParametrizedType of(ParametrizedType owner, LiteralReferenceType raw, List<TypeArgument> arguments) {
        if (arguments.size() < 1 && owner == null) {
            throw new UnsupportedOperationException("Expected at least one type argument or a parametrized owner");
        }
        final TypeVariable[] parameters = TypeCache.wrapTypeVariables(raw.getTypeParameters());
        final ParametrizedType type = new ParametrizedType(checkOwner(owner, raw.getTypeClass(), arguments), raw, arguments, parameters);
        if (parameters.length < 1 && owner == null) {
            throw new UnsupportedOperationException("Not a generic type: " + raw);
        }
        if (parameters.length != arguments.size()) {
            throw new UnsupportedOperationException("Mismatch in type parameter and argument count: " + parameters.length + " != " + arguments.size());
        }
        // The captured arguments must be contained in the parameter bound resulting from substitution
        final ParametrizedType capture = type.capture();
        final List<TypeArgument> capturedArguments = capture.getArguments();
        final Substitutions substitutions = capture.getSubstitutions();
        for (int i = 0; i < parameters.length; i++) {
            final TypeVariable substitutedParameter = parameters[i].substituteBoundTypeVariables(substitutions);
            final TypeArgument capturedArgument = capturedArguments.get(i);
            // Try to detect cycles where the upper bound of a parameter is the type itself (and has a contained lower bound)
            if (substitutedParameter.getUpperBound().isOnly(capture) && substitutedParameter.getLowerBound().convertibleTo(capturedArgument)) {
                continue;
            }
            if (!substitutedParameter.boundsContain(capturedArgument)) {
                throw new UnsupportedOperationException("Cannot assign type argument " + capturedArgument + " to " + substitutedParameter);
            }
        }
        return type;
    }
}
