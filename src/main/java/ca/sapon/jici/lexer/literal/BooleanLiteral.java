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
package ca.sapon.jici.lexer.literal;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.evaluator.value.ValueKind;
import ca.sapon.jici.lexer.TokenID;

public class BooleanLiteral extends Literal implements Value {
    private static final String trueSource = "true";
    private static final String falseSource = "false";
    private static final BooleanLiteral THE_TRUE = new BooleanLiteral(true);
    private static final BooleanLiteral THE_FALSE = new BooleanLiteral(false);
    private final boolean value;

    private BooleanLiteral(boolean value) {
        super(value ? TokenID.LITERAL_TRUE : TokenID.LITERAL_FALSE, value ? trueSource : falseSource);
        this.value = value;
    }

    @Override
    public boolean asBoolean() {
        return value;
    }

    @Override
    public byte asByte() {
        throw new IllegalArgumentException("Cannot cast a boolean to a byte");
    }

    @Override
    public short asShort() {
        throw new IllegalArgumentException("Cannot cast a boolean to a short");
    }

    @Override
    public char asChar() {
        throw new IllegalArgumentException("Cannot cast a boolean to a char");
    }

    @Override
    public int asInt() {
        throw new IllegalArgumentException("Cannot cast a boolean to an int");
    }

    @Override
    public long asLong() {
        throw new IllegalArgumentException("Cannot cast a boolean to a long");
    }

    @Override
    public float asFloat() {
        throw new IllegalArgumentException("Cannot cast a boolean to a float");
    }

    @Override
    public double asDouble() {
        throw new IllegalArgumentException("Cannot cast a boolean to a double");
    }

    @Override
    public Boolean asObject() {
        return value;
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
    public Class<?> getTypeClass(Environment environment, Class<?> upperBound) {
        return getTypeClass();
    }

    public static boolean is(String source) {
        return trueSource.equals(source) || falseSource.equals(source);
    }

    public static BooleanLiteral from(String source) {
        return source.equals(trueSource) ? THE_TRUE : THE_FALSE;
    }

    @Override
    public Value getValue(Environment environment) {
        return this;
    }
}
