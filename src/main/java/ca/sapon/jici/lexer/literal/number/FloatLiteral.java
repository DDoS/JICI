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
package ca.sapon.jici.lexer.literal.number;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.evaluator.type.PrimitiveType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.evaluator.value.ValueKind;
import ca.sapon.jici.lexer.TokenID;
import ca.sapon.jici.util.StringUtil;

public class FloatLiteral extends NumberLiteral {
    private float value = 0;
    private boolean evaluated = false;

    public FloatLiteral(String source, int index) {
        super(TokenID.LITERAL_FLOAT, source, index);
    }

    private void evaluate() {
        if (!evaluated) {
            String source = getSource();
            final int lastIndex = source.length() - 1;
            if (StringUtil.equalsNoCaseASCII(source.charAt(lastIndex), 'f')) {
                source = source.substring(0, lastIndex);
            }
            try {
                value = Float.parseFloat(source);
            } catch (NumberFormatException exception) {
                throw new EvaluatorException(exception, this);
            }
            evaluated = true;
        }
    }

    @Override
    public boolean asBoolean() {
        throw new EvaluatorException("Cannot convert a float to a boolean", this);
    }

    @Override
    public byte asByte() {
        return (byte) asFloat();
    }

    @Override
    public short asShort() {
        return (short) asFloat();
    }

    @Override
    public char asChar() {
        return (char) asFloat();
    }

    @Override
    public int asInt() {
        return (int) asFloat();
    }

    @Override
    public long asLong() {
        return (long) asFloat();
    }

    @Override
    public float asFloat() {
        evaluate();
        return value;
    }

    @Override
    public double asDouble() {
        return asFloat();
    }

    @Override
    public Float asObject() {
        return asFloat();
    }

    @Override
    public String asString() {
        return Float.toString(asFloat());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T as() {
        return (T) asObject();
    }

    @Override
    public ValueKind getKind() {
        return ValueKind.FLOAT;
    }

    @Override
    public boolean isPrimitive() {
        return true;
    }

    @Override
    public Class<?> getTypeClass() {
        return float.class;
    }

    @Override
    public Type getType(Environment environment) {
        evaluate();
        return PrimitiveType.THE_FLOAT;
    }

    @Override
    public Value getValue(Environment environment) {
        return this;
    }
}
