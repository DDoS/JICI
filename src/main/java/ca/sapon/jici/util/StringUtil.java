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
package ca.sapon.jici.util;

import java.util.List;

public final class StringUtil {
    private static final int[] DIGIT_VALUES = new int[256];

    static {
        for (int i = 0; i < DIGIT_VALUES.length; i++) {
            DIGIT_VALUES[i] = -1;
        }
        for (int c = '0', i = 0; c <= '9'; c++, i++) {
            DIGIT_VALUES[c] = i;
        }
        for (int c = 'a', i = 10; c <= 'z'; c++, i++) {
            DIGIT_VALUES[c] = i;
        }
        for (int c = 'A', i = 10; c <= 'Z'; c++, i++) {
            DIGIT_VALUES[c] = i;
        }
    }

    private StringUtil() {
    }

    public static String toString(List<?> list, String separator) {
        final int size = list.size() - 1;
        if (size >= 0) {
            final StringBuilder builder = new StringBuilder();
            for (int i = 0; i < size; i++) {
                builder.append(list.get(i));
                builder.append(separator);
            }
            return builder.append(list.get(size)).toString();
        }
        return "";
    }

    public static String removeAll(String string, char remove) {
        final int length = string.length();
        final char[] chars = new char[length];
        int count = 0;
        for (int i = 0; i < length; i++) {
            final char c = string.charAt(i);
            if (c != remove) {
                chars[count++] = c;
            }
        }
        return String.valueOf(chars, 0, count);
    }

    public static int getDigitValue(char digit, int radix) {
        if (digit >= 256) {
            return -1;
        }
        final int value = DIGIT_VALUES[digit];
        return value >= radix ? -1 : value;
    }

    public static boolean equalsNoCaseASCII(char a, char b) {
        return (a & ~32) == (b & ~32);
    }

    public static char decodeUnicodeSequence(String source, int i) {
        // format: \\uXXXX where X is a hexadecimal digit
        // (starts with 1 backslash, but it needs to be escaped for this to compile)
        if (i + 6 > source.length()) {
            throw new InvalidUnicodeSequence(source.substring(i, source.length()));
        }
        if (source.charAt(i) != '\\' || source.charAt(i + 1) != 'u') {
            throw new InvalidUnicodeSequence(source.substring(i, i + 6));
        }
        // we need to make sure the first digit isn't a sign for the next step
        final char firstDigit = source.charAt(i + 2);
        if (firstDigit == '-' || firstDigit == '+') {
            throw new InvalidUnicodeSequence(source.substring(i, i + 6));
        }
        // try to parse a short and convert it to a character
        try {
            return (char) Short.parseShort(source.substring(i + 2, i + 6), 16);
        } catch (NumberFormatException exception) {
            throw new InvalidUnicodeSequence(source.substring(i, i + 6));
        }
    }

    public static class InvalidUnicodeSequence extends RuntimeException {
        private static final long serialVersionUID = 1;

        public InvalidUnicodeSequence(String sequence) {
            super("\"" + sequence + "\" is not of form \\uXXXX, where X is a hexadecimal digit");
        }
    }
}
