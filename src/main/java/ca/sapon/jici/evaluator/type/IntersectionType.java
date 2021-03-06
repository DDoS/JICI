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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import ca.sapon.jici.evaluator.Substitutions;
import ca.sapon.jici.evaluator.member.Callable;
import ca.sapon.jici.evaluator.member.ClassVariable;
import ca.sapon.jici.evaluator.member.ConstructorCallable;
import ca.sapon.jici.evaluator.value.ValueKind;
import ca.sapon.jici.util.StringUtil;
import ca.sapon.jici.util.TypeUtil;

/**
 * An intersection of reference types, such as {@code String & Integer} or {@code Set<String> & Collection<CharSequence>}.
 */
public class IntersectionType implements ReferenceType, ComponentType, TypeArgument {
    public static final IntersectionType NOTHING = of(LiteralReferenceType.THE_OBJECT);
    public static final IntersectionType EVERYTHING = of(NullType.THE_NULL);
    private Set<SingleReferenceType> types;
    private boolean reduced = false;

    private IntersectionType(Set<SingleReferenceType> types) {
        if (types.size() < 1) {
            throw new UnsupportedOperationException("Expected at least one type");
        }
        this.types = types;
    }

    public Set<SingleReferenceType> getTypes() {
        if (!reduced) {
            types = TypeUtil.removeSuperTypes(types);
            reduced = true;
        }
        return types;
    }

    @Override
    public String getName() {
        final Set<SingleReferenceType> types = getTypes();
        if (types.size() == 1) {
            return types.iterator().next().toString();
        }
        return '(' + StringUtil.toString(types, " & ") + ')';
    }

    @Override
    public ValueKind getKind() {
        return ValueKind.OBJECT;
    }

