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

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class ObjectValueType implements ValueType {
    private static final Map<Class<?>, Class<?>> UNBOXED_TYPES = new HashMap<>();
    protected Class<?> type;
    protected ValueType unbox = null;

    static {
        UNBOXED_TYPES.put(Boolean.class, boolean.class);
        UNBOXED_TYPES.put(Byte.class, byte.class);
        UNBOXED_TYPES.put(Short.class, short.class);
        UNBOXED_TYPES.put(Character.class, char.class);
        UNBOXED_TYPES.put(Integer.class, int.class);
        UNBOXED_TYPES.put(Long.class, long.class);
        UNBOXED_TYPES.put(Float.class, float.class);
        UNBOXED_TYPES.put(Double.class, double.class);
    }

    public ObjectValueType(Class<?> type) {
        this.type = type;
    }

    @Override
    public Class<?> getClassType() {
        return type;
    }

    @Override
    public String getName() {
        return type == null ? "null" : type.getCanonicalName();
    }

    @Override
    public boolean is(Class<?> type) {
        return this.type == type;
    }

    @Override
    public boolean isNull() {
        return type == null;
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
    public boolean isLogical() {
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
    public boolean isObject() {
        return true;
    }

    @Override
    public ValueType unbox() {
        if (unbox == null) {
            final Class<?> unboxClass = unbox(type);
            if (unboxClass != null && unboxClass.isPrimitive()) {
                unbox = PrimitiveValueType.of(unboxClass);
            } else {
                unbox = this;
            }
        }
        return unbox;
    }

    @Override
    public ObjectValueType box() {
        return this;
    }

    @Override
    public boolean canNarrowTo(int value) {
        return false;
    }

    @Override
    public PrimitiveValueType unaryWiden() {
        throw new IllegalArgumentException("Cannot unary widen an object type");
    }

    @Override
    public PrimitiveValueType binaryWiden(Class<?> with) {
        throw new IllegalArgumentException("Cannot binary widen an object type");
    }

    @Override
    public boolean convertibleTo(Class<?> to) {
        return convertibleTo(type, to);
    }

    public static Class<?> unbox(Class<?> type) {
        final Class<?> unboxed = UNBOXED_TYPES.get(type);
        return unboxed == null ? type : unboxed;
    }

    public static boolean convertibleTo(Class<?> from, Class<?> to) {
        if (to.isPrimitive()) {
            return PrimitiveValueType.convertibleTo(unbox(from), to);
        }
        return from == null || to.isAssignableFrom(from);
    }
}
