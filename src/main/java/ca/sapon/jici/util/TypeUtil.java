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
package ca.sapon.jici.util;

import java.io.Serializable;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.evaluator.type.LiteralReferenceType;
import ca.sapon.jici.evaluator.type.LiteralType;
import ca.sapon.jici.evaluator.type.ParametrizedType;
import ca.sapon.jici.evaluator.type.PrimitiveType;
import ca.sapon.jici.evaluator.type.ReferenceIntersectionType;
import ca.sapon.jici.evaluator.type.ReferenceType;
import ca.sapon.jici.evaluator.type.SingleReferenceType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.type.TypeArgument;
import ca.sapon.jici.evaluator.type.TypeVariable;
import ca.sapon.jici.evaluator.type.VoidType;
import ca.sapon.jici.evaluator.type.WildcardType;
import ca.sapon.jici.lexer.Identifier;
import ca.sapon.jici.parser.expression.Expression;

/**
 *
 */
public final class TypeUtil {
    private static final Map<Class<?>, LiteralReferenceType> BOXING_CONVERSIONS = new HashMap<>();
    private static final Map<Class<?>, PrimitiveType> UNBOXING_CONVERSIONS = new HashMap<>();

    static {
        BOXING_CONVERSIONS.put(boolean.class, LiteralReferenceType.of(Boolean.class));
        BOXING_CONVERSIONS.put(byte.class, LiteralReferenceType.of(Byte.class));
        BOXING_CONVERSIONS.put(short.class, LiteralReferenceType.of(Short.class));
        BOXING_CONVERSIONS.put(char.class, LiteralReferenceType.of(Character.class));
        BOXING_CONVERSIONS.put(int.class, LiteralReferenceType.of(Integer.class));
        BOXING_CONVERSIONS.put(long.class, LiteralReferenceType.of(Long.class));
        BOXING_CONVERSIONS.put(float.class, LiteralReferenceType.of(Float.class));
        BOXING_CONVERSIONS.put(double.class, LiteralReferenceType.of(Double.class));

        UNBOXING_CONVERSIONS.put(Boolean.class, PrimitiveType.THE_BOOLEAN);
        UNBOXING_CONVERSIONS.put(Byte.class, PrimitiveType.THE_BYTE);
        UNBOXING_CONVERSIONS.put(Short.class, PrimitiveType.THE_SHORT);
        UNBOXING_CONVERSIONS.put(Character.class, PrimitiveType.THE_CHAR);
        UNBOXING_CONVERSIONS.put(Integer.class, PrimitiveType.THE_INT);
        UNBOXING_CONVERSIONS.put(Long.class, PrimitiveType.THE_LONG);
        UNBOXING_CONVERSIONS.put(Float.class, PrimitiveType.THE_FLOAT);
        UNBOXING_CONVERSIONS.put(Double.class, PrimitiveType.THE_DOUBLE);
    }

    private TypeUtil() {
    }

    public static LiteralReferenceType box(Class<?> type) {
        return BOXING_CONVERSIONS.get(type);
    }

    public static PrimitiveType unbox(Class<?> type) {
        return UNBOXING_CONVERSIONS.get(type);
    }

    public static LiteralType[] wrap(Class<?>[] types) {
        final LiteralType[] wrapped = new LiteralType[types.length];
        for (int i = 0; i < types.length; i++) {
            wrapped[i] = wrap(types[i]);
        }
        return wrapped;
    }

    public static LiteralType wrap(Class<?> type) {
        if (type == null) {
            throw new NullPointerException("type");
        }
        if (type == void.class) {
            return VoidType.THE_VOID;
        }
        if (type.isPrimitive()) {
            return PrimitiveType.of(type);
        }
        return LiteralReferenceType.of(type);
    }