    @Override
    public boolean isVoid() {
        return false;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public boolean isNumeric() {
        return false;
    }

    @Override
    public boolean isIntegral() {
        return false;
    }

    @Override
    public boolean isBoolean() {
        return false;
    }

    @Override
    public boolean isArray() {
        for (SingleReferenceType type : getTypes()) {
            if (!type.isArray()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isReference() {
        return true;
    }

    @Override
    public boolean isReifiable() {
        return false;
    }

    @Override
    public boolean contains(TypeArgument other) {
        return equals(other);
    }

    @Override
    public boolean convertibleTo(Type to) {
        for (SingleReferenceType bound : getTypes()) {
            if (bound.convertibleTo(to)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public IntersectionType asArray(int dimensions) {
        final Set<SingleReferenceType> componentTypes = new LinkedHashSet<>();
        for (SingleReferenceType type : getTypes()) {
            componentTypes.add(type.asArray(dimensions));
        }
        return of(componentTypes);
    }

    @Override
    public Object newArray(int length) {
        throw new UnsupportedOperationException("Cannot instantiate an array from an intersection type");
    }

    @Override
    public Object newArray(int[] lengths) {
        throw new UnsupportedOperationException("Cannot instantiate an array from an intersection type");
    }

    @Override
    public IntersectionType getComponentType() {
        if (!isArray()) {
            throw new UnsupportedOperationException("Not an array type");
        }
        final Set<SingleReferenceType> componentTypes = new LinkedHashSet<>();
        for (SingleReferenceType type : getTypes()) {
            final ComponentType componentType = type.getComponentType();
            if (!(componentType instanceof SingleReferenceType)) {
                throw new UnsupportedOperationException("Cannot have an intersection with a primitive type");
            }
            componentTypes.add((SingleReferenceType) componentType);
        }
        return of(componentTypes);
    }

    @Override
    public LiteralReferenceType getInnerClass(String name, TypeArgument[] typeArguments) {
        LiteralReferenceType candidate = null;
        for (SingleReferenceType type : getTypes()) {
            final LiteralReferenceType innerClass;
            try {
                innerClass = type.getInnerClass(name, typeArguments);
            } catch (UnsupportedOperationException exception) {
                continue;
            }
            if (candidate == null) {
                candidate = innerClass;
            } else {
                throw new UnsupportedOperationException("Inner class for name " + name + " in type " + getName() +
                        " is ambiguous: both " + candidate + " and " + innerClass + " match");
            }
        }
        if (candidate == null) {
            throw new UnsupportedOperationException("No inner class for name " + name + " in " + getName());
        }
        return candidate;
    }

    @Override
    public ConstructorCallable getConstructor(TypeArgument[] typeArguments, Type[] arguments) {
        ConstructorCallable candidate = null;
        for (SingleReferenceType type : getTypes()) {
            final ConstructorCallable constructor;
            try {
                constructor = type.getConstructor(typeArguments, arguments);
            } catch (UnsupportedOperationException exception) {
                continue;
            }
            if (candidate == null) {
                candidate = constructor;
            } else {
                throw new UnsupportedOperationException("Constructor for signature" +
                        (typeArguments.length > 0 ? '<' + StringUtil.toString(typeArguments, ", ") + '>' : "") +
                        "init(" + StringUtil.toString(Arrays.asList(arguments), ", ") + ")" +
                        " in type " + getName() + " is ambiguous: both " +
                        candidate + " and " + constructor + " match");
            }
        }
        if (candidate == null) {
            throw new UnsupportedOperationException("No constructor for signature: " +
                    (typeArguments.length > 0 ? '<' + StringUtil.toString(typeArguments, ", ") + '>' : "") +
                    "init(" + StringUtil.toString(Arrays.asList(arguments), ", ") + ") in " + getName());
        }
        return candidate;
    }

    @Override
    public ClassVariable getField(String name) {
        ClassVariable candidate = null;
        for (SingleReferenceType type : getTypes()) {
            final ClassVariable field;
            try {
                field = type.getField(name);
            } catch (UnsupportedOperationException ignored) {
                continue;
            }
            if (candidate == null) {
                candidate = field;
            } else {
                throw new UnsupportedOperationException("Field for name" + name + " in type " + getName() +
                        " is ambiguous: both " + candidate + " and " + field + " match");
            }
        }
        throw new UnsupportedOperationException("No field named " + name + " in " + getName());
    }

    @Override
    public Callable getMethod(String name, TypeArgument[] typeArguments, Type[] arguments) {
        Callable candidate = null;
        for (SingleReferenceType type : getTypes()) {
            final Callable method;
            try {
                method = type.getMethod(name, typeArguments, arguments);
            } catch (UnsupportedOperationException exception) {
                continue;
            }
            if (candidate == null) {
                candidate = method;
            } else {
                throw new UnsupportedOperationException("Method for signature" +
                        (typeArguments.length > 0 ? '<' + StringUtil.toString(typeArguments, ", ") + '>' : "") +
                        name + "(" + StringUtil.toString(Arrays.asList(arguments), ", ") + ")" +
                        " in type " + getName() + " is ambiguous: both " +
                        candidate + " and " + method + " match");
            }
        }
        if (candidate == null) {
            throw new UnsupportedOperationException("No method for signature: " +
                    (typeArguments.length > 0 ? '<' + StringUtil.toString(typeArguments, ", ") + '>' : "") +
                    name + "(" + StringUtil.toString(Arrays.asList(arguments), ", ") + ") in " + getName());
        }
        return candidate;
    }

    @Override
    public Set<LiteralReferenceType> getDirectSuperTypes() {
        final Set<LiteralReferenceType> directSuperTypes = new HashSet<>();
        for (SingleReferenceType type : getTypes()) {
            if (type instanceof LiteralReferenceType) {
                directSuperTypes.add((LiteralReferenceType) type);
            } else {
                directSuperTypes.addAll(type.getDirectSuperTypes());
            }
        }
        return directSuperTypes;
    }

    @Override
    public Set<LiteralReferenceType> getSuperTypes() {
        final Set<LiteralReferenceType> result = new HashSet<>();
        for (LiteralReferenceType type : getDirectSuperTypes()) {
            result.addAll(type.getSuperTypes());
        }
        return result;
    }

    @Override
    public IntersectionType getErasure() {
        final Set<SingleReferenceType> erasures = new LinkedHashSet<>();
        for (SingleReferenceType type : getTypes()) {
            erasures.add(type.getErasure());
        }
        return of(erasures);
    }

    @Override
    public boolean isUncheckedConversion(Type to) {
        for (SingleReferenceType bound : getTypes()) {
            if (bound.isUncheckedConversion(to)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public IntersectionType capture() {
        return this;
    }

    @Override
    public IntersectionType substituteTypeVariables(Substitutions substitution) {
        final Set<ReferenceType> newIntersection = new LinkedHashSet<>();
        for (SingleReferenceType type : getTypes()) {
            if (type instanceof TypeArgument) {
                // Apply to type arguments
                final TypeArgument typeArgument = ((TypeArgument) type).substituteTypeVariables(substitution);
                if (!(typeArgument instanceof ReferenceType)) {
                    throw new UnsupportedOperationException("Substitution " + substitution + " on " + type +
                            " doesn't result in a reference type, instead it is " + typeArgument);
                }
                newIntersection.add((ReferenceType) typeArgument);
            } else {
                // Any other member gets no substitution
                newIntersection.add(type);
            }
        }
        return of(newIntersection);
    }

    @Override
    public Set<TypeVariable> getTypeVariables() {
        final Set<TypeVariable> typeVariables = new HashSet<>();
        for (SingleReferenceType type : getTypes()) {
            if ((type instanceof TypeArgument)) {
                typeVariables.addAll(((TypeArgument) type).getTypeVariables());
            }
        }
        return typeVariables;
    }

    public SingleReferenceType checkIfValidUpperBound() {
        final Set<LiteralReferenceType> interfaces = new LinkedHashSet<>();
        SingleReferenceType nonInterface = null;
        SingleReferenceType firstInterface = null;
        // Check for a single non interface type in the upper bound
        for (SingleReferenceType type : getTypes()) {
            if (type instanceof LiteralReferenceType) {
                final LiteralReferenceType literalReferenceType = (LiteralReferenceType) type;
                if (literalReferenceType.isInterface()) {
                    interfaces.add(literalReferenceType);
                    if (firstInterface == null) {
                        firstInterface = literalReferenceType;
                    }
                    continue;
                }
            }
            if (nonInterface != null) {
                throw new UnsupportedOperationException("Cannot have more than one non-interface type in the upper bound, found: " + nonInterface + " and " + type);
            }
            nonInterface = type;
        }
        // Check that the interfaces don't have super types with the same erasure but different parametrizations
        if (TypeUtil.haveDifferentInvocationsInSuperTypes(interfaces)) {
            throw new UnsupportedOperationException("Upper bound has some interface types who's super types have the same erasure but different parametrizations");
        }
        return nonInterface == null ? firstInterface : nonInterface;
    }

    public boolean is(SingleReferenceType type) {
        return getTypes().size() == 1 && has(type);
    }

    public boolean has(SingleReferenceType type) {
        return getTypes().contains(type);
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof IntersectionType && getTypes().equals(((IntersectionType) other).getTypes());
    }

    @Override
    public int hashCode() {
        return getTypes().hashCode();
    }

    public static IntersectionType of(ReferenceType... types) {
        return of(Arrays.asList(types));
    }

    public static IntersectionType of(Collection<? extends ReferenceType> types) {
        return of(TypeUtil.expandIntersectionTypes(types));
    }

    public static IntersectionType of(Set<SingleReferenceType> types) {
        return new IntersectionType(types);
    }
}
