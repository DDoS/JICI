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

import ca.sapon.jici.evaluator.Callable;
import ca.sapon.jici.evaluator.ClassVariable;
import ca.sapon.jici.evaluator.Substitutions;
import ca.sapon.jici.util.StringUtil;

/**
 * A type variable, such as {@code <T>}.
 */
public class TypeVariable extends SingleReferenceType implements TypeArgument, BoundedType {
    private final String name;
    private final int dimensions;
    private final IntersectionType lowerBound;
    private final IntersectionType upperBound;
    private final SingleReferenceType firstUpperBound;

    private TypeVariable(String name, int dimensions, IntersectionType lowerBound, IntersectionType upperBound) {
        this.name = name;
        this.dimensions = dimensions;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        firstUpperBound = upperBound.checkIfValidUpperBound();
    }

    public String getDeclaredName() {
        return name;
    }

    @Override
    public IntersectionType getLowerBound() {
        return lowerBound;
    }

    @Override
    public IntersectionType getUpperBound() {
        return upperBound;
    }

    @Override
    public String getName() {
        String fullName = name + StringUtil.repeat("[]", dimensions);
        if (!lowerBound.equals(IntersectionType.EVERYTHING)) {
            fullName += " super " + lowerBound;
        }
        if (!upperBound.equals(IntersectionType.NOTHING)) {
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
        IntersectionType arrayLowerBound = lowerBound;
        if (!arrayLowerBound.equals(IntersectionType.EVERYTHING)) {
            arrayLowerBound = arrayLowerBound.asArray(dimensions);
        }
        return new TypeVariable(name, this.dimensions + dimensions, arrayLowerBound, upperBound.asArray(dimensions));
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
        IntersectionType lowerBoundComponent = lowerBound;
        if (!lowerBoundComponent.equals(IntersectionType.EVERYTHING)) {
            lowerBoundComponent = lowerBoundComponent.getComponentType();
        }
        return new TypeVariable(name, dimensions - 1, lowerBoundComponent, upperBound.getComponentType());
    }

    @Override
    public Callable getConstructor(Type[] arguments) {
        return upperBound.getConstructor(arguments);
    }

    @Override
    public ClassVariable getField(String name) {
        return upperBound.getField(name);
    }

    @Override
    public Callable getMethod(String name, TypeArgument[] typeArguments, Type[] arguments) {
        return upperBound.getMethod(name, typeArguments, arguments);
    }

    @Override
    public TypeArgument substituteTypeVariables(Substitutions substitutions) {
        // If the variable is itself in the substitutions, return the substitution
        final TypeArgument substitution = substitutions.forVariable(name);
        if (substitution != null) {
            return substitution.asArray(dimensions);
        }
        // Else apply recursively on lower and upper bounds
        return new TypeVariable(name, dimensions, lowerBound.substituteTypeVariables(substitutions),
                upperBound.substituteTypeVariables(substitutions));
    }

    public TypeVariable substituteBoundTypeVariables(Substitutions substitutions) {
        // Apply only on lower and upper bounds
        return new TypeVariable(name, dimensions, lowerBound.substituteTypeVariables(substitutions),
                upperBound.substituteTypeVariables(substitutions));
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
        if (to instanceof TypeVariable) {
            final TypeVariable target = (TypeVariable) to;
            return upperBound.convertibleTo(target.getUpperBound()) && target.getLowerBound().convertibleTo(lowerBound);
        }
        // Can convert to an intersection type if can convert to all the types it contains
        if (to instanceof IntersectionType) {
            final IntersectionType target = (IntersectionType) to;
            for (SingleReferenceType type : target.getTypes()) {
                if (!convertibleTo(type)) {
                    return false;
                }
            }
            return true;
        }
        // Can convert to any other type if the upper bound can
        return upperBound.convertibleTo(to);
    }

    public boolean boundsContain(Type type) {
        return lowerBound.convertibleTo(type) && type.convertibleTo(upperBound);
    }

    @Override
    public LiteralReferenceType getErasure() {
        // The upper bound cannot be the null type so the erasure must be a literal reference
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
        return name.equals(that.name) && lowerBound.equals(that.lowerBound) && upperBound.equals(that.upperBound);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + lowerBound.hashCode();
        result = 31 * result + upperBound.hashCode();
        return result;
    }

    public static TypeVariable of(String name, List<SingleReferenceType> declaredUpperBound) {
        if (declaredUpperBound.isEmpty()) {
            // Empty implies object
            declaredUpperBound.add(LiteralReferenceType.THE_OBJECT);
        }
        // Ensure the following holds for the first upper bound: a type variable, a class or an interface
        final SingleReferenceType firstUpperBound = declaredUpperBound.get(0);
        if (firstUpperBound instanceof LiteralReferenceType) {
            if (firstUpperBound.isArray()) {
                throw new UnsupportedOperationException("Cannot have an array type in the upper bound: " + firstUpperBound);
            }
        } else if (!(firstUpperBound instanceof TypeVariable)) {
            throw new UnsupportedOperationException("The first upper bound must be a type variable, class or interface: " + firstUpperBound);
        }
        return new TypeVariable(name, 0, IntersectionType.EVERYTHING, IntersectionType.of(declaredUpperBound));
    }

    public static TypeVariable of(String name, IntersectionType lowerBound, IntersectionType upperBound) {
        return new TypeVariable(name, 0, lowerBound, upperBound);
    }
}
