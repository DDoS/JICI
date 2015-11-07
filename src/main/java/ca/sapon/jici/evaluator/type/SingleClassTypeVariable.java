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
package ca.sapon.jici.evaluator.type;

import java.util.Set;

import ca.sapon.jici.util.StringUtil;

/**
 *
 */
public class SingleClassTypeVariable extends SingleClassType {
    private final String name;
    private final int dimensions;
    private final Set<SingleClassType> upperBound;

    private SingleClassTypeVariable(String name, Set<SingleClassType> upperBound, int dimensions) {
        this.name = name;
        this.dimensions = dimensions;
        this.upperBound = upperBound;
    }

    public Set<SingleClassType> getUpperBound() {
        return upperBound;
    }

    @Override
    public Class<?> getTypeClass() {
        throw new UnsupportedOperationException("Type variable " + name + " is unsolved");
    }

    @Override
    public SingleClassType asArray(int dimensions) {
        return new SingleClassTypeVariable(name, upperBound, this.dimensions + dimensions);
    }

    @Override
    public String getName() {
        return name + StringUtil.repeat("[]", dimensions);
    }

    @Override
    public boolean convertibleTo(Type to) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SingleClassTypeVariable)) {
            return false;
        }
        final SingleClassTypeVariable that = (SingleClassTypeVariable) o;
        return dimensions == that.dimensions && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + dimensions;
        return result;
    }

    public static SingleClassTypeVariable of(String name, Set<SingleClassType> upperBound) {
        return new SingleClassTypeVariable(name, upperBound, 0);
    }
}
