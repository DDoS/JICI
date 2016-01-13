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

import java.util.Collections;
import java.util.Set;

import ca.sapon.jici.evaluator.Substitutions;
import ca.sapon.jici.evaluator.value.ValueKind;

/**
 * A wildcard type, such as {@code <?>}, {@code <? extends String>} or {@code <? super Integer>}.
 */
public class WildcardType implements TypeArgument, BoundedType {
    public static final WildcardType THE_UNBOUNDED = of(Collections.<SingleReferenceType>emptySet(), Collections.<SingleReferenceType>emptySet());
    private final IntersectionType lowerBound;
    private final IntersectionType upperBound;

    private WildcardType(IntersectionType lowerBound, IntersectionType upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    @Override
    public IntersectionType getLowerBound() {
        return lowerBound;
    }

    @Override
    public IntersectionType getUpperBound() {
        return upperBound;
    }

    public boolean isUnbounded() {
        return lowerBound.getTypes().contains(LiteralReferenceType.THE_OBJECT) && lowerBound.getTypes().contains(NullType.THE_NULL);
    }

    @Override
    public String getName() {
        String name = "?";
        if (!lowerBound.getTypes().contains(NullType.THE_NULL)) {
            name += " super " + lowerBound;
        }
        if (!upperBound.getTypes().contains(LiteralReferenceType.THE_OBJECT)) {
            name += " extends " + upperBound;
        }
        return name;
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
        return upperBound.isArray();
    }

    @Override
    public boolean isReference() {
        return true;
    }

    @Override
    public boolean isReifiable() {
        return isUnbounded();
    }

    @Override
    public boolean convertibleTo(Type to) {
        throw new UnsupportedOperationException("Wildcard type needs to be capture converted for this operation");
    }

    @Override
    public WildcardType substituteTypeVariables(Substitutions substitution) {
        // Apply recursively on lower and upper bounds
        return new WildcardType(lowerBound.substituteTypeVariables(substitution), upperBound.substituteTypeVariables(substitution));
    }

    @Override
    public Set<TypeVariable> getTypeVariables() {
        final Set<TypeVariable> typeVariables = lowerBound.getTypeVariables();
        typeVariables.addAll(upperBound.getTypeVariables());
        return typeVariables;
    }

    @Override
    public boolean contains(TypeArgument other) {
        if (other instanceof WildcardType) {
            final WildcardType otherWildcard = (WildcardType) other;
            return otherWildcard.upperBound.convertibleTo(upperBound) && lowerBound.convertibleTo(otherWildcard.lowerBound);
        }
        return other.convertibleTo(upperBound) && lowerBound.convertibleTo(other);
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof WildcardType)) {
            return false;
        }
        final WildcardType that = (WildcardType) other;
        return this.lowerBound.equals(that.lowerBound) && this.upperBound.equals(that.upperBound);
    }

    @Override
    public int hashCode() {
        int result = lowerBound.hashCode();
        result = 31 * result + upperBound.hashCode();
        return result;
    }

    public static WildcardType of(Set<SingleReferenceType> lowerBound, Set<SingleReferenceType> upperBound) {
        final IntersectionType lower = lowerBound.isEmpty() ? IntersectionType.EVERYTHING : IntersectionType.of(lowerBound);
        final IntersectionType upper = upperBound.isEmpty() ? IntersectionType.NOTHING : IntersectionType.of(upperBound);
        return of(lower, upper);
    }

    public static WildcardType of(IntersectionType lowerBound, IntersectionType upperBound) {
        upperBound.checkIfValidUpperBound();
        return new WildcardType(lowerBound, upperBound);
    }
}
