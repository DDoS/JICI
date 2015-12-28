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

/**
 * A type variable, such as {@code <T>}.
 */
public class TypeVariable extends SingleReferenceType implements TypeArgument {
    private final String name;
    private final IntersectionType upperBound;
    private final SingleReferenceType firstBound;

    private TypeVariable(String name, SingleReferenceType firstBound, IntersectionType upperBound) {
        this.name = name;
        this.upperBound = upperBound;
        this.firstBound = firstBound;
    }

    public IntersectionType getUpperBound() {
        return upperBound;
    }

    @Override
    public String getName() {
        String fullName = name;
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
        return new TypeVariable(name, firstBound, upperBound.asArray(dimensions));
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
        return new TypeVariable(name, firstBound, upperBound.getComponentType());
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
    public boolean contains(TypeArgument other) {
        return equals(other);
    }

    @Override
    public boolean convertibleTo(Type to) {
        return upperBound.convertibleTo(to);
    }

    @Override
    public LiteralReferenceType getErasure() {
        // The upper bound class cannot be null so the erasure must be a literal reference
        return (LiteralReferenceType) firstBound.getErasure();
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
        return name.equals(that.name) && upperBound.equals(that.upperBound) && firstBound.equals(that.firstBound);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + upperBound.hashCode();
        result = 31 * result + firstBound.hashCode();
        return result;
    }

    public static TypeVariable of(String name, List<SingleReferenceType> upperBound) {
        if (upperBound.isEmpty()) {
            // Empty implies object
            upperBound.add(LiteralReferenceType.THE_OBJECT);
        }
        // Ensure the following holds for the upper bound, in order:
        final SingleReferenceType firstBound = upperBound.get(0);
        if (firstBound instanceof TypeVariable) {
            // a single type variable
            if (upperBound.size() > 1) {
                throw new UnsupportedOperationException("Cannot have more than one type in the upper bound: " + upperBound);
            }
        } else if (firstBound instanceof LiteralReferenceType) {
            // or a class or an interface
            final LiteralReferenceType firstBoundLiteral = (LiteralReferenceType) firstBound;
            if (firstBoundLiteral.isArray()) {
                throw new UnsupportedOperationException("Cannot have an array type in the upper bound: " + firstBoundLiteral);
            }
            // with zero or more interfaces
            for (int i = 1; i < upperBound.size(); i++) {
                final SingleReferenceType bound = upperBound.get(i);
                if (bound instanceof LiteralReferenceType) {
                    final LiteralReferenceType literalReferenceType = (LiteralReferenceType) bound;
                    if (literalReferenceType.isInterface()) {
                        continue;
                    }
                }
                throw new UnsupportedOperationException("Only the first bound can be anything other than an interface: " + bound);
            }
        } else {
            throw new UnsupportedOperationException("The first bound must be a type variable, class or interface: " + firstBound);
        }
        return new TypeVariable(name, firstBound, IntersectionType.of(upperBound));
    }
}
