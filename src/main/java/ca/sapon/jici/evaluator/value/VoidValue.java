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

public class VoidValue implements Value {
    public static final VoidValue THE_VOID = new VoidValue();

    private VoidValue() {
    }

    @Override
    public boolean asBoolean() {
        throw new IllegalArgumentException("Cannot cast a void to a byte");
    }

    @Override
    public byte asByte() {
        throw new IllegalArgumentException("Cannot cast a void to a byte");
    }

    @Override
    public short asShort() {
        throw new IllegalArgumentException("Cannot cast a void to a short");
    }

    @Override
    public char asChar() {
        throw new IllegalArgumentException("Cannot cast a void to a char");
    }

    @Override
    public int asInt() {
        throw new IllegalArgumentException("Cannot cast a void to an int");
    }

    @Override
    public long asLong() {
        throw new IllegalArgumentException("Cannot cast a void to a long");
    }

    @Override
    public float asFloat() {
        throw new IllegalArgumentException("Cannot cast a void to a float");
    }

    @Override
    public double asDouble() {
        throw new IllegalArgumentException("Cannot cast a void to a double");
    }

    @Override
    public Boolean asObject() {
        throw new IllegalArgumentException("Cannot cast a void to an object");
    }

    @Override
    public String asString() {
        throw new IllegalArgumentException("Cannot cast a void to a string");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T as() {
        return (T) asObject();
    }

    @Override
    public ValueKind getKind() {
        return ValueKind.VOID;
    }

    @Override
    public boolean isPrimitive() {
        return true;
    }

    @Override
    public Class<?> getTypeClass() {
        return void.class;
    }

    @Override
    public String toString() {
        return "void";
    }

    public static VoidValue defaultValue() {
        throw new IllegalArgumentException("Void has no value");
    }
}
