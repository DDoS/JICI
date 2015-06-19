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
public class ClassType implements Type {
    public static final ClassType THE_STRING = ClassType.of(String.class);
    public static final ClassType THE_OBJECT = ClassType.of(Object.class);
    private static final Map<Class<?>, Class<?>> UNBOXED_TYPES = new HashMap<>();
    private Class<?> type;
    private Type unbox = null;

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

    protected ClassType(Class<?> type) {
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
    public boolean is(Type type) {
        return !(type instanceof ClassUnionType) && this.type == type.getTypeClass();
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
    public Type unbox() {
        if (unbox == null) {
            final Class<?> unboxClass = unbox(type);
            if (unboxClass == void.class) {
                unbox = VoidType.THE_VOID;
            } else if (unboxClass != null && unboxClass.isPrimitive()) {
                unbox = PrimitiveType.of(unboxClass);
            } else {
                unbox = this;
            }
        }
        return unbox;
    }

    @Override
    public ClassType box() {
        return this;
    }

    @Override
    public boolean canNarrowFrom(int value) {
        return false;
    }

    @Override
    public PrimitiveType unaryWiden() {
        throw new UnsupportedOperationException("Cannot unary widen an object type");
    }

    @Override
    public PrimitiveType binaryWiden(Type with) {
        throw new UnsupportedOperationException("Cannot binary widen an object type");
    }

    @Override
    public boolean convertibleTo(Type to) {
        if (to instanceof ClassUnionType) {
            throw new UnsupportedOperationException("Cannot convert to an object union type");
        }
        return convertibleTo(type, to.getTypeClass());
    }

    @Override
    public Constructor<?> getConstructor(Type[] arguments) {
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
    public Constructor<?> getVarargConstructor(Type[] arguments) {
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

    private void failGetConstructor(Type[] arguments) {
        throw new UnsupportedOperationException("No constructor for signature: "
                + "(" + StringUtil.toString(Arrays.asList(arguments), ", ") + ") in " + getName());
    }

    @Override
    public Field getField(String name) {
        try {
            return type.getField(name);
        } catch (NoSuchFieldException exception) {
            throw new UnsupportedOperationException("No field named " + name + " in " + getName());
        }
    }

    @Override
    public Method getMethod(String name, Type[] arguments) {
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
        // generics can cause methods to only differ by the return type, so fix that
        ReflectionUtil.fixReturnTypeConflicts(candidates);
        // try to resolve the overloads
        final Method method = ReflectionUtil.resolveOverloads(candidates, arguments);
        if (method == null) {
            failGetMethod(name, arguments);
        }
        return method;
    }

    @Override
    public Method getVarargMethod(String name, Type[] arguments) {
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
        // generics can cause methods to only differ by the return type, so fix that
        ReflectionUtil.fixReturnTypeConflicts(candidates);
        // try to resolve the overloads
        final Method method = ReflectionUtil.resolveOverloads(candidates, arguments);
        if (method == null) {
            failGetMethod(name, arguments);
        }
        return method;
    }

    private void failGetMethod(String name, Type[] arguments) {
        throw new UnsupportedOperationException("No method for signature: "
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
            return from.isPrimitive() && PrimitiveType.convertibleTo(from, to);
        }
        return to.isAssignableFrom(from);
    }

    public static ClassType of(Class<?> type) {
        return new ClassType(type);
    }
}
