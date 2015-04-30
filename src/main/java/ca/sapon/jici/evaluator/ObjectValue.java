/**
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
package ca.sapon.jici.evaluator;

public class ObjectValue implements Value {
    private static final ObjectValue THE_NULL = new ObjectValue(null);
    private final Object value;

    private ObjectValue(Object value) {
        this.value = value;
    }

    @Override
    public boolean asBoolean() {
        throw new IllegalArgumentException("Cannot cast an object to a boolean");
    }

    @Override
    public byte asByte() {
        if (value instanceof Byte) {
            return (Byte) value;
        }
        throw new IllegalArgumentException("Cannot cast an object to a byte");
    }

    @Override
    public short asShort() {
        if (value instanceof Short) {
            return (Short) value;
        }
        throw new IllegalArgumentException("Cannot cast an object to a short");
    }

    @Override
    public char asChar() {
        if (value instanceof Character) {
            return (Character) value;
        }
        throw new IllegalArgumentException("Cannot cast an object to a char");
    }

    @Override
    public int asInt() {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        throw new IllegalArgumentException("Cannot cast an object to a int");
    }

    @Override
    public long asLong() {
        if (value instanceof Long) {
            return (Long) value;
        }
        throw new IllegalArgumentException("Cannot cast an object to a long");
    }

    @Override
    public float asFloat() {
        if (value instanceof Float) {
            return (Float) value;
        }
        throw new IllegalArgumentException("Cannot cast an object to a float");
    }

    @Override
    public double asDouble() {
        if (value instanceof Double) {
            return (Double) value;
        }
        throw new IllegalArgumentException("Cannot cast an object to a double");
    }

    @Override
    public Object asObject() {
        return value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T as() {
        return (T) asObject();
    }

    @Override
    public ValueKind getKind() {
        return ValueKind.OBJECT;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    public static ObjectValue of(Object value) {
        if (value == null) {
            return THE_NULL;
        }
        return new ObjectValue(value);
    }
}
