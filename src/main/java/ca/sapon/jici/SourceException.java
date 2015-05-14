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
package ca.sapon.jici;

/**
 *
 */
public class SourceException extends RuntimeException {
    private static final long serialVersionUID = 1;
    private final String error;
    private final String offender;
    private final int index;

    public SourceException(String error, String offender, int index) {
        super(generateMessage(error, offender, index));
        this.error = error;
        this.offender = offender;
        this.index = index;
    }

    public String getError() {
        return error;
    }

    public String getOffender() {
        return offender;
    }

    public int getIndex() {
        return index;
    }

    private static String generateMessage(String error, String offender, int index) {
        final StringBuilder builder = new StringBuilder().append('"').append(error).append('"');
        if (offender != null) {
            builder.append(" caused by \"").append(offender).append('"');
        }
        builder.append(" at index: ").append(index);
        return builder.toString();
    }

    private static String generateMessage(String error, String source, String offender, int index) {
        final int line = findLine(source, index);
        // find start and end of line containing the offender
        int start = index, end = index - 1;
        while (--start >= 0 && !isLineTerminator(source.charAt(start))) {
        }
        while (++end < source.length() && !isLineTerminator(source.charAt(end))) {
        }
        source = source.substring(start + 1, end);
        index -= start;
        // build the error message with source and cursor lines
        final StringBuilder builder = new StringBuilder()
                .append('"').append(error).append('"')
                .append(" caused by \"").append(offender).append('"')
                .append(" at line: ").append(line).append(" index: ").append(index)
                .append(" in \n").append(source).append('\n');
        for (int i = 0; i < index - 1; i++) {
            builder.append(' ');
        }
        builder.append('^');
        return builder.toString();
    }

    private static int findLine(String source, int index) {
        int line = 0;
        for (int i = 0; i <= index; i++) {
            final char c = source.charAt(i);
            if (isLineTerminator(c)) {
                i = consumeLineTerminator(source, i);
                line++;
            }
        }
        return line;
    }

    private static int consumeLineTerminator(String source, int i) {
        final char c = source.charAt(i);
        if (c == '\n') {
            // LF
            i++;
        } else if (c == '\r') {
            // CR
            if (++i < source.length() && source.charAt(i) == '\n') {
                // CR + LF
                ++i;
            }
        }
        return i;
    }

    private static boolean isLineTerminator(char c) {
        return c == '\n' || c == '\r';
    }
}
