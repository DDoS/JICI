/*
 * This file is part of JICI, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015-2015 Aleksi Sapon <http://sapon.ca/jici/>
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import ca.sapon.jici.evaluator.Substitutions;
import ca.sapon.jici.util.StringUtil;
import ca.sapon.jici.util.TypeUtil;

/**
 * A type that takes type arguments, such as {@code Set<T>} or {@code Map<String, Integer>}.
 */
public class ParametrizedType extends LiteralReferenceType {
    private static final AtomicInteger CAPTURE_COUNTER = new AtomicInteger(1);
    private final LiteralReferenceType erased;
    private final List<TypeArgument> arguments;
    // Cache these to prevent them from being created every time
    private TypeVariable[] parameters = null;
    private Substitutions substitutions = null;

    private ParametrizedType(LiteralReferenceType erased, List<TypeArgument> arguments) {
        super(erased.getTypeClass());
        if (arguments.size() <= 0) {
            throw new IllegalArgumentException("Expected at least one type argument");
        }
        this.erased = erased;
        this.arguments = arguments;
    }

    public List<TypeArgument> getArguments() {
        return arguments;
    }

    @Override
    public String getName() {
        Class<?> _class = erased.getTypeClass();
        int dimensions = 0;
        while (_class.isArray()) {
            _class = _class.getComponentType();
            dimensions++;
        }
        return _class.getCanonicalName() + '<' + StringUtil.toString(arguments, ", ") + '>' + StringUtil.repeat("[]", dimensions);
    }

    @Override
    public boolean isReifiable() {
        for (TypeArgument argument : arguments) {
            if (!(argument instanceof WildcardType) || !((WildcardType) argument).isUnbounded()) {
                return false;
            }
        }
        return true;
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
        final ComponentType componentType = erased.getComponentType();
        if (!(componentType instanceof LiteralReferenceType)) {
            throw new UnsupportedOperationException("Component type is not a literal reference type");
        }
        return new ParametrizedType((LiteralReferenceType) componentType, arguments);
    }

    @Override
    public ParametrizedType substituteTypeVariables(Substitutions substitution) {
        final List<TypeArgument> newArguments = new ArrayList<>();
        for (TypeArgument type : arguments) {
            if (type instanceof TypeVariable) {
                // For type variables, substitute if the name matches, else apply recursively and add
                final TypeVariable typeVariable = (TypeVariable) type;
                final TypeArgument typeArgument = substitution.forVariable(typeVariable);
                if (typeArgument != null) {
                    newArguments.add(typeArgument);
                } else {
                    newArguments.add(typeVariable.substituteTypeVariables(substitution));
                }
            } else {
                // Apply recursively to other members
                newArguments.add(type.substituteTypeVariables(substitution));
            }
        }
        return new ParametrizedType(erased, newArguments);
    }

    @Override
    public Set<TypeVariable> getTypeVariables() {
        final Iterator<TypeArgument> iterator = arguments.iterator();
        final Set<TypeVariable> typeVariables = iterator.next().getTypeVariables();
        while (iterator.hasNext()) {
            typeVariables.addAll(iterator.next().getTypeVariables());
        }
        return typeVariables;
    }

    @Override
    public ParametrizedType asArray(int dimensions) {
        return new ParametrizedType(erased.asArray(dimensions), arguments);
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

    @Override
    public LiteralReferenceType getDirectSuperClass() {
        final LiteralReferenceType superType = erased.getDirectSuperClass();
        if (superType instanceof ParametrizedType) {
            return ((ParametrizedType) superType).substituteTypeVariables(getSubstitutions());
        }
        return superType;
    }

    @Override
    public LiteralReferenceType[] getDirectlyImplementedInterfaces() {
        final LiteralReferenceType[] interfaces = erased.getDirectlyImplementedInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            final LiteralReferenceType _interface = interfaces[i];
            if (_interface instanceof ParametrizedType) {
                interfaces[i] = ((ParametrizedType) _interface).substituteTypeVariables(getSubstitutions());
            }
        }
        return interfaces;
    }

