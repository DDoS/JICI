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
import ca.sapon.jici.evaluator.value.type.PrimitiveValueType;
import ca.sapon.jici.evaluator.value.type.ValueType;
import ca.sapon.jici.lexer.TokenID;
import ca.sapon.jici.util.StringUtil;

public class CharacterLiteral extends Literal {
    private char value = '\0';
    private boolean evaluated = false;

    private CharacterLiteral(String source, int index) {
        super(TokenID.LITERAL_CHARACTER, source, index);
    }

    private void evaluate() {
        if (!evaluated) {
            String source = getSource();
            source = source.substring(1, source.length() - 1);
            // format: X or \X where X is any character
            switch (source.length()) {
                case 1:
                    value = source.charAt(0);
                    evaluated = true;
                    return;
                case 2:
                    if (source.charAt(0) == '\\') {
                        try {
                            value = StringUtil.decodeJavaEscape(source.charAt(1));
                        } catch (IllegalArgumentException exception) {
                            throw new EvaluatorException(exception.getMessage(), this);
                        }
                        evaluated = true;
                        return;
                    }
            }
            throw new EvaluatorException("Malformed character literal: '" + source + "'", this);
        }
    }

    @Override
    public boolean asBoolean() {
        throw new EvaluatorException("Cannot convert a char to a boolean", this);
    }

    @Override
    public byte asByte() {
        return (byte) asChar();
    }

    @Override
    public short asShort() {
        return (short) asChar();
    }

    @Override
    public char asChar() {
        evaluate();
        return value;
    }

    @Override
    public int asInt() {
        return asChar();
    }

    @Override
    public long asLong() {
        return asChar();
    }

    @Override
    public float asFloat() {
        return asChar();
    }

    @Override
    public double asDouble() {
        return asChar();
    }

    @Override
    public Character asObject() {
        return asChar();
    }

    @Override
    public String asString() {
        return Character.toString(asChar());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T as() {
        return (T) asObject();
    }

    @Override
    public ValueKind getKind() {
        return ValueKind.CHAR;
    }

    @Override
    public boolean isPrimitive() {
        return true;
    }

    @Override
    public Class<?> getTypeClass() {
        return char.class;
    }

    @Override
    public ValueType getValueType(Environment environment) {
        return PrimitiveValueType.of(getTypeClass());
    }

    @Override
    public Value getValue(Environment environment) {
        return this;
    }

    public static CharacterLiteral from(String source, int index) {
        return new CharacterLiteral(source, index);
    }
}
