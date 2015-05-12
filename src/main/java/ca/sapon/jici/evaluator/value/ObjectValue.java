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
package ca.sapon.jici.evaluator.value;

public class ObjectValue implements Value {
    private static final ObjectValue THE_NULL = new ObjectValue();
    private final Object value;
    private final ValueKind kind;
    private final Value unboxed;

    private ObjectValue() {
        value = null;
        kind = ValueKind.OBJECT;
        unboxed = null;
    }

    private ObjectValue(Object value) {
        this.value = value;
        unboxed = ValueKind.unbox(value);
        kind = unboxed == null ? ValueKind.OBJECT : unboxed.getKind();
    }

    @Override
    public boolean asBoolean() {
        if (kind == ValueKind.OBJECT) {
            throw new IllegalArgumentException("Cannot cast an object to a boolean");
        }
        return unboxed.asBoolean();
    }

    @Override
    public byte asByte() {
        if (kind == ValueKind.OBJECT) {
            throw new IllegalArgumentException("Cannot cast an object to a byte");
        }
        return unboxed.asByte();
    }

    @Override
    public short asShort() {
        if (kind == ValueKind.OBJECT) {
            throw new IllegalArgumentException("Cannot cast an object to a short");
        }
        return unboxed.asShort();
    }

    @Override
    public char asChar() {
        if (kind == ValueKind.OBJECT) {
            throw new IllegalArgumentException("Cannot cast an object to a char");
        }
        return unboxed.asChar();
    }

    @Override
    public int asInt() {
        if (kind == ValueKind.OBJECT) {
            throw new IllegalArgumentException("Cannot cast an object to a int");
        }
        return unboxed.asInt();
    }

    @Override
    public long asLong() {
        if (kind == ValueKind.OBJECT) {
            throw new IllegalArgumentException("Cannot cast an object to a long");
        }
        return unboxed.asLong();
    }

    @Override
    public float asFloat() {
        if (kind == ValueKind.OBJECT) {
            throw new IllegalArgumentException("Cannot cast an object to a float");
        }
        return unboxed.asFloat();
    }

    @Override
    public double asDouble() {
        if (kind == ValueKind.OBJECT) {
            throw new IllegalArgumentException("Cannot cast an object to a double");
        }
        return unboxed.asDouble();
    }

    @Override
    public Object asObject() {
        return value;
    }

    @Override
    public String asString() {
        return value == null ? "null" : value.toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T as() {
        return (T) asObject();
    }

    @Override
    public ValueKind getKind() {
        return kind;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public Class<?> getTypeClass() {
        return value == null ? null : value.getClass();
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

    public static ObjectValue defaultValue() {
        return THE_NULL;
    }
}
