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

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import ca.sapon.jici.evaluator.Accessible;
import ca.sapon.jici.evaluator.Callable;
import ca.sapon.jici.util.ReflectionUtil;
import ca.sapon.jici.util.StringUtil;
import ca.sapon.jici.util.TypeUtil;

/**
 * A reference type literally used in the code, such as {@code String} or {@code int[]}.
 */
public class LiteralReferenceType extends SingleReferenceType implements LiteralType, TypeArgument {
    public static final LiteralReferenceType THE_STRING = LiteralReferenceType.of(String.class);
    public static final LiteralReferenceType THE_OBJECT = LiteralReferenceType.of(Object.class);
    public static final LiteralReferenceType THE_CLONEABLE = LiteralReferenceType.of(Cloneable.class);
    public static final LiteralReferenceType THE_SERIALIZABLE = LiteralReferenceType.of(Serializable.class);
    private static final Map<Class<?>, PrimitiveType> UNBOXING_CONVERSIONS = new HashMap<>();
    private final Class<?> type;
    private PrimitiveType unbox;
    private boolean unboxCached = false;

    static {
        UNBOXING_CONVERSIONS.put(Boolean.class, PrimitiveType.THE_BOOLEAN);
        UNBOXING_CONVERSIONS.put(Byte.class, PrimitiveType.THE_BYTE);
        UNBOXING_CONVERSIONS.put(Short.class, PrimitiveType.THE_SHORT);
        UNBOXING_CONVERSIONS.put(Character.class, PrimitiveType.THE_CHAR);
        UNBOXING_CONVERSIONS.put(Integer.class, PrimitiveType.THE_INT);
        UNBOXING_CONVERSIONS.put(Long.class, PrimitiveType.THE_LONG);
        UNBOXING_CONVERSIONS.put(Float.class, PrimitiveType.THE_FLOAT);
        UNBOXING_CONVERSIONS.put(Double.class, PrimitiveType.THE_DOUBLE);
    }

    protected LiteralReferenceType(Class<?> type) {
        this.type = type;
    }

    @Override
    public String getName() {
        return type.getCanonicalName();
    }

    @Override
    public boolean isArray() {
        return type.isArray();
    }

    @Override
    public boolean isReifiable() {
        return true;
    }

    public boolean isRaw() {
        return type.getTypeParameters().length > 0;
    }

    @Override
    public Class<?> getTypeClass() {
        return type;
    }

    public boolean isBox() {
        if (!unboxCached) {
            unbox = UNBOXING_CONVERSIONS.get(type);
            unboxCached = true;
        }
        return unbox != null;
    }

    public PrimitiveType unbox() {
        if (isBox()) {
            return unbox;
        }
        throw new UnsupportedOperationException(type.getCanonicalName() + " is not a box type");
    }

    public LiteralType tryUnbox() {
        if (isBox()) {
            return unbox;
        }
        return this;
    }

    public LiteralReferenceType getErasure() {
        return this;
    }

    @Override
    public LiteralType getComponentType() {
        final Class<?> componentType = type.getComponentType();
        if (componentType == null) {
            throw new UnsupportedOperationException("Not an array type");
        }
        return TypeUtil.wrap(componentType);
    }

    @Override
    public LiteralReferenceType asArray(int dimensions) {
        return of(ReflectionUtil.asArrayType(type, dimensions));
    }

    @Override
    public boolean contains(TypeArgument other) {
        return equals(other);
    }

    @Override
    public boolean convertibleTo(Type to) {
        // Literal class types might be convertible to a primitive if they can be unboxed
        // If the target literal type is parametrized, this type must have a parent with
        // the same erasure and compatible type arguments
        // Else they can be cast to another literal class if they are a subtype
        // They can also be converted to an intersection if they can be converted to each member
        if (to.isPrimitive()) {
            return isBox() && unbox().convertibleTo(to);
        }
        if (to instanceof ParametrizedType) {
            final ParametrizedType parametrized = (ParametrizedType) to;
            final LiteralReferenceType erasure = parametrized.getErasure();
            for (LiteralReferenceType superType : TypeUtil.getSuperTypes(this)) {
                if (superType.getErasure().equals(erasure)) {
                    return !(superType instanceof ParametrizedType)
                            || parametrized.argumentsContain(((ParametrizedType) superType).getArguments());
                }
            }
            return false;
        }
        if (to instanceof LiteralReferenceType) {
            final LiteralReferenceType target = (LiteralReferenceType) to;
            return target.type.isAssignableFrom(type);
        }
        if (to instanceof ReferenceIntersectionType) {
            final ReferenceIntersectionType target = (ReferenceIntersectionType) to;
            for (SingleReferenceType type : target.getTypes()) {
                if (!convertibleTo(type)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public LiteralReferenceType getSuperType() {
        final java.lang.reflect.Type superClass = type.getGenericSuperclass();
        return superClass != null ? (LiteralReferenceType) TypeUtil.wrap(superClass) : null;
    }

    @Override
    public LiteralReferenceType[] getInterfaces() {
        final java.lang.reflect.Type[] interfaces = type.getGenericInterfaces();
        final LiteralReferenceType[] wrapped = new LiteralReferenceType[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            wrapped[i] = (LiteralReferenceType) TypeUtil.wrap(interfaces[i]);
        }
        return wrapped;
    }

    @Override
    public Callable getConstructor(Type[] arguments) {
        final Constructor<?>[] constructors = type.getConstructors();
        final int argumentCount = arguments.length;
        final Map<Constructor<?>, Type[]> candidates = new HashMap<>();
        final Map<Constructor<?>, Type[]> varargCandidate = new HashMap<>();
        for (Constructor<?> candidate : constructors) {
            if (!candidate.isSynthetic()) {
                final Class<?>[] parameterTypes = candidate.getParameterTypes();
                // look for matches in length
                if (parameterTypes.length == argumentCount) {
                    candidates.put(candidate, TypeUtil.wrap(parameterTypes));
                }
                // look for varargs with matches in name and length of non-varargs
                if (candidate.isVarArgs() && parameterTypes.length - 1 <= argumentCount) {
                    // expand the parameters through the vararg to match the argument count
                    varargCandidate.put(candidate, TypeUtil.wrap(ReflectionUtil.expandsVarargs(parameterTypes, argumentCount)));
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
            field = type.getField(name);
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
        final Method[] methods = type.getMethods();
        final int argumentCount = arguments.length;
        final Map<Method, Type[]> candidates = new HashMap<>();
        final Map<Method, Type[]> varargCandidate = new HashMap<>();
        for (Method candidate : methods) {
            if (!candidate.isSynthetic() && candidate.getName().equals(name)) {
                final Class<?>[] parameterTypes = candidate.getParameterTypes();
                // look for matches in length and name
                if (parameterTypes.length == argumentCount) {
                    candidates.put(candidate, TypeUtil.wrap(parameterTypes));
                }
                // look for varargs with matches in name and length of non-varargs
                if (candidate.isVarArgs() && parameterTypes.length - 1 <= argumentCount) {
                    // expand the parameters through the vararg to match the argument count
                    varargCandidate.put(candidate, TypeUtil.wrap(ReflectionUtil.expandsVarargs(parameterTypes, argumentCount)));
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
    public boolean equals(Object other) {
        return this == other || (other instanceof LiteralReferenceType) && this.type == ((LiteralReferenceType) other).type;
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    public static LiteralReferenceType of(Class<?> type) {
        return new LiteralReferenceType(type);
    }
}