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

public class LexerException extends Exception {
    private static final long serialVersionUID = 1;

    public LexerException(String error, String source, int index) {
        super(generateMessage(error, source, index));
    }

    private static String generateMessage(String error, String source, int index) {
        final String offender = escapeOffender(source.charAt(index));
        // find start and end of line containing the offender
        int start = index, end = index - 1;
        while (--start >= 0 && source.charAt(start) != '\n') {
        }
        while (++end < source.length() && source.charAt(end) != '\n') {
        }
        source = source.substring(start + 1, end);
        index -= start;
        // build the error message with source and cursor lines
        final StringBuilder builder = new StringBuilder(error)
                .append(" caused by ")
                .append(offender)
                .append(" at position ")
                .append(index)
                .append(" in \n")
                .append(source)
                .append('\n');
        for (int i = 0; i < index - 1; i++) {
            builder.append(' ');
        }
        builder.append('^');
        return builder.toString();
    }

    private static String escapeOffender(char offender) {
        if (Character.isWhitespace(offender)) {
            return Character.getName(offender);
        }
        return '\'' + String.valueOf(offender) + '\'';
    }
}
