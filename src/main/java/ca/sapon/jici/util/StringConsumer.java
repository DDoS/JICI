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

public class StringConsumer {
    private final String string;
    private int position = 0;

    public StringConsumer(String string) {
        this.string = string;
    }

    public String string() {
        return string;
    }

    public int position() {
        return position;
    }

    public boolean consume() {
        return ++position < string.length();
    }

    public boolean expel() {
        return --position >= string.length();
    }

    public boolean has() {
        return has(0);
    }

    public boolean has(int offset) {
        return position + offset < string.length();
    }

    public char get() {
        return get(0);
    }

    public char get(int offset) {
        return string.charAt(position + offset);
    }

    public String consumed() {
        return consumed(0);
    }

    public String consumed(int offset) {
        return string.substring(offset, position);
    }
}
