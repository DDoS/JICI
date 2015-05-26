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
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.evaluator.value.ValueKind;
import ca.sapon.jici.evaluator.type.ClassType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.lexer.TokenID;
import ca.sapon.jici.util.StringUtil;

public class StringLiteral extends Literal {
    private String value = null;

    private StringLiteral(String source, int index) {
        super(TokenID.LITERAL_STRING, source, index);
    }

    private void evaluate() {
        if (value == null) {
            String source = getSource();
            source = source.substring(1, source.length() - 1);
            final int length = source.length();
            final char[] chars = new char[length];
            int count = 0;
            boolean escaped = false;
            for (int i = 0; i < length; i++) {
                if (escaped) {
                    try {
                        chars[count++] = StringUtil.decodeJavaEscape(source.charAt(i));
                    } catch (IllegalArgumentException exception) {
                        throw new EvaluatorException(exception.getMessage(), this);
                    }
                    escaped = false;
                } else {
                    final char c = source.charAt(i);
                    if (c == '\\') {
                        escaped = true;
                    } else {
                        chars[count++] = c;
                        escaped = false;
                    }
                }
            }
            value = String.valueOf(chars, 0, count);
        }
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
    public String asObject() {
        return asString();
    }

    @Override
    public String asString() {
        evaluate();
        return value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T as() {
        return (T) asObject();
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
        return String.class;
    }

    @Override
    public Type getType(Environment environment) {
        return ClassType.THE_STRING;
    }

    @Override
    public Value getValue(Environment environment) {
        return this;
    }

    public static StringLiteral from(String source, int index) {
        return new StringLiteral(source, index);
    }
}
