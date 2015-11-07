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

import ca.sapon.jici.evaluator.value.ValueKind;
import ca.sapon.jici.util.StringUtil;
import ca.sapon.jici.util.TypeUtil;

/**
 *
 */
public class WildcardType implements TypeParameter {
    private final Set<SingleClassType> lowerBound;
    private final Set<SingleClassType> upperBound;

    private WildcardType(Set<SingleClassType> lowerBound, Set<SingleClassType> upperBound) {
        this.lowerBound = lowerBound;
        if (upperBound.size() == 1 && upperBound.contains(SingleClassType.THE_OBJECT)) {
            this.upperBound = Collections.emptySet();
        } else {
            this.upperBound = upperBound;
        }
    }

    @Override
    public String getName() {
        String name = "?";
        if (!upperBound.isEmpty()) {
            name += " extends " + StringUtil.toString(upperBound, " & ");
        }
        if (!lowerBound.isEmpty()) {
            name += " super " + StringUtil.toString(lowerBound, " & ");
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
        for (SingleClassType upper : upperBound) {
            if (!upper.isArray()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isObject() {
        return true;
    }

    @Override
    public boolean convertibleTo(Type to) {
        return TypeUtil.convertibleTo(this, to);
    }

    @Override
    public boolean contains(TypeParameter other) {
        if (other instanceof WildcardType) {
            final WildcardType otherWildcard = (WildcardType) other;
            // Search for an upper bound of the other contained in one of this
            boolean anyUpperContained = true;
            thisBounds:
            for (SingleClassType thisUpper : upperBound) {
                anyUpperContained = false;
                for (SingleClassType otherUpper : otherWildcard.upperBound) {
                    if (!TypeUtil.convertibleTo(otherUpper, thisUpper)) {
                        continue thisBounds;
                    }
                }
                anyUpperContained = true;
                break;
            }
            if (!anyUpperContained) {
                return false;
            }
            // Search for a lower bound of this contained in one of other
            boolean anyLowerContained = true;
            otherBounds:
            for (SingleClassType otherLower : otherWildcard.lowerBound) {
                anyLowerContained = false;
                for (SingleClassType thisLower : lowerBound) {
                    if (!TypeUtil.convertibleTo(thisLower, otherLower)) {
                        continue otherBounds;
                    }
                }
                anyLowerContained = true;
                break;
            }
            return anyLowerContained;
        }
        if (other instanceof SingleClassType) {
            final SingleClassType otherClass = (SingleClassType) other;
            return upperBound.contains(otherClass) || lowerBound.contains(otherClass);
        }
        return false;
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

    public static WildcardType of(Set<SingleClassType> lowerBound, Set<SingleClassType> upperBound) {
        return new WildcardType(lowerBound, upperBound);
    }
}