    public static Type wrap(java.lang.reflect.Type type) {
        if (type instanceof Class<?>) {
            return wrap((Class<?>) type);
        }
        if (type instanceof GenericArrayType) {
            final GenericArrayType genericArrayType = (GenericArrayType) type;
            java.lang.reflect.Type componentType = genericArrayType.getGenericComponentType();
            int dimensions = 1;
            while (componentType instanceof GenericArrayType) {
                componentType = ((GenericArrayType) componentType).getGenericComponentType();
                dimensions++;
            }
            final Type wrapped = wrap(componentType);
            if (wrapped instanceof SingleReferenceType) {
                return ((SingleReferenceType) wrapped).asArray(dimensions);
            }
        }
        if (type instanceof ParameterizedType) {
            final ParameterizedType paramType = (ParameterizedType) type;
            final java.lang.reflect.Type[] params = paramType.getActualTypeArguments();
            final List<TypeArgument> wrapped = new ArrayList<>(params.length);
            for (java.lang.reflect.Type param : params) {
                final Type wrap = wrap(param);
                if (!(wrap instanceof TypeArgument)) {
                    throw new UnsupportedOperationException("Invalid type for generic parameter: " + wrap.getName());
                }
                wrapped.add(((TypeArgument) wrap));
            }
            return ParametrizedType.of((Class<?>) paramType.getRawType(), wrapped);
        }
        if (type instanceof java.lang.reflect.TypeVariable) {
            final java.lang.reflect.TypeVariable<?> typeVariable = (java.lang.reflect.TypeVariable<?>) type;
            final Set<SingleReferenceType> wrappedUpper = wrapBounds(typeVariable.getBounds());
            return TypeVariable.of(typeVariable.getName(), wrappedUpper);
        }
        if (type instanceof java.lang.reflect.WildcardType) {
            final java.lang.reflect.WildcardType wildcardType = (java.lang.reflect.WildcardType) type;
            final Set<SingleReferenceType> wrappedLower = wrapBounds(wildcardType.getLowerBounds());
            final Set<SingleReferenceType> wrappedUpper = wrapBounds(wildcardType.getLowerBounds());
            return WildcardType.of(wrappedLower, wrappedUpper);
        }
        throw new UnsupportedOperationException(type.getClass().getSimpleName());
    }

    private static Set<SingleReferenceType> wrapBounds(java.lang.reflect.Type[] types) {
        final Set<SingleReferenceType> wrapped = new HashSet<>(types.length);
        for (java.lang.reflect.Type type : types) {
            final Type wrap = wrap(type);
            if (!(wrap instanceof SingleReferenceType)) {
                throw new UnsupportedOperationException("Invalid type for bound: " + wrap.getName());
            }
            wrapped.add((SingleReferenceType) wrap);
        }
        return wrapped;
    }

    // based on https://stackoverflow.com/questions/9797212/finding-the-nearest-common-superclass-or-superinterface-of-a-collection-of-cla
    public static Set<Class<?>> getLowestUpperBound(Iterable<Class<?>> classes) {
        final Set<Class<?>> common = getCommonSuperClasses(classes);
        final Set<Class<?>> lowest = new HashSet<>(common.size());
        while (!common.isEmpty()) {
            final Iterator<Class<?>> iterator = common.iterator();
            Class<?> _class = iterator.next();
            iterator.remove();
            while (iterator.hasNext()) {
                Class<?> candidate = iterator.next();
                if (candidate.isAssignableFrom(_class)) {
                    iterator.remove();
                } else if (_class.isAssignableFrom(candidate)) {
                    _class = candidate;
                    iterator.remove();
                }
            }
            lowest.add(_class);
        }
        return lowest;
    }

    public static Set<Class<?>> getCommonSuperClasses(Iterable<Class<?>> classes) {
        final Iterator<Class<?>> iterator = classes.iterator();
        if (!iterator.hasNext()) {
            return Collections.emptySet();
        }
        final Set<Class<?>> superClasses = getSuperClasses(iterator.next());
        while (iterator.hasNext()) {
            final Class<?> _class = iterator.next();
            final Iterator<Class<?>> candidates = superClasses.iterator();
            while (candidates.hasNext()) {
                final Class<?> superClass = candidates.next();
                if (!superClass.isAssignableFrom(_class)) {
                    candidates.remove();
                }
            }
        }
        return superClasses;
    }

