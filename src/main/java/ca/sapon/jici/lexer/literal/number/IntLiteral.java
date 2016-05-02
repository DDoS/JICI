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
import ca.sapon.jici.lexer.Symbol;
import ca.sapon.jici.lexer.TokenID;
import ca.sapon.jici.util.StringUtil;

public class IntLiteral extends NumberLiteral {
    private int value = 0;
    private boolean evaluated = false;

    public IntLiteral(String source, int index) {
        super(TokenID.LITERAL_INT, source, index);
    }

    private IntLiteral(String source, int start, int end) {
        super(TokenID.LITERAL_INT, source, start, end);
    }

    private void evaluate() {
        if (evaluated) {
            return;
        }
        String source = StringUtil.reduceSign(getSource());
        final int radix = StringUtil.findRadix(source);
        try {
            source = StringUtil.removeRadixIdentifier(source, radix);
            value = Integer.parseInt(source, radix);
        } catch (Exception exception) {
            throw new EvaluatorException(exception.getMessage(), this);
        }
        evaluated = true;
    }

    @Override
    public boolean asBoolean() {
        throw new EvaluatorException("Cannot convert an int to a boolean", this);
    }

    @Override
    public byte asByte() {
        return (byte) asInt();
    }

    @Override
    public short asShort() {
        return (short) asInt();
    }

    @Override
    public char asChar() {
        return (char) asInt();
    }

    @Override
    public int asInt() {
        evaluate();
        return value;
    }

    @Override
    public long asLong() {
        return asInt();
    }

    @Override
    public float asFloat() {
        return asInt();
    }

    @Override
    public double asDouble() {
        return asInt();
    }

    @Override
    public Integer asObject() {
        return asInt();
    }

    @Override
    public String asString() {
        return Integer.toString(asInt());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T as() {
        return (T) asObject();
    }

    @Override
    public ValueKind getKind() {
        return ValueKind.INT;
    }

    @Override
    public Class<?> getTypeClass() {
        return int.class;
    }

    @Override
    public Type getType(Environment environment) {
        evaluate();
        return PrimitiveType.THE_INT;
    }

    @Override
    public boolean isPrimitive() {
        return true;
    }

    @Override
    public Value getValue(Environment environment) {
        return this;
    }

    public IntLiteral withSign(Symbol sign) {
        final TokenID id = sign.getID();
        if (id != TokenID.SYMBOL_MINUS && id != TokenID.SYMBOL_PLUS) {
            throw new UnsupportedOperationException("Expected a sign symbol");
        }
        return new IntLiteral(sign.getSource() + (sign.getSource().charAt(0) == getSource().charAt(0) ? ' ' : "") + getSource(), sign.getIndex(), getEnd());
    }
}
