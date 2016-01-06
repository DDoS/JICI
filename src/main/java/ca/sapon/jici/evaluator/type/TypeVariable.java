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

import java.lang.reflect.Array;
import java.util.List;
import java.util.Set;

import ca.sapon.jici.evaluator.Accessible;
import ca.sapon.jici.evaluator.Callable;
import ca.sapon.jici.evaluator.Substitutions;

/**
 * A type variable, such as {@code <T>}.
 */
public class TypeVariable extends SingleReferenceType implements TypeArgument {
    private final String name;
    private final IntersectionType lowerBound;
    private final IntersectionType upperBound;
    private final SingleReferenceType firstUpperBound;

    private TypeVariable(String name, IntersectionType lowerBound, IntersectionType upperBound, SingleReferenceType firstUpperBound) {
        this.name = name;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.firstUpperBound = firstUpperBound;
    }

    public String getDeclaredName() {
        return name;
    }

    public IntersectionType getLowerBound() {
        return lowerBound;
    }

    public IntersectionType getUpperBound() {
        return upperBound;
    }

    @Override
    public String getName() {
        String fullName = name;
        if (!lowerBound.getTypes().contains(NullType.THE_NULL)) {
            fullName += " super " + lowerBound;
        }
        if (!upperBound.getTypes().contains(LiteralReferenceType.THE_OBJECT)) {
            fullName += " extends " + upperBound;
        }
        return fullName;
    }

    @Override
    public boolean isArray() {
        return upperBound.isArray();
    }

    @Override
    public boolean isReifiable() {
        return false;
    }

    @Override
    public TypeVariable asArray(int dimensions) {
        return new TypeVariable(name, lowerBound, upperBound.asArray(dimensions), firstUpperBound);
    }

    @Override
    public Object newArray(int length) {
        return Array.newInstance(getErasure().getTypeClass(), length);
    }

    @Override
    public Object newArray(int[] lengths) {
        return Array.newInstance(getErasure().getTypeClass(), lengths);
    }

    @Override
    public TypeVariable getComponentType() {
        return new TypeVariable(name, lowerBound, upperBound.getComponentType(), firstUpperBound);
    }

    @Override
    public Callable getConstructor(Type[] arguments) {
        return upperBound.getConstructor(arguments);
    }

    @Override
    public Accessible getField(String name) {
        return upperBound.getField(name);
    }

    @Override
    public Callable getMethod(String name, Type[] arguments) {
        return upperBound.getMethod(name, arguments);
    }

    @Override
    public TypeVariable substituteTypeVariables(Substitutions substitution) {
        // Apply recursively on lower and upper bound
        return new TypeVariable(name, lowerBound.substituteTypeVariables(substitution), upperBound.substituteTypeVariables(substitution), firstUpperBound);
    }

    @Override
    public Set<TypeVariable> getTypeVariables() {
        final Set<TypeVariable> typeVariables = lowerBound.getTypeVariables();
        typeVariables.addAll(upperBound.getTypeVariables());
        typeVariables.add(this);
        return typeVariables;
    }

    @Override
    public boolean contains(TypeArgument other) {
        return equals(other);
    }

    @Override
    public boolean convertibleTo(Type to) {
        // Can convert to a type variable if its bounds are within these ones
        // Can convert to an intersection type if can convert to all the types in it
        // Can convert to any other type if the upper bound can
        if (to instanceof TypeVariable) {
            final TypeVariable target = (TypeVariable) to;
            return upperBound.convertibleTo(target.getUpperBound()) && target.getLowerBound().convertibleTo(lowerBound);
        }
        if (to instanceof IntersectionType) {
            final IntersectionType target = (IntersectionType) to;
            for (SingleReferenceType type : target.getTypes()) {
                if (!convertibleTo(type)) {
                    return false;
                }
            }
            return true;
        }
        return upperBound.convertibleTo(to);
    }

    @Override
    public LiteralReferenceType getErasure() {
        // The upper bound class cannot be null so the erasure must be a literal reference
        return (LiteralReferenceType) firstUpperBound.getErasure();
    }

    @Override
    public Set<SingleReferenceType> getDirectSuperTypes() {
        // The direct super types of a type variable are listed in its upper bound
        return upperBound.getTypes();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TypeVariable)) {
            return false;
        }
        final TypeVariable that = (TypeVariable) other;
        return name.equals(that.name) && lowerBound.equals(that.lowerBound) && upperBound.equals(that.upperBound) && firstUpperBound.equals(that.firstUpperBound);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + lowerBound.hashCode();
        result = 31 * result + upperBound.hashCode();
        result = 31 * result + firstUpperBound.hashCode();
        return result;
    }

    public static TypeVariable of(String name, List<SingleReferenceType> upperBound) {
        if (upperBound.isEmpty()) {
            // Empty implies object
            upperBound.add(LiteralReferenceType.THE_OBJECT);
        }
        // Ensure the following holds for the first upper bound: a type variable, a class or an interface
        final SingleReferenceType firstUpperBound = upperBound.get(0);
        if (firstUpperBound instanceof LiteralReferenceType) {
            if (firstUpperBound.isArray()) {
                throw new UnsupportedOperationException("Cannot have an array type in the upper bound: " + firstUpperBound);
            }
        } else if (!(firstUpperBound instanceof TypeVariable)) {
            throw new UnsupportedOperationException("The first upper bound must be a type variable, class or interface: " + firstUpperBound);
        }
        final IntersectionType reducedUpperBound = IntersectionType.of(upperBound);
        reducedUpperBound.checkIfValidUpperBound();
        return new TypeVariable(name, IntersectionType.EVERYTHING, reducedUpperBound, upperBound.get(0));
    }

    public static TypeVariable of(String name, IntersectionType lowerBound, IntersectionType upperBound) {
        final SingleReferenceType erasure = upperBound.checkIfValidUpperBound();
        return new TypeVariable(name, lowerBound, upperBound, erasure);
    }
}
