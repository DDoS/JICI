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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import ca.sapon.jici.evaluator.value.ValueKind;

/**
 *
 */
public interface ValueType {
    Class<?> getTypeClass();

    String getName();

    ValueKind getKind();

    boolean is(ValueType type);

    boolean isVoid();

    boolean isNull();

    boolean isPrimitive();

    boolean isNumeric();

    boolean isIntegral();

    boolean isBoolean();

    boolean isArray();

    boolean isObject();

    ValueType unbox();

    ObjectValueType box();

    boolean canNarrowFrom(int value);

    PrimitiveValueType unaryWiden();

    PrimitiveValueType binaryWiden(ValueType with);

    boolean convertibleTo(ValueType to);

    Constructor<?> getConstructor(ValueType[] arguments);

    Field getField(String name);

    Method getMethod(String name, ValueType[] arguments);

    String toString();
}