    public static Set<Class<?>> getSuperClasses(Class<?> _class) {
        final Set<Class<?>> result = new HashSet<>();
        final Queue<Class<?>> queue = new ArrayDeque<>();
        queue.add(_class);
        if (_class.isInterface()) {
            queue.add(Object.class);
        }
        while (!queue.isEmpty()) {
            final Class<?> child = queue.remove();
            if (result.add(child)) {
                if (child.isArray()) {
                    addArraySuperClasses(child, queue);
                } else {
                    final Class<?> superClass = child.getSuperclass();
                    if (superClass != null) {
                        queue.add(superClass);
                    }
                    queue.addAll(Arrays.asList(child.getInterfaces()));
                }
            }
        }
        return result;
    }

    private static void addArraySuperClasses(Class<?> arrayType, Collection<Class<?>> to) {
        int dimensions = 0;
        Class<?> componentType = arrayType;
        do {
            componentType = componentType.getComponentType();
            to.add(ReflectionUtil.asArrayType(Object.class, dimensions));
            to.add(ReflectionUtil.asArrayType(Cloneable.class, dimensions));
            to.add(ReflectionUtil.asArrayType(Serializable.class, dimensions));
            dimensions++;
        } while (componentType.isArray());
        if (!componentType.isPrimitive()) {
            final Class<?> superClass = componentType.getSuperclass();
            if (superClass != null) {
                to.add(ReflectionUtil.asArrayType(superClass, dimensions));
            }
            for (Class<?> _interface : componentType.getInterfaces()) {
                to.add(ReflectionUtil.asArrayType(_interface, dimensions));
            }
        }
    }

    public static PrimitiveType coerceToPrimitive(Environment environment, Expression expression) {
        return coerceToPrimitive(expression, expression.getType(environment));
    }

    public static PrimitiveType coerceToPrimitive(Expression expression, Type type) {
        final PrimitiveType primitiveType;
        if (type instanceof PrimitiveType) {
            primitiveType = (PrimitiveType) type;
        } else if (type instanceof SingleReferenceType && ((SingleReferenceType) type).isBox()) {
            primitiveType = ((SingleReferenceType) type).unbox();
        } else {
            throw new EvaluatorException("Not a primitive type: " + type.getName(), expression);
        }
        return primitiveType;
    }

    public static Class<?> findNameMatch(ReferenceType type, List<Identifier> name) {
        if (type instanceof SingleReferenceType) {
            final SingleReferenceType singleClass = (SingleReferenceType) type;
            final Class<?> typeClass = singleClass.getTypeClass();
            Class<?> currentClass = typeClass;
            for (int i = name.size() - 1; i >= 0; i--) {
                if (currentClass != null && name.get(i).getSource().equals(currentClass.getSimpleName())) {
                    // partial name match, continue
                    currentClass = currentClass.getEnclosingClass();
                } else {
                    // name match fail, check super class
                    final Class<?> superClass = typeClass.getSuperclass();
                    if (superClass != null) {
                        final Class<?> match = findNameMatch(LiteralReferenceType.of(superClass), name);
                        if (match != null) {
                            return match;
                        }
                    }
                    // now check implemented interfaces
                    for (Class<?> implemented : typeClass.getInterfaces()) {
                        final Class<?> match = findNameMatch(LiteralReferenceType.of(implemented), name);
                        if (match != null) {
                            return match;
                        }
                    }
                    return null;
                }
            }
            return typeClass;
        }
        if (type instanceof ReferenceIntersectionType) {
            for (ReferenceType referenceType : ((ReferenceIntersectionType) type).getLowestUpperBound()) {
                final Class<?> match = findNameMatch(referenceType, name);
                if (match != null) {
                    return match;
                }
            }
            return null;
        }
        return null;
    }
}
