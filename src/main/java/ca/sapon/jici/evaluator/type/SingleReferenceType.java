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

import ca.sapon.jici.evaluator.Accessible;
import ca.sapon.jici.evaluator.Callable;
import ca.sapon.jici.evaluator.value.ValueKind;
import ca.sapon.jici.util.ReflectionUtil;
import ca.sapon.jici.util.StringUtil;

/**
 * A reference type comprised of a single backing type. That is, a non-divisible type.
 */
public abstract class SingleReferenceType implements ReferenceType, LiteralType, TypeArgument {
    public static final SingleReferenceType THE_STRING = LiteralReferenceType.of(String.class);
    public static final SingleReferenceType THE_OBJECT = LiteralReferenceType.of(Object.class);
    private static final Map<Class<?>, Class<?>> UNBOXED_TYPES = new HashMap<>();
    private PrimitiveType unbox;
    private boolean unboxCached = false;

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
        return getTypeClass().isArray();
    }

    @Override
    public boolean isObject() {
        return true;
    }

    public abstract LiteralType getComponentType();

    @Override
    public boolean contains(TypeArgument other) {
        return equals(other);
    }

    public boolean isBox() {
        if (!unboxCached) {
            final Class<?> unboxClass = unbox(getTypeClass());
            if (unboxClass != null && unboxClass.isPrimitive()) {
                unbox = PrimitiveType.of(unboxClass);
            } else {
                unbox = null;
            }
            unboxCached = true;
        }
        return unbox != null;
    }

    public PrimitiveType unbox() {
        if (isBox()) {
            return unbox;
        }
        throw new UnsupportedOperationException(getTypeClass().getCanonicalName() + " is not a box type");
    }

    public LiteralType tryUnbox() {
        if (isBox()) {
            return unbox;
        }
        return this;
    }

    @Override
    public Callable getConstructor(Type[] arguments) {
        final Constructor<?>[] constructors = getTypeClass().getConstructors();
        final int argumentCount = arguments.length;
        final Map<Constructor<?>, Class<?>[]> candidates = new HashMap<>();
        final Map<Constructor<?>, Class<?>[]> varargCandidate = new HashMap<>();
        for (Constructor<?> candidate : constructors) {
            if (!candidate.isSynthetic()) {
                final Class<?>[] parameterTypes = candidate.getParameterTypes();
                // look for matches in length
                if (parameterTypes.length == argumentCount) {
                    candidates.put(candidate, parameterTypes);
                }
                // look for varargs with matches in name and length of non-varargs
                if (candidate.isVarArgs() && parameterTypes.length - 1 <= argumentCount) {
                    // expand the parameters through the vararg to match the argument count
                    varargCandidate.put(candidate, ReflectionUtil.expandsVarargs(parameterTypes, argumentCount));
                }
            }
        }
        // try to resolve the overloads
        Constructor<?> constructor = ReflectionUtil.resolveOverloads(candidates, arguments);
        if (constructor != null) {
            return Callable.forConstructor(constructor);
        }
        // try vararg candidates
        constructor = ReflectionUtil.resolveOverloads(varargCandidate, arguments);
        if (constructor != null) {
            return Callable.forVarargConstructor(constructor);
        }
        throw new UnsupportedOperationException("No constructor for signature: "
                + "(" + StringUtil.toString(Arrays.asList(arguments), ", ") + ") in " + getName());
    }

    @Override
    public Accessible getField(String name) {
        if (isArray() && "length".equals(name)) {
            return Accessible.forArrayLength();
        }
        Field field;
        try {
            field = getTypeClass().getField(name);
            if (field.isSynthetic()) {
                return failGetField(name);
            }
        } catch (NoSuchFieldException exception) {
            return failGetField(name);
        }
        return Accessible.forField(field);
    }

    private Accessible failGetField(String name) {
        throw new UnsupportedOperationException("No field named " + name + " in " + getName());
    }

    @Override
    public Callable getMethod(String name, Type[] arguments) {
        if (isArray() && arguments.length == 0 && "clone".equals(name)) {
            return Callable.forArrayClone(this);
        }
        final Method[] methods = getTypeClass().getMethods();
        final int argumentCount = arguments.length;
        final Map<Method, Class<?>[]> candidates = new HashMap<>();
        final Map<Method, Class<?>[]> varargCandidate = new HashMap<>();
        for (Method candidate : methods) {
            if (!candidate.isSynthetic() && candidate.getName().equals(name)) {
                final Class<?>[] parameterTypes = candidate.getParameterTypes();
                // look for matches in length and name
                if (parameterTypes.length == argumentCount) {
                    candidates.put(candidate, parameterTypes);
                }
                // look for varargs with matches in name and length of non-varargs
                if (candidate.isVarArgs() && parameterTypes.length - 1 <= argumentCount) {
                    // expand the parameters through the vararg to match the argument count
                    varargCandidate.put(candidate, ReflectionUtil.expandsVarargs(parameterTypes, argumentCount));
                }
            }
        }
        // generics can cause methods to only differ by the return type, so fix that
        ReflectionUtil.fixReturnTypeConflicts(candidates);
        // try to resolve the overloads
        Method method = ReflectionUtil.resolveOverloads(candidates, arguments);
        if (method != null) {
            return Callable.forMethod(method);
        }
        // try vararg candidates
        ReflectionUtil.fixReturnTypeConflicts(varargCandidate);
        method = ReflectionUtil.resolveOverloads(varargCandidate, arguments);
        if (method != null) {
            return Callable.forVarargMethod(method);
        }
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
}