    @Override
    public ParametrizedType capture(AtomicInteger idCounter) {
        final ArrayList<TypeArgument> capturedArguments = new ArrayList<>();
        final TypeVariable[] parameters = getParameters();
        // Generate substitutions for the parameters, so we can get the dependency ordering
        final Map<String, TypeArgument> namesToArguments = new HashMap<>();
        for (TypeVariable parameter : parameters) {
            namesToArguments.put(parameter.getDeclaredName(), parameter.getUpperBound());
            // Fill the captured arguments with null placeholders so set(index, value) will work
            capturedArguments.add(null);
        }
        final Substitutions substitutions = new Substitutions(namesToArguments);
        // Following the dependency ordering, substitute the wildcards by type variables
        for (String name : substitutions.getOrder()) {
            final int index = getParameterIndex(name);
            final TypeArgument argument = arguments.get(index);
            if (argument instanceof WildcardType) {
                // Capture of a wildcard type replaces it with a unique type variable and merges the bounds (after substitution) from the declaration
                final WildcardType wildcard = (WildcardType) argument;
                final Set<SingleReferenceType> upperBound = new HashSet<>(parameters[index].getUpperBound().substituteTypeVariables(substitutions).getTypes());
                upperBound.addAll(wildcard.getUpperBound().getTypes());
                final TypeVariable capturedArgument = TypeVariable.of("CAP#" + idCounter.getAndIncrement(), wildcard.getLowerBound(), IntersectionType.of(upperBound));
                capturedArguments.set(index, capturedArgument);
                // Replace the parameter by the captured arguments in the substitution too
                namesToArguments.put(name, capturedArgument);
            } else {
                // Capture of any other type does nothing
                capturedArguments.set(index, argument);
            }
        }
        // Create a new parametrized type with the new arguments
        return new ParametrizedType(erased, capturedArguments);
    }

    private int getParameterIndex(String name) {
        final TypeVariable[] parameters = getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getDeclaredName().equals(name)) {
                return i;
            }
        }
        throw new UnsupportedOperationException("Not a parameter: " + name);
    }

    private TypeVariable[] getParameters() {
        if (this.parameters == null) {
            final java.lang.reflect.TypeVariable<?>[] parameters = erased.getTypeParameters();
            this.parameters = new TypeVariable[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                this.parameters[i] = (TypeVariable) TypeUtil.wrap(parameters[i]);
            }
        }
        return this.parameters;
    }

    private Substitutions getSubstitutions() {
        if (this.substitutions == null) {
            final Map<String, TypeArgument> namesToArguments = new HashMap<>();
            final TypeVariable[] parameters = getParameters();
            for (int i = 0; i < parameters.length; i++) {
                namesToArguments.put(parameters[i].getDeclaredName(), arguments.get(i));
            }
            this.substitutions = new Substitutions(namesToArguments);
        }
        return this.substitutions;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof ParametrizedType) {
            final ParametrizedType that = (ParametrizedType) other;
            return erased.equals(that.erased) && arguments.equals(that.arguments);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + arguments.hashCode();
    }

    public static ParametrizedType of(Class<?> raw, List<TypeArgument> arguments) {
        return of(LiteralReferenceType.of(raw), arguments);
    }

    public static ParametrizedType of(LiteralReferenceType raw, List<TypeArgument> arguments) {
        if (arguments.size() < 1) {
            throw new UnsupportedOperationException("Expected at least one type argument");
        }
        final ParametrizedType type = new ParametrizedType(raw, arguments);
        final TypeVariable[] parameters = type.getParameters();
        if (parameters.length < 1) {
            throw new UnsupportedOperationException("Not a generic type: " + raw);
        }
        if (parameters.length != arguments.size()) {
            throw new UnsupportedOperationException("Mismatch in type parameter and argument count: " + parameters.length + " != " + arguments.size());
        }
        // The captured arguments must be subtypes of the parameter upper bounds resulting from substitution
        final ParametrizedType capture = type.capture(CAPTURE_COUNTER);
        final List<TypeArgument> capturedArguments = capture.getArguments();
        final Substitutions substitutions = capture.getSubstitutions();
        for (int i = 0; i < parameters.length; i++) {
            final IntersectionType parameterUpperBound = parameters[i].getUpperBound().substituteTypeVariables(substitutions);
            final TypeArgument capturedArgument = capturedArguments.get(i);
            if (!capturedArgument.convertibleTo(parameterUpperBound)) {
                throw new UnsupportedOperationException("Cannot convert argument " + capturedArgument + " to " + parameterUpperBound);
            }
        }
        return type;
    }
}
