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

public class CharValue implements Result {
    private static final CharValue[] COMMON_VALUES = new CharValue[256];
    private final char value;

    static {
        for (int i = 0; i < 256; i++) {
            COMMON_VALUES[i] = new CharValue((char) (i - 128));
        }
    }

    private CharValue(char value) {
        this.value = value;
    }

    @Override
    public boolean asBoolean() {
        throw new IllegalArgumentException("Cannot cast a char to a boolean");
    }

    @Override
    public byte asByte() {
        return (byte) value;
    }

    @Override
    public short asShort() {
        return (short) value;
    }

    @Override
    public char asChar() {
        return value;
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
    public Character asObject() {
        return Character.valueOf(value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getValue() {
        return (T) asObject();
    }

    @Override
    public ResultType getType() {
        return ResultType.CHAR;
    }

    @Override
    public boolean isPrimitive() {
        return true;
    }

    public static CharValue of(char value) {
        final int offsetValue = value + 128;
        if ((offsetValue & ~0xFF) == 0) {
            return COMMON_VALUES[offsetValue];
        }
        return new CharValue(value);
    }
}