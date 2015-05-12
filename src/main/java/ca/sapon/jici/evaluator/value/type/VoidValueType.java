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
public class VoidValueType implements ValueType {
    public static final VoidValueType THE_VOID = new VoidValueType();

    private VoidValueType() {
    }

    @Override
    public Class<?> getTypeClass() {
        return void.class;
    }

    @Override
    public String getName() {
        return "void";
    }

    @Override
    public ValueKind getKind() {
        return ValueKind.VOID;
    }

    @Override
    public boolean is(Class<?> type) {
        return type == void.class;
    }

    @Override
    public boolean isVoid() {
        return true;
    }

    @Override
    public boolean isNull() {
        return false;
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
        return false;
    }

    @Override
    public ValueType unbox() {
        return this;
    }

    @Override
    public ObjectValueType box() {
        return ObjectValueType.of(Void.class);
    }

    @Override
    public boolean canNarrowTo(int value) {
        return false;
    }

    @Override
    public PrimitiveValueType unaryWiden() {
        throw new IllegalArgumentException("Cannot unary widen the void type");
    }

    @Override
    public PrimitiveValueType binaryWiden(Class<?> with) {
        throw new IllegalArgumentException("Cannot unary widen the void type");
    }

    @Override
    public boolean convertibleTo(Class<?> to) {
        throw new IllegalArgumentException("Cannot convert the void type");
    }

    @Override
    public Field getField(String name) {
        throw new IllegalArgumentException("Cannot dereference void");
    }

    @Override
    public Method getMethod(String name, ValueType[] arguments) {
        throw new IllegalArgumentException("Cannot dereference void");
    }

    @Override
    public String toString() {
        return getName();
    }
}
