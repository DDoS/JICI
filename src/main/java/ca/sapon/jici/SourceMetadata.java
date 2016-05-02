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
package ca.sapon.jici;

import ca.sapon.jici.lexer.LexerException;
import ca.sapon.jici.parser.ParserException;

/**
 *
 */
public class SourceMetadata {
    public static final SourceMetadata NONE = new SourceMetadata("") {
        @Override
        public void mapDecodedChar(int sourceIndex, int decodedIndex) {
        }

        @Override
        public ErrorInformation generateErrorInformation(SourceException exception) {
            throw new UnsupportedOperationException("No metadata is available to generate a message");
        }
    };
    private final String source;
    private final int[] decodedMap;

    public SourceMetadata(String source) {
        this.source = source;
        this.decodedMap = new int[source.length()];
        // initialize the map as 1:1
        for (int i = 0; i < decodedMap.length; i++) {
            decodedMap[i] = i;
        }
    }

    public void mapDecodedChar(int sourceIndex, int decodedIndex) {
        decodedMap[decodedIndex] = sourceIndex;
    }

    public ErrorInformation generateErrorInformation(SourceException exception) {
        int start = exception.getStart();
        int end = exception.getEnd();
        if (exception instanceof LexerException || exception instanceof ParserException) {
            final int length = source.length();
            if (start < length) {
                start = decodedMap[start];
            } else {
                start = length;
            }
            if (end < length) {
                end = decodedMap[end];
            } else {
                end = length;
            }
        }
        return generateErrorInformation(exception.getError(), exception.getOffender(), start, end);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private ErrorInformation generateErrorInformation(String error, String offender, int start, int end) {
        final int length = source.length();
        // find the line number the error occurred on
        final int lineNumber = findLine(source, Math.min(start, length - 1));
        // find start and end of line containing the offender
        int lineStart = start, lineEnd = start;
        while (--lineStart >= 0 && !isLineTerminator(source.charAt(lineStart))) {
        }
        lineStart++;
        while (++lineEnd < length && !isLineTerminator(source.charAt(lineEnd))) {
        }
        lineEnd--;
        final String line = source.substring(lineStart, Math.min(lineEnd + 1, source.length()));
        start -= lineStart;
        end -= lineStart;
        return new ErrorInformation(error, offender, line, lineNumber, start, end);
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

    public static class ErrorInformation {
        private final String message;
        private final String offender;
        private final String line;
        private final int lineNumber;
        private final int startIndex;
        private final int endIndex;
        private String asString = null;

        private ErrorInformation(String message, String offender, String line, int lineNumber, int startIndex, int endIndex) {
            this.message = message;
            this.offender = offender;
            this.line = line;
            this.lineNumber = lineNumber;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }

        public String getMessage() {
            return message;
        }

        public String getOffender() {
            return offender;
        }

        public String getLine() {
            return line;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public int getStartIndex() {
            return startIndex;
        }

        public int getEndIndex() {
            return endIndex;
        }

        @Override
        public String toString() {
            if (asString == null) {
                final StringBuilder builder = new StringBuilder();
                builder.append('"').append(message).append('"');
                if (offender != null) {
                    builder.append(" caused by ").append(offender);
                }
                builder.append(" at line: ").append(lineNumber).append(" index: ").append(startIndex);
                if (startIndex != endIndex) {
                    builder.append(" to ").append(endIndex);
                }
                builder.append(" in \n").append(line).append('\n');
                for (int i = 0; i < startIndex; i++) {
                    builder.append(' ');
                }
                if (startIndex == endIndex) {
                    builder.append('^');
                } else {
                    for (int i = startIndex; i <= endIndex; i++) {
                        builder.append('~');
                    }
                }
                asString = builder.toString();
            }
            return asString;
        }
    }
}
