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
package ca.sapon.jici.decoder;

import ca.sapon.jici.SourceMetadata;
import ca.sapon.jici.util.StringUtil;

/**
 *
 */
public final class Decoder {
    private Decoder() {
    }

    public static String decode(String source) {
        return decode(source, SourceMetadata.NONE);
    }

    public static String decode(String source, SourceMetadata metadata) {
        final int length = source.length();
        final char[] chars = new char[length];
        int escapes = 0;
        int i = 0, j = 0;
        while (i < length) {
            char c = source.charAt(i);
            final int advance;
            if (c == '\\') {
                escapes++;
                advance = 1;
            } else {
                if ((escapes & 1) == 1 && c == 'u') {
                    final int end = Math.min(i + 5, length);
                    try {
                        c = StringUtil.decodeUnicodeEscape(source.substring(i + 1, end));
                    } catch (IllegalArgumentException exception) {
                        throw new DecoderException("Malformed unicode escape", source.substring(i - 1, end), i - 1, end - 1);
                    }
                    j--;
                    advance = 5;
                } else {
                    advance = 1;
                }
                escapes = 0;
            }
            chars[j] = c;
            metadata.mapDecodedChar(i, j);
            i += advance;
            j++;
        }
        return String.valueOf(chars, 0, j);
    }
}
