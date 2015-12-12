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
    private final Set<? extends ClassType> lowerBound;
    private final Set<? extends ClassType> upperBound;

    private WildcardType(Set<SingleClassType> lowerBound, Set<SingleClassType> upperBound) {
        if (lowerBound.isEmpty()) {
            this.lowerBound = Collections.<ClassType>singleton(NullType.THE_NULL);
        } else {
            this.lowerBound = lowerBound;
        }
        if (upperBound.isEmpty()) {
            this.upperBound = Collections.<ClassType>singleton(SingleClassType.THE_OBJECT);
        } else {
            this.upperBound = upperBound;
        }
    }

    @Override
    public String getName() {
        String name = "?";
        if (upperBound.size() != 1 || !upperBound.contains(SingleClassType.THE_OBJECT)) {
            name += " extends " + StringUtil.toString(upperBound, " & ");
        }
        if (lowerBound.size() != 1 || !lowerBound.contains(NullType.THE_NULL)) {
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
        for (ClassType upper : upperBound) {
            if (upper.isArray()) {
                return true;
            }
        }
        return false;
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
            // All upper bounds must be supertypes of other
            for (ClassType thisUpper : upperBound) {
                for (ClassType otherUpper : otherWildcard.upperBound) {
                    if (!TypeUtil.convertibleTo(otherUpper, thisUpper)) {
                        return false;
                    }
                }
            }
            // All lower bounds must be subtypes of other
            for (ClassType thisLower : lowerBound) {
                for (ClassType otherLower : otherWildcard.lowerBound) {
                    if (!TypeUtil.convertibleTo(thisLower, otherLower)) {
                        return false;
                    }
                }
            }
            return true;
        }
        if (other instanceof SingleClassType) {
            final SingleClassType otherClass = (SingleClassType) other;
            // All upper bounds must be supertypes of other
            for (ClassType upper : upperBound) {
                if (!TypeUtil.convertibleTo(otherClass, upper)) {
                    return false;
                }
            }
            // All lower bounds must be subtypes of other
            for (ClassType lower : lowerBound) {
                if (!TypeUtil.convertibleTo(lower, otherClass)) {
                    return false;
                }
            }
            return true;
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
