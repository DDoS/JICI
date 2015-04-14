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

import java.util.AbstractList;
import java.util.List;
import java.util.Stack;

public class OffsetStackList<E> extends AbstractList<E> {
    private final List<E> list;
    private final Stack<Integer> offsets = new Stack<>();
    private int topOffset = 0;

    public OffsetStackList(List<E> list) {
        this.list = list;
    }

    public int popOffset() {
        return topOffset = offsets.pop();
    }

    public void pushOffset() {
        offsets.push(topOffset);
    }

    public int peekOffset() {
        return topOffset = offsets.peek();
    }

    public int incrementOffset(int increment) {
        return topOffset += increment;
    }

    @Override
    public E get(int index) {
        return list.get(index + topOffset);
    }

    @Override
    public int size() {
        return list.size() - topOffset;
    }

    @Override
    public E set(int index, E element) {
        return list.set(index + topOffset, element);
    }

    @Override
    public void add(int index, E element) {
        list.add(index + topOffset, element);
    }

    @Override
    public E remove(int index) {
        return list.remove(index + topOffset);
    }
}
