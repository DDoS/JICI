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
package ca.sapon.jici.lexer;

public abstract class NumberLiteral extends Literal {
    protected NumberLiteral(String source) {
        super(source);
    }

    public static NumberLiteral get(String source) {
        // is a floating point if
        //   - has a decimal separator
        //   - has a p or P
        //   - doesn't start with 0x, 0X, 0b or OB and ends with f, F, d, D
        // else is an integer
        // is a float if ends with f or F, else double
        // is an long if ends with l or L, else int
        boolean hasDecimalSeparator = false;
        boolean hasP = false;
        final int length = source.length();
        for (int i = 0; i < length; i++) {
            final char c = source.charAt(i);
            if (c == '.') {
                hasDecimalSeparator = true;
            } else if (c == 'p') {
                hasP = true;
            }
        }
        if (hasDecimalSeparator || hasP) {
            return getFloatingPoint(source);
        }
        if (length > 1 && source.charAt(0) == '0' && equalsNoCase(source.charAt(1), 'x')) {
            return getInteger(source);
        }
        final char end = source.charAt(length - 1);
        if (equalsNoCase(end, 'f')) {
            return new FloatLiteral(source);
        } else if (equalsNoCase(end, 'd')) {
            return new DoubleLiteral(source);
        }
        return getInteger(source);
    }

    private static NumberLiteral getInteger(String source) {
        return endsWithNoCase(source, 'l') ? new LongLiteral(source) : new IntLiteral(source);
    }

    private static NumberLiteral getFloatingPoint(String source) {
        return endsWithNoCase(source, 'f') ? new FloatLiteral(source) : new DoubleLiteral(source);
    }

    private static boolean endsWithNoCase(String source, char end) {
        return equalsNoCase(source.charAt(source.length() - 1), end);
    }

    private static boolean equalsNoCase(char a, char b) {
        return Character.toLowerCase(a) == b;
    }
}
