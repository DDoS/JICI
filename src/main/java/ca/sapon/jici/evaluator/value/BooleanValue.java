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

public class BooleanValue implements Value {
    private static final BooleanValue THE_TRUE = new BooleanValue(true);
    private static final BooleanValue THE_FALSE = new BooleanValue(false);
    private final boolean value;

    private BooleanValue(boolean value) {
        this.value = value;
    }

    @Override
    public boolean asBoolean() {
        return value;
    }

    @Override
    public byte asByte() {
        throw new UnsupportedOperationException("Cannot convert a boolean to a byte");
    }

    @Override
    public short asShort() {
        throw new UnsupportedOperationException("Cannot convert a boolean to a short");
    }

    @Override
    public char asChar() {
        throw new UnsupportedOperationException("Cannot convert a boolean to a char");
    }

    @Override
    public int asInt() {
        throw new UnsupportedOperationException("Cannot convert a boolean to an int");
    }

    @Override
    public long asLong() {
        throw new UnsupportedOperationException("Cannot convert a boolean to a long");
    }

    @Override
    public float asFloat() {
        throw new UnsupportedOperationException("Cannot convert a boolean to a float");
    }

    @Override
    public double asDouble() {
        throw new UnsupportedOperationException("Cannot convert a boolean to a double");
    }

    @Override
    public Boolean asObject() {
        return value;
    }

    @Override
    public String asString() {
        return Boolean.toString(value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T as() {
        return (T) asObject();
    }

    @Override
    public ValueKind getKind() {
        return ValueKind.BOOLEAN;
    }

    @Override
    public boolean isPrimitive() {
        return true;
    }

    @Override
    public Class<?> getTypeClass() {
        return boolean.class;
    }

    @Override
    public String toString() {
        return asString();
    }

    public static BooleanValue of(boolean value) {
        return value ? THE_TRUE : THE_FALSE;
    }

    public static BooleanValue defaultValue() {
        return THE_FALSE;
    }
}
