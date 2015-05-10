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

/**
 *
 */
public class ObjectUnionValueType extends ObjectValueType {
    private final ObjectValueType type1;
    private final ObjectValueType type2;

    public ObjectUnionValueType(ObjectValueType type1, ObjectValueType type2) {
        super(type1.is(type2.getTypeClass()) ? type1.getTypeClass() : Object.class);
        this.type1 = type1;
        this.type2 = type2;
    }

    @Override
    public boolean convertibleTo(Class<?> to) {
        return type1.convertibleTo(to) && type2.convertibleTo(to);
    }

    @Override
    public Field getField(String name) {
        final Field field1 = type1.getField(name);
        final Field field2 = type2.getField(name);
        return !field1.equals(field2) ? null : field1;
    }

    @Override
    public Method getMethod(String name, ValueType[] arguments) {
        final Method method1 = type1.getMethod(name, arguments);
        final Method method2 = type1.getMethod(name, arguments);
        return !method1.equals(method2) ? null : method1;
    }
}
