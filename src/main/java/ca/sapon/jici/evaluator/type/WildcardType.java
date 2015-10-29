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

import java.util.List;

import ca.sapon.jici.evaluator.value.ValueKind;
import ca.sapon.jici.util.StringUtil;

/**
 *
 */
public class WildcardType implements TypeParameter {
    private final List<SingleClassType> lowerBound;
    private final List<SingleClassType> upperBound;

    public WildcardType(List<SingleClassType> lowerBound, List<SingleClassType> upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    @Override
    public String getName() {
        String name = "?";
        if (!lowerBound.isEmpty()) {
            name += " super " + StringUtil.toString(lowerBound, " & ");
        }
        if (!upperBound.isEmpty()) {
            name += " extends " + StringUtil.toString(upperBound, " & ");
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
        if (lowerBound.isEmpty() && upperBound.isEmpty()) {
            return false;
        }
        for (SingleClassType lower : lowerBound) {
            if (!lower.convertibleTo(to)) {
                return false;
            }
        }
        for (SingleClassType upper : upperBound) {
            if (!to.convertibleTo(upper)) {
                return false;
            }
        }
        return true;
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
}
