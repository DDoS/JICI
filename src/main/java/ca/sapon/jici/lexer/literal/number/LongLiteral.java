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

public class LongLiteral extends NumberLiteral {
    private long value = 0;
    private boolean evaluated = false;

    public LongLiteral(String source, int index) {
        super(TokenID.LITERAL_LONG, source, index);
    }

    private LongLiteral(String source, int start, int end) {
        super(TokenID.LITERAL_LONG, source, start, end);
    }

    private void evaluate() {
        if (!evaluated) {
            String source = StringUtil.reduceSign(getSource());
            final int lastIndex = source.length() - 1;
            if (StringUtil.equalsNoCaseASCII(source.charAt(lastIndex), 'l')) {
                source = source.substring(0, lastIndex);
            }
            final int radix = StringUtil.findRadix(source);
            try {
                source = StringUtil.removeRadixIdentifier(source, radix);
                value = Long.parseLong(source, radix);
            } catch (Exception exception) {
                throw new EvaluatorException(exception.getMessage(), this);
            }
            evaluated = true;
        }
    }

    @Override
    public boolean asBoolean() {
        throw new EvaluatorException("Cannot convert a long to a boolean", this);
    }

    @Override
    public byte asByte() {
        return (byte) asLong();
    }

    @Override
    public short asShort() {
        return (short) asLong();
    }

    @Override
    public char asChar() {
        return (char) asLong();
    }

    @Override
    public int asInt() {
        return (int) asLong();
    }

    @Override
    public long asLong() {
        evaluate();
        return value;
    }

    @Override
    public float asFloat() {
        return asLong();
    }

    @Override
    public double asDouble() {
        return asLong();
    }

    @Override
    public Long asObject() {
        return asLong();
    }

    @Override
    public String asString() {
        return Long.toString(asLong());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T as() {
        return (T) asObject();
    }

    @Override
    public ValueKind getKind() {
        return ValueKind.LONG;
    }

    @Override
    public boolean isPrimitive() {
        return true;
    }

    @Override
    public Class<?> getTypeClass() {
        return long.class;
    }

    @Override
    public Type getType(Environment environment) {
        evaluate();
        return PrimitiveType.THE_LONG;
    }

    @Override
    public Value getValue(Environment environment) {
        return this;
    }

    public LongLiteral withSign(Symbol sign) {
        final TokenID id = sign.getID();
        if (id != TokenID.SYMBOL_MINUS && id != TokenID.SYMBOL_PLUS) {
            throw new UnsupportedOperationException("Expected a sign symbol");
        }
        return new LongLiteral(sign.getSource() + (sign.getSource().charAt(0) == getSource().charAt(0) ? ' ' : "") + getSource(), sign.getIndex(), getEnd());
    }
}
