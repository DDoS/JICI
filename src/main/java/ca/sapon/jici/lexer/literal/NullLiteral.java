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
package ca.sapon.jici.lexer.literal;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.evaluator.value.ValueKind;
import ca.sapon.jici.evaluator.type.NullType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.lexer.TokenID;

public class NullLiteral extends Literal {
    private static final String nullSource = "null";

    private NullLiteral(int index) {
        super(TokenID.LITERAL_NULL, nullSource, index);
    }

    @Override
    public boolean asBoolean() {
        throw new EvaluatorException("Cannot convert an object to a boolean", this);
    }

    @Override
    public byte asByte() {
        throw new EvaluatorException("Cannot convert an object to a byte", this);
    }

    @Override
    public short asShort() {
        throw new EvaluatorException("Cannot convert an object to a short", this);
    }

    @Override
    public char asChar() {
        throw new EvaluatorException("Cannot convert an object to a char", this);
    }

    @Override
    public int asInt() {
        throw new EvaluatorException("Cannot convert an object to an int", this);
    }

    @Override
    public long asLong() {
        throw new EvaluatorException("Cannot convert an object to a long", this);
    }

    @Override
    public float asFloat() {
        throw new EvaluatorException("Cannot convert an object to a float", this);
    }

    @Override
    public double asDouble() {
        throw new EvaluatorException("Cannot convert an object to a double", this);
    }

    @Override
    public Object asObject() {
        return null;
    }

    @Override
    public String asString() {
        return "null";
    }

    @Override
    public <T> T as() {
        return null;
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
    public Class<?> getTypeClass() {
        return null;
    }

    @Override
    public Type getType(Environment environment) {
        return NullType.THE_NULL;
    }

    public static boolean is(String source) {
        return nullSource.equals(source);
    }

    @Override
    public Value getValue(Environment environment) {
        return this;
    }

    public static NullLiteral from(int index) {
        return new NullLiteral(index);
    }
}
