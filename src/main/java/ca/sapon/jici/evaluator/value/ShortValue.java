/*
 * This file is part of JICI, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015-2016 Aleksi Sapon <http://sapon.ca/jici/>
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

public class ShortValue implements Value {
    private static final ShortValue[] COMMON_VALUES = new ShortValue[256];
    private final short value;

    static {
        for (int i = 0; i < 256; i++) {
            COMMON_VALUES[i] = new ShortValue((short) (i - 128));
        }
    }

    private ShortValue(short value) {
        this.value = value;
    }

    @Override
    public boolean asBoolean() {
        throw new UnsupportedOperationException("Cannot convert a short to a boolean");
    }

    @Override
    public byte asByte() {
        return (byte) value;
    }

    @Override
    public short asShort() {
        return value;
    }

    @Override
    public char asChar() {
        return (char) value;
    }

    @Override
    public int asInt() {
        return value;
    }

    @Override
    public long asLong() {
        return value;
    }

    @Override
    public float asFloat() {
        return value;
    }

    @Override
    public double asDouble() {
        return value;
    }

    @Override
    public Short asObject() {
        return value;
    }

    @Override
    public String asString() {
        return Short.toString(value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T as() {
        return (T) asObject();
    }

    @Override
    public ValueKind getKind() {
        return ValueKind.SHORT;
    }

    @Override
    public boolean isPrimitive() {
        return true;
    }

    @Override
    public Class<?> getTypeClass() {
        return short.class;
    }

    @Override
    public String toString() {
        return asString();
    }

    public static ShortValue of(short value) {
        final int offsetValue = value + 128;
        if ((offsetValue & ~0xFF) == 0) {
            return COMMON_VALUES[offsetValue];
        }
        return new ShortValue(value);
    }

    public static ShortValue defaultValue() {
        return COMMON_VALUES[128];
    }
}
