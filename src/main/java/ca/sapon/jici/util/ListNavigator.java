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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class ListNavigator<E> {
    private final List<E> list;
    private final Deque<Integer> positions = new ArrayDeque<>();
    private int topPosition = 0;
    private int fractionalPosition = 0;

    public ListNavigator(List<E> list) {
        this.list = list;
    }

    public int position() {
        return topPosition;
    }

    public int popPosition() {
        return topPosition = positions.pop();
    }

    public void pushPosition() {
        positions.push(topPosition);
    }

    public int peekPosition() {
        return topPosition = positions.peek();
    }

    public void discardPosition() {
        positions.pop();
    }

    public int advance() {
        return advance(1);
    }

    public int advance(int increment) {
        return topPosition += increment;
    }

    public int retreat() {
        return retreat(1);
    }

    public int retreat(int increment) {
        return advance(-increment);
    }

    public int remaining() {
        return list.size() - topPosition;
    }

    public boolean has() {
        return has(1);
    }

    public boolean has(int remaining) {
        return remaining() >= remaining;
    }

    public E get() {
        return get(0);
    }

    public E get(int forward) {
        return list.get(topPosition + forward);
    }

    public int fractional() {
        return fractionalPosition;
    }

    public int advanceFractional() {
        return advanceFractional(1);
    }

    public int advanceFractional(int increment) {
        return fractionalPosition += increment;
    }

    public void closeFractional() {
        fractionalPosition = 0;
        advance();
    }
}
