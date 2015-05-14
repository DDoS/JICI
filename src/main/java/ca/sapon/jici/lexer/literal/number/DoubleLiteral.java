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
package ca.sapon.jici.lexer.literal.number;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.evaluator.value.ValueKind;
import ca.sapon.jici.evaluator.value.type.PrimitiveValueType;
import ca.sapon.jici.evaluator.value.type.ValueType;
import ca.sapon.jici.lexer.TokenID;

public class DoubleLiteral extends NumberLiteral implements Value {
    private double value = 0;
    private boolean evaluated = false;

    public DoubleLiteral(String source, int index) {
        super(TokenID.LITERAL_DOUBLE, source, index);
    }

    private void evaluate() {
        if (!evaluated) {
            value = Double.parseDouble(getSource());
            evaluated = true;
        }
    }

    @Override
    public boolean asBoolean() {
        throw new IllegalArgumentException("Cannot cast a double to a boolean");
    }

    @Override
    public byte asByte() {
        return (byte) asDouble();
    }

    @Override
    public short asShort() {
        return (short) asDouble();
    }

    @Override
    public char asChar() {
        return (char) asDouble();
    }

    @Override
    public int asInt() {
        return (int) asDouble();
    }

    @Override
    public long asLong() {
        return (long) asDouble();
    }

    @Override
    public float asFloat() {
        return (float) asDouble();
    }

    @Override
    public double asDouble() {
        evaluate();
        return value;
    }

    @Override
    public Double asObject() {
        return asDouble();
    }

    @Override
    public String asString() {
        return Double.toString(asDouble());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T as() {
        return (T) asObject();
    }

    @Override
    public ValueKind getKind() {
        return ValueKind.DOUBLE;
    }

    @Override
    public boolean isPrimitive() {
        return true;
    }

    @Override
    public Class<?> getTypeClass() {
        return double.class;
    }

    @Override
    public ValueType getValueType(Environment environment) {
        return PrimitiveValueType.of(getTypeClass());
    }

    @Override
    public String toString() {
        return getSource() + 'd';
    }

    @Override
    public Value getValue(Environment environment) {
        return this;
    }
}
