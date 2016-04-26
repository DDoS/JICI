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
package ca.sapon.jici.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

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

    public static String toString(Object[] elements, String separator) {
        return toString(Arrays.asList(elements), separator);
    }

    public static String toString(Collection<?> elements, String separator) {
        final int size = elements.size() - 1;
        if (size >= 0) {
            final StringBuilder builder = new StringBuilder();
            final Iterator<?> iterator = elements.iterator();
            for (int i = 0; i < size; i++) {
                builder.append(iterator.next());
                builder.append(separator);
            }
            return builder.append(iterator.next()).toString();
        }
        return "";
    }

    public static String repeat(String string, int number) {
        final StringBuilder builder = new StringBuilder();
        repeat(string, number, builder);
        return builder.toString();
    }

    public static void repeat(String string, int number, StringBuilder to) {
        for (int i = 0; i < number; i++) {
            to.append(string);
        }
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

    public static int findRadix(String source) {
        if (source.length() < 2) {
            return 10;
        }
        if (source.charAt(0) == '0') {
            switch (source.charAt(1)) {
                case 'b':
                case 'B':
                    return 2;
                case 'x':
                case 'X':
                    return 16;
                default:
                    return 8;
            }
        }
        return 10;
    }

    public static String removeRadixIdentifier(String source, int radix) {
        switch (radix) {
            case 2:
            case 16:
                return source.substring(2);
            case 8:
                return source.substring(1);
            case 10:
                return source;
            default:
                throw new IllegalArgumentException("No known radix identifier for " + radix);
        }
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

    public static char decodeJavaEscape(char escape) {
        switch (escape) {
            case 'b':
                return '\b';
            case 't':
                return '\t';
            case 'n':
                return '\n';
            case 'f':
                return '\f';
            case 'r':
                return '\r';
            case '"':
                return '\"';
            case '\'':
                return '\'';
            case '\\':
                return '\\';
            case '0':
                return '\0';
            case '1':
                return '\1';
            case '2':
                return '\2';
            case '3':
                return '\3';
            case '4':
                return '\4';
            case '5':
                return '\6';
            case '6':
                return '\6';
            case '7':
                return '\7';
            default:
                throw new IllegalArgumentException("'" + escape + "' is neither b, t, n, f, r, \", ', \\, 0, 1, 2, 3, 4, 5, 6 or 7");
        }
    }

    public static char decodeUnicodeEscape(String escape) {
        // format: XXXX where X is a hexadecimal digit
        if (escape.length() == 4) {
            int digit = getDigitValue(escape.charAt(3), 16);
            if (digit >= 0) {
                int value = digit;
                digit = getDigitValue(escape.charAt(2), 16);
                if (digit >= 0) {
                    value += digit << 4;
                    digit = getDigitValue(escape.charAt(1), 16);
                    if (digit >= 0) {
                        value += digit << 8;
                        digit = getDigitValue(escape.charAt(0), 16);
                        if (digit >= 0) {
                            value += digit << 12;
                            return (char) value;
                        }
                    }
                }
            }
        }
        throw new IllegalArgumentException("Expected 4 hexadecimal digits, got: " + escape);
    }

    public static String escapeCharacter(char offender) {
        final String string;
        if (Character.isWhitespace(offender)) {
            string = Character.getName(offender);
        } else {
            string = String.valueOf(offender);
        }
        return '\'' + string + '\'';
    }
}
