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

import ca.sapon.jici.evaluator.value.ValueKind;
import ca.sapon.jici.parser.name.TypeParameterName.BoundKind;

/**
 *
 */
public class WildcardType implements TypeParameter {
    private final SingleClassType bound;
    private final BoundKind kind;

    public WildcardType(SingleClassType bound, BoundKind kind) {
        if (kind == BoundKind.EXACT) {
            throw new UnsupportedOperationException(kind.name());
        }
        this.bound = bound;
        this.kind = kind;
    }

    @Override
    public String getName() {
        switch (kind) {
            case NONE:
                return "?";
            case LOWER:
                return "? super " + bound.toString();
            case UPPER:
                return "? extends " + bound.toString();
            default:
                throw new UnsupportedOperationException(kind.name());
        }
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
        return kind == BoundKind.UPPER && bound.isArray();
    }

    @Override
    public boolean isObject() {
        return true;
    }

    @Override
    public boolean convertibleTo(Type to) {
        switch (kind) {
            case NONE:
                return false;
            case LOWER:
                return bound.convertibleTo(to);
            case UPPER:
                return to.convertibleTo(bound);
            default:
                return false;
        }
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
        return bound != null ? bound.equals(that.bound) : that.bound == null && kind == that.kind;
    }

    @Override
    public int hashCode() {
        int result = bound != null ? bound.hashCode() : 0;
        result = 31 * result + kind.hashCode();
        return result;
    }
}
