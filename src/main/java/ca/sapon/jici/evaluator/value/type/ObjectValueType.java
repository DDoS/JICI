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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import ca.sapon.jici.evaluator.value.ValueKind;
import ca.sapon.jici.util.ReflectionUtil;
import ca.sapon.jici.util.StringUtil;

/**
 *
 */
public class ObjectValueType implements ValueType {
    public static final ObjectValueType THE_STRING = ObjectValueType.of(String.class);
    public static final ObjectValueType THE_OBJECT = ObjectValueType.of(Object.class);
    private static final Map<Class<?>, Class<?>> UNBOXED_TYPES = new HashMap<>();
    private Class<?> type;
    private ValueType unbox = null;

    static {
        UNBOXED_TYPES.put(Boolean.class, boolean.class);
        UNBOXED_TYPES.put(Byte.class, byte.class);
        UNBOXED_TYPES.put(Short.class, short.class);
        UNBOXED_TYPES.put(Character.class, char.class);
        UNBOXED_TYPES.put(Integer.class, int.class);
        UNBOXED_TYPES.put(Long.class, long.class);
        UNBOXED_TYPES.put(Float.class, float.class);
        UNBOXED_TYPES.put(Double.class, double.class);
        UNBOXED_TYPES.put(Void.class, void.class);
    }

    protected ObjectValueType(Class<?> type) {
        this.type = type;
    }

    @Override
    public Class<?> getTypeClass() {
        return type;
    }

    @Override
    public String getName() {
        return type.getCanonicalName();
    }

    @Override
    public ValueKind getKind() {
        return ValueKind.OBJECT;
    }

    @Override
    public boolean is(ValueType type) {
        return type instanceof ObjectValueType && this.type == ((ObjectValueType) type).getTypeClass();
    }

    @Override
    public boolean isVoid() {
        return false;
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
        return type.isArray();
    }

    @Override
    public boolean isObject() {
        return true;
    }

    @Override
    public ValueType unbox() {
        if (unbox == null) {
            final Class<?> unboxClass = unbox(type);
            if (unboxClass == void.class) {
                unbox = VoidValueType.THE_VOID;
            } else if (unboxClass != null && unboxClass.isPrimitive()) {
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
    public boolean canNarrowFrom(int value) {
        return false;
    }

    @Override
    public PrimitiveValueType unaryWiden() {
        throw new IllegalArgumentException("Cannot unary widen an object type");
    }

    @Override
    public PrimitiveValueType binaryWiden(ValueType with) {
        throw new IllegalArgumentException("Cannot binary widen an object type");
    }

    @Override
    public boolean convertibleTo(ValueType to) {
        if (to instanceof ObjectUnionValueType) {
            throw new IllegalArgumentException("Cannot convert to an object union type");
        }
        return convertibleTo(type, to.getTypeClass());
    }

    @Override
    public Constructor<?> getConstructor(ValueType[] arguments) {
        // try to find a matching constructor
        final Constructor<?>[] constructors = type.getConstructors();
        // look for matches in length
        final int argumentCount = arguments.length;
        final Map<Constructor<?>, Class<?>[]> candidates = new HashMap<>();
        for (Constructor<?> candidate : constructors) {
            final Class<?>[] parameterTypes = candidate.getParameterTypes();
            if (parameterTypes.length == argumentCount) {
                candidates.put(candidate, parameterTypes);
            }
        }
        // try to resolve the overloads
        final Constructor<?> constructor = ReflectionUtil.resolveOverloads(candidates, arguments);
        if (constructor == null) {
            failGetConstructor(arguments);
        }
        return constructor;
    }

    @Override
    public Constructor<?> getVarargConstructor(ValueType[] arguments) {
        // try to find a matching constructor
        final Constructor<?>[] constructors = type.getConstructors();
        // look for varargs with match in length of non-varargs
        final int argumentCount = arguments.length;
        final Map<Constructor<?>, Class<?>[]> candidates = new HashMap<>();
        for (Constructor<?> candidate : constructors) {
            if (candidate.isVarArgs()) {
                final Class<?>[] parameterTypes = candidate.getParameterTypes();
                if (parameterTypes.length - 1 <= argumentCount) {
                    // expand the parameters through the vararg to match the argument count
                    candidates.put(candidate, ReflectionUtil.expandsVarargs(parameterTypes, argumentCount));
                }
            }
        }
        // try to resolve the overloads
        final Constructor<?> constructor = ReflectionUtil.resolveOverloads(candidates, arguments);
        if (constructor == null) {
            failGetConstructor(arguments);
        }
        return constructor;
    }

    private void failGetConstructor(ValueType[] arguments) {
        throw new IllegalArgumentException("No constructor for signature: "
                + "(" + StringUtil.toString(Arrays.asList(arguments), ", ") + ") in " + getName());
    }

    @Override
    public Field getField(String name) {
        try {
            return type.getField(name);
        } catch (NoSuchFieldException exception) {
            throw new IllegalArgumentException("No field named " + name + " in " + getName());
        }
    }

    @Override
    public Method getMethod(String name, ValueType[] arguments) {
        // try to find a matching method without varargs
        final Method[] methods = type.getMethods();
        // look for matches in length and name
        final int argumentCount = arguments.length;
        final Map<Method, Class<?>[]> candidates = new HashMap<>();
        for (Method candidate : methods) {
            if (candidate.getName().equals(name)) {
                final Class<?>[] parameterTypes = candidate.getParameterTypes();
                if (parameterTypes.length == argumentCount) {
                    candidates.put(candidate, parameterTypes);
                }
            }
        }
        // try to resolve the overloads
        final Method method = ReflectionUtil.resolveOverloads(candidates, arguments);
        if (method == null) {
            failGetMethod(name, arguments);
        }
        return method;
    }

    @Override
    public Method getVarargMethod(String name, ValueType[] arguments) {
        // try to find a matching method with varargs
        final Method[] methods = type.getMethods();
        // look for varargs with matches in name and length of non-varargs
        final int argumentCount = arguments.length;
        final Map<Method, Class<?>[]> candidates = new HashMap<>();
        for (Method candidate : methods) {
            if (candidate.isVarArgs() && candidate.getName().equals(name)) {
                final Class<?>[] parameterTypes = candidate.getParameterTypes();
                if (parameterTypes.length - 1 <= argumentCount) {
                    // expand the parameters through the vararg to match the argument count
                    candidates.put(candidate, ReflectionUtil.expandsVarargs(parameterTypes, argumentCount));
                }
            }
        }
        // try to resolve the overloads
        final Method method = ReflectionUtil.resolveOverloads(candidates, arguments);
        if (method == null) {
            failGetMethod(name, arguments);
        }
        return method;
    }

    private void failGetMethod(String name, ValueType[] arguments) {
        throw new IllegalArgumentException("No method for signature: "
                + name + "(" + StringUtil.toString(Arrays.asList(arguments), ", ") + ") in " + getName());
    }

    @Override
    public String toString() {
        return getName();
    }

    public static Class<?> unbox(Class<?> type) {
        final Class<?> unboxed = UNBOXED_TYPES.get(type);
        return unboxed == null ? type : unboxed;
    }

    public static boolean convertibleTo(Class<?> from, Class<?> to) {
        if (to.isPrimitive()) {
            from = unbox(from);
            if (from.isPrimitive()) {
                return PrimitiveValueType.convertibleTo(from, to);
            }
            to = PrimitiveValueType.box(to).getTypeClass();
        }
        return to.isAssignableFrom(from);
    }

    public static ObjectValueType of(Class<?> type) {
        return new ObjectValueType(type);
    }
}
