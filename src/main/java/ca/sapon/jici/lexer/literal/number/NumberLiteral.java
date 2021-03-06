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

import ca.sapon.jici.lexer.TokenID;
import ca.sapon.jici.lexer.literal.Literal;
import ca.sapon.jici.util.StringUtil;

public abstract class NumberLiteral extends Literal {
    protected NumberLiteral(TokenID id, String source, int index) {
        super(id, source, index);
    }

    protected NumberLiteral(TokenID id, String source, int start, int end) {
        super(id, source, start, end);
    }

    public static NumberLiteral from(String source, int index) {
        int length = source.length();
        // fast track 0 integers since they are very common
        if (length == 1 && source.charAt(0) == '0') {
            return new ZeroLiteral(index);
        }
        // remove number separators
        source = StringUtil.removeAll(source, '_');
        final NumberLiteral number = disambiguateNumber(source, index);
        // update the end index to the one before removing underscores
        number.setEnd(number.getStart() + length - 1);
        return number;
    }

    private static NumberLiteral disambiguateNumber(String source, int index) {
        int length = source.length();
        // is a floating point if
        //   - has a decimal separator
        //   - has a p or P
        // is an integer if
        //   - starts with 0x, 0X, 0b or 0B
        //
        // is a float if ends with f or F, else double
        // is a double if has an e
        // is an long if ends with l or L, else int
        boolean hasDecimalSeparator = false;
        boolean hasP = false, hasE = false;
        for (int i = 0; i < length; i++) {
            final char c = source.charAt(i);
            if (c == '.') {
                hasDecimalSeparator = true;
            } else if (StringUtil.equalsNoCaseASCII(c, 'p')) {
                hasP = true;
            } else if (StringUtil.equalsNoCaseASCII(c, 'e')) {
                hasE = true;
            }
        }
        if (hasDecimalSeparator || hasP) {
            return getFloatingPoint(source, index);
        }
        if (length > 1 && source.charAt(0) == '0' && StringUtil.equalsNoCaseASCII(source.charAt(1), 'x')) {
            return getInteger(source, index);
        }
        final char end = source.charAt(length - 1);
        if (StringUtil.equalsNoCaseASCII(end, 'f')) {
            return new FloatLiteral(source, index);
        } else if (StringUtil.equalsNoCaseASCII(end, 'd')) {
            return new DoubleLiteral(source, index);
        } else if (hasE) {
            return new DoubleLiteral(source, index);
        }
        return getInteger(source, index);
    }

    private static NumberLiteral getInteger(String source, int index) {
        return endsWithNoCase(source, 'l') ? new LongLiteral(source, index) : new IntLiteral(source, index);
    }

    private static NumberLiteral getFloatingPoint(String source, int index) {
        if (endsWithNoCase(source, 'f')) {
            return new FloatLiteral(source, index);
        }
        return new DoubleLiteral(source, index);
    }

    private static boolean endsWithNoCase(String source, char end) {
        return StringUtil.equalsNoCaseASCII(source.charAt(source.length() - 1), end);
    }
}
