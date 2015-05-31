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
package ca.sapon.jici.test;

import ca.sapon.jici.decoder.Decoder;
import ca.sapon.jici.decoder.DecoderException;
import org.junit.Assert;
import org.junit.Test;

public class DecoderTest {
    @Test
    public void test() {
        testDecode("abcd efgh", "abcd efgh");
        testDecode("abcd\\efgh", "abcd\\efgh");
        testDecode("\\u0041", "A");
        testDecode("\\u005C", "\\");
        testDecode("\\\\u005C", "\\\\u005C");
        testDecode("abcd\\u0020efgh", "abcd efgh");

        try {
            Decoder.decode("\\u05G");
            Assert.fail();
        } catch (DecoderException ignored) {
        }

        try {
            Decoder.decode("\\u05C");
            Assert.fail();
        } catch (DecoderException ignored) {
        }

        try {
            Decoder.decode("\\u");
            Assert.fail();
        } catch (DecoderException ignored) {
        }
    }

    private void testDecode(String source, String expected) {
        Assert.assertEquals(expected, Decoder.decode(source));
    }
}
