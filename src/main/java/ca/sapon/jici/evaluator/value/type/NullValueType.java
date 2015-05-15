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
package ca.sapon.jici.evaluator.value.type;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import ca.sapon.jici.evaluator.value.ValueKind;

/**
 *
 */
public class NullValueType implements ValueType {
    public static final NullValueType THE_NULL = new NullValueType();

    private NullValueType() {
    }

    @Override
    public Class<?> getTypeClass() {
        return null;
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
    public boolean is(Class<?> type) {
        return type == null;
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
    public ValueType unbox() {
        throw new IllegalArgumentException("Cannot unbox the null type");
    }

    @Override
    public ObjectValueType box() {
        throw new IllegalArgumentException("Cannot box the null type");
    }

    @Override
    public boolean canNarrowFrom(int value) {
        return false;
    }

    @Override
    public PrimitiveValueType unaryWiden() {
        throw new IllegalArgumentException("Cannot unary widen the null type");
    }

    @Override
    public PrimitiveValueType binaryWiden(Class<?> with) {
        throw new IllegalArgumentException("Cannot binary widen the null type");
    }

    @Override
    public boolean convertibleTo(Class<?> to) {
        return !to.isPrimitive();
    }

    @Override
    public Field getField(String name) {
        throw new IllegalArgumentException("Cannot dereference null");
    }

    @Override
    public Method getMethod(String name, ValueType[] arguments) {
        throw new IllegalArgumentException("Cannot dereference null");
    }

    @Override
    public String toString() {
        return getName();
    }
}
