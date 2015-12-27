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

import ca.sapon.jici.evaluator.Accessible;
import ca.sapon.jici.evaluator.Callable;
import ca.sapon.jici.util.StringUtil;

/**
 * A type variable, such as {@code <T>}.
 */
public class TypeVariable extends SingleReferenceType implements TypeArgument {
    private final String name;
    private final int dimensions;
    private final IntersectionType upperBound;
    private final SingleReferenceType firstBound;
    private final SingleReferenceType upperBoundClass;
    private final LiteralReferenceType[] upperBoundInterfaces;

    private TypeVariable(String name, SingleReferenceType firstBound, SingleReferenceType upperBoundClass, LiteralReferenceType[] upperBoundInterfaces, IntersectionType upperBound, int dimensions) {
        this.name = name;
        this.dimensions = dimensions;
        this.upperBound = upperBound;
        this.firstBound = firstBound;
        this.upperBoundClass = upperBoundClass;
        this.upperBoundInterfaces = upperBoundInterfaces;
    }

    public IntersectionType getUpperBound() {
        return upperBound;
    }

    @Override
    public String getName() {
        String fullName = name + StringUtil.repeat("[]", dimensions);
        if (!upperBound.getTypes().contains(LiteralReferenceType.THE_OBJECT)) {
            fullName += " extends " + upperBound;
        }
        return fullName;
    }

    @Override
    public boolean isArray() {
        return dimensions > 0;
    }

    @Override
    public boolean isReifiable() {
        return false;
    }

    @Override
    public TypeVariable asArray(int dimensions) {
        return new TypeVariable(name, firstBound, upperBoundClass, upperBoundInterfaces, upperBound, this.dimensions + dimensions);
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
        if (dimensions <= 0) {
            throw new UnsupportedOperationException("Not an array type");
        }
        return new TypeVariable(name, firstBound, upperBoundClass, upperBoundInterfaces, upperBound, dimensions - 1);
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
    public SingleReferenceType getSuperType() {
        return upperBoundClass;
    }

    @Override
    public LiteralReferenceType[] getInterfaces() {
        return upperBoundInterfaces;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TypeVariable)) {
            return false;
        }
        final TypeVariable that = (TypeVariable) o;
        return dimensions == that.dimensions && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + dimensions;
        return result;
    }

    public static TypeVariable of(String name, List<SingleReferenceType> upperBound) {
        if (upperBound.isEmpty()) {
            // Empty implies object
            upperBound.add(LiteralReferenceType.THE_OBJECT);
        }
        // Ensure the following holds for the upper bound, in order: a single type variable, or a class or an interface and zero or more interfaces
        final SingleReferenceType firstBound = upperBound.get(0);
        final SingleReferenceType upperBoundClass;
        final LiteralReferenceType[] upperBoundInterfaces;
        if (firstBound instanceof TypeVariable) {
            if (upperBound.size() > 1) {
                throw new UnsupportedOperationException("Cannot have more than one type in the upper bound: " + upperBound);
            }
            upperBoundClass = firstBound;
            upperBoundInterfaces = new LiteralReferenceType[0];
        } else if (firstBound instanceof LiteralReferenceType) {
            final LiteralReferenceType firstBoundLiteral = (LiteralReferenceType) firstBound;
            if (firstBoundLiteral.isArray()) {
                throw new UnsupportedOperationException("Cannot have an array type in the upper bound: " + firstBoundLiteral);
            }
            int j = 0;
            if (firstBoundLiteral.isInterface()) {
                upperBoundClass = LiteralReferenceType.THE_OBJECT;
                upperBoundInterfaces = new LiteralReferenceType[upperBound.size()];
                upperBoundInterfaces[j++] = firstBoundLiteral;
            } else {
                upperBoundClass = firstBoundLiteral;
                upperBoundInterfaces = new LiteralReferenceType[upperBound.size() - 1];
            }
            for (int i = 1; i < upperBound.size(); i++) {
                final SingleReferenceType bound = upperBound.get(i);
                if (bound instanceof LiteralReferenceType) {
                    final LiteralReferenceType literalReferenceType = (LiteralReferenceType) bound;
                    if (literalReferenceType.isInterface()) {
                        upperBoundInterfaces[j++] = literalReferenceType;
                        continue;
                    }
                }
                throw new UnsupportedOperationException("Only the first bound can be anything other than an interface: " + bound);
            }
        } else {
            throw new UnsupportedOperationException("The first bound must be a type variable, class or interface: " + firstBound);
        }
        return new TypeVariable(name, firstBound, upperBoundClass, upperBoundInterfaces, IntersectionType.of(upperBound), 0);
    }
}
