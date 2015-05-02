/**
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

public class StringLiteral extends Literal implements Value {
    private String value = null;

    public StringLiteral(String source) {
        super(TokenID.LITERAL_STRING, source.substring(1, source.length() - 1));
    }

    private void evaluate() {
        if (value == null) {
            final String source = getSource();
            final int length = source.length();
            final char[] chars = new char[length];
            int count = 0;
            boolean escaped = false;
            for (int i = 0; i < length; i++) {
                if (escaped) {
                    chars[count++] = StringUtil.decodeJavaEscape(source.charAt(i));
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
        throw new IllegalArgumentException("Cannot cast an object to a boolean");
    }

    @Override
    public byte asByte() {
        throw new IllegalArgumentException("Cannot cast an object to a byte");
    }

    @Override
    public short asShort() {
        throw new IllegalArgumentException("Cannot cast an object to a short");
    }

    @Override
    public char asChar() {
        throw new IllegalArgumentException("Cannot cast an object to a char");
    }

    @Override
    public int asInt() {
        throw new IllegalArgumentException("Cannot cast an object to an int");
    }

    @Override
    public long asLong() {
        throw new IllegalArgumentException("Cannot cast an object to a long");
    }

    @Override
    public float asFloat() {
        throw new IllegalArgumentException("Cannot cast an object to a float");
    }

    @Override
    public double asDouble() {
        throw new IllegalArgumentException("Cannot cast an object to a double");
    }

    @Override
    public String asObject() {
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
    public String toString() {
        return "\"" + getSource() + "\"";
    }

    @Override
    public Value getValue(Environment environment) {
        return this;
    }
}
