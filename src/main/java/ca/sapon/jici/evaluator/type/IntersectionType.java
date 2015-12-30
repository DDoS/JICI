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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ca.sapon.jici.evaluator.Accessible;
import ca.sapon.jici.evaluator.Callable;
import ca.sapon.jici.evaluator.value.ValueKind;
import ca.sapon.jici.util.StringUtil;
import ca.sapon.jici.util.TypeUtil;

/**
 * An intersection of reference types, such as {@code String & Integer} or {@code Set<String> & Collection<CharSequence>}.
 */
public class IntersectionType implements ReferenceType, ComponentType, TypeArgument {
    public static final IntersectionType NOTHING = of(LiteralReferenceType.THE_OBJECT);
    public static final IntersectionType EVERYTHING = of(NullType.THE_NULL);
    private final Set<SingleReferenceType> types;

    private IntersectionType(Set<SingleReferenceType> intersection) {
        if (intersection.size() < 1) {
            throw new UnsupportedOperationException("Expected at least one type");
        }
        types = TypeUtil.removeSuperTypes(intersection);
    }

    public Set<SingleReferenceType> getTypes() {
        return types;
    }

    @Override
    public String getName() {
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
        for (SingleReferenceType type : types) {
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
        for (SingleReferenceType bound : types) {
            if (bound.convertibleTo(to)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public IntersectionType asArray(int dimensions) {
        final Set<SingleReferenceType> componentTypes = new HashSet<>();
        for (SingleReferenceType type : types) {
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
        final Set<SingleReferenceType> componentTypes = new HashSet<>();
        for (SingleReferenceType type : types) {
            final ComponentType componentType = type.getComponentType();
            if (!(componentType instanceof SingleReferenceType)) {
                throw new UnsupportedOperationException("Cannot have an intersection with a primitive type");
            }
            componentTypes.add((SingleReferenceType) componentType);
        }
        return of(componentTypes);
    }

    @Override
    public Callable getConstructor(Type[] arguments) {
        for (SingleReferenceType type : types) {
            try {
                return type.getConstructor(arguments);
            } catch (UnsupportedOperationException ignored) {
            }
        }
        throw new UnsupportedOperationException("No constructor for signature: "
                + "(" + StringUtil.toString(Arrays.asList(arguments), ", ") + ") in " + getName());
    }

    @Override
    public Accessible getField(String name) {
        for (SingleReferenceType type : types) {
            try {
                return type.getField(name);
            } catch (UnsupportedOperationException ignored) {
            }
        }
        throw new UnsupportedOperationException("No field named " + name + " in " + getName());
    }

    @Override
    public Callable getMethod(String name, Type[] arguments) {
        for (SingleReferenceType type : types) {
            try {
                return type.getMethod(name, arguments);
            } catch (UnsupportedOperationException ignored) {
            }
        }
        throw new UnsupportedOperationException("No method for signature: "
                + name + "(" + StringUtil.toString(Arrays.asList(arguments), ", ") + ") in " + getName());
    }

    @Override
    public Set<SingleReferenceType> getDirectSuperTypes() {
        return types;
    }

    @Override
    public IntersectionType substituteTypeVariables(Map<String, TypeArgument> namesToValues) {
        final Set<ReferenceType> newIntersection = new HashSet<>();
        for (SingleReferenceType type : types) {
            if (type instanceof TypeVariable) {
                // For type variables, substitute if the name matches, else apply recursively and add
                final TypeVariable typeVariable = (TypeVariable) type;
                final TypeArgument substitution = namesToValues.get(typeVariable.getDeclaredName());
                if (substitution != null) {
                    if (!(substitution instanceof ReferenceType)) {
                        throw new UnsupportedOperationException("Substitution is not a reference type: " + typeVariable + " -> " + substitution);
                    }
                    newIntersection.add((ReferenceType) substitution);
                } else {
                    newIntersection.add(typeVariable.substituteTypeVariables(namesToValues));
                }
            } else if (type instanceof LiteralReferenceType) {
                // Apply recursively to other reference type members
                newIntersection.add(((LiteralReferenceType) type).substituteTypeVariables(namesToValues));
            } else {
                // Any other member gets no substitution
                newIntersection.add(type);
            }
        }
        return of(newIntersection);
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof IntersectionType && types.equals(((IntersectionType) other).types);
    }

    @Override
    public int hashCode() {
        return types.hashCode();
    }

    public static IntersectionType of(ReferenceType... intersection) {
        return of(Arrays.asList(intersection));
    }

    public static IntersectionType of(Collection<? extends ReferenceType> intersection) {
        return of(TypeUtil.expandIntersectionTypes(intersection));
    }

    public static IntersectionType of(Set<SingleReferenceType> intersection) {
        return new IntersectionType(intersection);
    }
}
