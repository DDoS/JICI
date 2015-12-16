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

import ca.sapon.jici.evaluator.Accessible;
import ca.sapon.jici.evaluator.Callable;
import ca.sapon.jici.evaluator.value.ValueKind;
import ca.sapon.jici.util.TypeUtil;

/**
 * The null type, for the {@code null} literal.
 */
public class NullType implements ReferenceType {
    public static final NullType THE_NULL = new NullType();

    private NullType() {
    }

    @Override
    public String getName() {
        return "null";
    }

    @Override
    public ValueKind getKind() {
        return ValueKind.OBJECT;
    }

    @Override
    public boolean isVoid() {
        return false;
    }

    @Override
    public boolean isNull() {
        return true;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public boolean isNumeric() {
        return false;
    }

    @Override
    public boolean isIntegral() {
        return false;
    }

    @Override
    public boolean isBoolean() {
        return false;
    }

    @Override
    public boolean isArray() {
        return false;
    }

    @Override
    public boolean isObject() {
        return true;
    }

    @Override
    public boolean convertibleTo(Type to) {
        return TypeUtil.convertibleTo(this, to);
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof NullType;
    }

    @Override
    public int hashCode() {
        return 9849851;
    }

    @Override
    public Callable getConstructor(Type[] arguments) {
        throw new UnsupportedOperationException("Cannot dereference the null type");
    }

    @Override
    public Accessible getField(String name) {
        throw new UnsupportedOperationException("Cannot dereference the null type");
    }

    @Override
    public Callable getMethod(String name, Type[] arguments) {
        throw new UnsupportedOperationException("Cannot dereference the null type");
    }
}
