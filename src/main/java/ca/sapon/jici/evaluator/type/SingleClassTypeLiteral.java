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

import ca.sapon.jici.util.ReflectionUtil;

/**
 *
 */
public class SingleClassTypeLiteral extends SingleClassType {
    private final Class<?> type;

    private SingleClassTypeLiteral(Class<?> type) {
        this.type = type;
    }

    @Override
    public String getName() {
        return type.getCanonicalName();
    }

    @Override
    public Class<?> getTypeClass() {
        return type;
    }

    @Override
    public SingleClassType asArray(int dimensions) {
        return of(ReflectionUtil.asArrayType(type, dimensions));
    }

    @Override
    public boolean convertibleTo(Type to) {
        return to instanceof ConcreteType && convertibleTo(type, ((ConcreteType) to).getTypeClass());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof SingleClassType) && this.type == ((SingleClassType) other).getTypeClass();
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    public static SingleClassTypeLiteral of(Class<?> type) {
        return new SingleClassTypeLiteral(type);
    }
}
