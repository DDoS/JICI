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
import ca.sapon.jici.util.StringUtil;

public class CharacterLiteral extends Literal implements Value {
    private char value = '\0';
    private boolean evaluated = false;

    public CharacterLiteral(String source) {
        super(TokenID.LITERAL_CHARACTER, source.substring(1, source.length() - 1));
    }

    private void evaluate() {
        if (!evaluated) {
            final String source = getSource();
            // format: X or \X where X is any character
            switch (source.length()) {
                case 1:
                    value = source.charAt(0);
                    evaluated = true;
                    return;
                case 2:
                    if (source.charAt(0) == '\\') {
                        value = StringUtil.decodeJavaEscape(source.charAt(1));
                        evaluated = true;
                        return;
                    }
            }
            throw new IllegalArgumentException("Malformed character literal: '" + source + "'");
        }
    }

    @Override
    public boolean asBoolean() {
        throw new IllegalArgumentException("Cannot cast a char to a boolean");
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
    public Class<?> getTypeClass(Environment environment) {
        return getTypeClass();
    }

    @Override
    public String toString() {
        return "'" + getSource() + "'";
    }

    @Override
    public Value getValue(Environment environment) {
        return this;
    }
}
