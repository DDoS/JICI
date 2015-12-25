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
import ca.sapon.jici.evaluator.type.NullType;
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

    public static Type[] wrap(java.lang.reflect.Type[] types) {
        final Type[] wrapped = new Type[types.length];
        for (int i = 0; i < types.length; i++) {
            wrapped[i] = wrap(types[i]);
        }
        return wrapped;
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

    public static <T extends Type> Set<T> greatestLowerBound(Collection<T> types) {
        if (types.isEmpty()) {
            // Fast track trivial cases
            return Collections.emptySet();
        }
        if (types.size() == 1) {
            // Fast track trivial cases
            return Collections.singleton(types.iterator().next());
        }
        // Discard any member that is a super type of another
        final Set<T> minimalTypes = new HashSet<>();
        potentialCandidates:
        for (T potentialCandidate : types) {
            for (Iterator<T> iterator = minimalTypes.iterator(); iterator.hasNext(); ) {
                final T existingCandidate = iterator.next();
                if (existingCandidate.convertibleTo(potentialCandidate)) {
                    // potential candidate is a super type of existing one, don't add
                    continue potentialCandidates;
                } else if (potentialCandidate.convertibleTo(existingCandidate)) {
                    // existing candidate is a super type of potential one, remove it
                    iterator.remove();
                }
            }
            minimalTypes.add(potentialCandidate);
        }
        return minimalTypes;
    }

    public static ReferenceIntersectionType lowestUpperBound(ReferenceType... types) {
        return lowestUpperBound(Arrays.asList(types));
    }

    private static ReferenceIntersectionType lowestUpperBound(Set<Collection<? extends ReferenceType>> recursions, ReferenceType... types) {
        return lowestUpperBound(Arrays.asList(types), recursions);
    }

    public static ReferenceIntersectionType lowestUpperBound(Collection<? extends ReferenceType> types) {
        return lowestUpperBound(types, new HashSet<Collection<? extends ReferenceType>>());
    }

    private static ReferenceIntersectionType lowestUpperBound(Collection<? extends ReferenceType> types, Set<Collection<? extends ReferenceType>> recursions) {
        if (types.size() == 1) {
            // Fast track trivial cases
            return ReferenceIntersectionType.of(types.iterator().next());
        }
        if (types.isEmpty() || !recursions.add(types)) {
            // Recursive lowest upper bound calls can lead to infinite types, don't compute a bound for these (like javac does)
            return ReferenceIntersectionType.NOTHING;
        }
        // Get the super type sets ST(U)
        final Map<ReferenceType, Set<SingleReferenceType>> superTypeSets = new HashMap<>();
        final Collection<Set<SingleReferenceType>> superTypes = superTypeSets.values();
        for (ReferenceType type : types) {
            if (type instanceof NullType) {
                // The null type has a universal set of super types, we can skip it
                continue;
            }
            superTypeSets.put(type, getSuperTypes(type));
        }
        // Intersect the erased super type sets EST(U) to generate the erased candidate set EC
        final Set<LiteralReferenceType> erasedCandidates = new HashSet<>();
        final Iterator<Set<SingleReferenceType>> superTypesIterator = superTypes.iterator();
        erasedCandidates.addAll(getErasedTypes(superTypesIterator.next()));
        while (superTypesIterator.hasNext()) {
            erasedCandidates.retainAll(getErasedTypes(superTypesIterator.next()));
        }
        // Get the minimal erased candidate set MEC
        final Set<LiteralReferenceType> minimalErasedCandidates = greatestLowerBound(erasedCandidates);
        // For generic candidates (those with relevant invocations) compute the least containing invocation
        final Set<SingleReferenceType> lowestUpperBound = new HashSet<>();
        for (LiteralReferenceType candidate : minimalErasedCandidates) {
            final Set<ParametrizedType> invocations = relevantInvocations(superTypes, candidate);
            if (invocations.isEmpty()) {
                lowestUpperBound.add(candidate);
            } else {
                lowestUpperBound.add(leastContainingInvocation(invocations, recursions));
            }
        }
        return ReferenceIntersectionType.of(lowestUpperBound);
    }

    private static Set<ParametrizedType> relevantInvocations(Collection<Set<SingleReferenceType>> superTypeSets, LiteralReferenceType type) {
        // Search for an invocation of the erased type in any of the super type sets
        final Set<ParametrizedType> invocations = new HashSet<>();
        for (Set<SingleReferenceType> superTypes : superTypeSets) {
            for (SingleReferenceType superType : superTypes) {
                if (superType instanceof ParametrizedType) {
                    final ParametrizedType parametrizedType = (ParametrizedType) superType;
                    if (parametrizedType.getRaw().equals(type)) {
                        invocations.add(parametrizedType);
                    }
                }
            }
        }
        return invocations;
    }

    private static ParametrizedType leastContainingInvocation(Set<ParametrizedType> invocations, Set<Collection<? extends ReferenceType>> recursions) {
        final Iterator<ParametrizedType> iterator = invocations.iterator();
        ParametrizedType left = iterator.next();
        if (invocations.size() == 1) {
            // For a single invocation we get the least containing type argument of each invocation argument
            final List<TypeArgument> arguments = left.getArguments();
            final List<TypeArgument> leastArguments = new ArrayList<>(arguments.size());
            for (TypeArgument argument : arguments) {
                leastArguments.add(leastContainingTypeArgument(argument, recursions));
            }
            return ParametrizedType.of(left.getRaw().getTypeClass(), leastArguments);
        }
        // For many invocation we reduce pairwise, left with right
        while (iterator.hasNext()) {
            // The least containing type argument is done pairwise on the arguments of the left and right type
            final ParametrizedType right = iterator.next();
            final List<TypeArgument> leftArguments = left.getArguments();
            final List<TypeArgument> rightArguments = right.getArguments();
            final List<TypeArgument> leastArguments = new ArrayList<>(leftArguments.size());
            for (int i = 0; i < leftArguments.size(); i++) {
                leastArguments.add(leastContainingTypeArgument(leftArguments.get(i), rightArguments.get(i), recursions));
            }
            left = ParametrizedType.of(left.getRaw().getTypeClass(), leastArguments);
        }
        return left;
    }

    private static TypeArgument leastContainingTypeArgument(TypeArgument left, TypeArgument right, Set<Collection<? extends ReferenceType>> recursions) {
        // In the case where one argument isn't a wildcard type, ensure the non-wildcard is on the left
        if (!(right instanceof WildcardType)) {
            final TypeArgument swap = left;
            left = right;
            right = swap;
        }
        if (left instanceof WildcardType) {
            // Pair of wildcards
            return leastContainingTypeArgument((WildcardType) left, (WildcardType) right, recursions);
        }
        if (right instanceof WildcardType) {
            // Only one wildcard
            return leastContainingTypeArgument(left, (WildcardType) right, recursions);
        }
        // No wildcards
        return left.equals(right) ? left : WildcardType.of(
                ReferenceIntersectionType.NOTHING,
                lowestUpperBound(recursions, (SingleReferenceType) left, (SingleReferenceType) right)
        );
    }

    private static TypeArgument leastContainingTypeArgument(WildcardType left, WildcardType right, Set<Collection<? extends ReferenceType>> recursions) {
        final Set<SingleReferenceType> combinedUpperBound = new HashSet<>(left.getUpperBound().getTypes());
        final Set<SingleReferenceType> combinedLowerBound = new HashSet<>(left.getLowerBound().getTypes());
        combinedUpperBound.addAll(right.getUpperBound().getTypes());
        combinedLowerBound.addAll(right.getLowerBound().getTypes());
        return combinedUpperBound.equals(combinedLowerBound) ? ReferenceIntersectionType.of(combinedUpperBound)
                : WildcardType.of(ReferenceIntersectionType.of(combinedLowerBound), lowestUpperBound(combinedUpperBound, recursions));
    }

    private static TypeArgument leastContainingTypeArgument(TypeArgument left, WildcardType right, Set<Collection<? extends ReferenceType>> recursions) {
        final Set<SingleReferenceType> combinedUpperBound = new HashSet<>(right.getUpperBound().getTypes());
        final Set<SingleReferenceType> combinedLowerBound = new HashSet<>(right.getLowerBound().getTypes());
        combinedUpperBound.add((SingleReferenceType) left);
        combinedLowerBound.add((SingleReferenceType) left);
        return WildcardType.of(ReferenceIntersectionType.of(combinedLowerBound), lowestUpperBound(combinedUpperBound, recursions));
    }

    private static TypeArgument leastContainingTypeArgument(TypeArgument argument, Set<Collection<? extends ReferenceType>> recursions) {
        final Set<SingleReferenceType> combinedUpperBound = new HashSet<>();
        final Set<SingleReferenceType> combinedLowerBound = new HashSet<>();
        // Left type is argument
        if (argument instanceof WildcardType) {
            final WildcardType wildcard = (WildcardType) argument;
            combinedUpperBound.addAll(wildcard.getUpperBound().getTypes());
            combinedLowerBound.addAll(wildcard.getLowerBound().getTypes());
        } else {
            combinedUpperBound.add((SingleReferenceType) argument);
            combinedLowerBound.add((SingleReferenceType) argument);
        }
        // Right type is ?
        combinedUpperBound.add(SingleReferenceType.THE_OBJECT);
        combinedLowerBound.add(NullType.THE_NULL);
        return WildcardType.of(ReferenceIntersectionType.of(combinedLowerBound), lowestUpperBound(combinedUpperBound, recursions));
    }

    private static Set<LiteralReferenceType> getErasedTypes(Set<SingleReferenceType> types) {
        final Set<LiteralReferenceType> erased = new HashSet<>();
        for (SingleReferenceType type : types) {
            if (type instanceof ParametrizedType) {
                erased.add(((ParametrizedType) type).getRaw());
            } else {
                erased.add(((LiteralReferenceType) type));
            }
        }
        return erased;
    }

    private static Set<SingleReferenceType> getSuperTypes(ReferenceType type) {
        final Set<SingleReferenceType> result = new HashSet<>();
        final Queue<SingleReferenceType> queue = new ArrayDeque<>();
        if (type instanceof ReferenceIntersectionType) {
            queue.addAll(((ReferenceIntersectionType) type).getTypes());
        } else {
            queue.add((SingleReferenceType) type);
        }
        while (!queue.isEmpty()) {
            final SingleReferenceType child = queue.remove();
            if (result.add(child)) {
                if (child.isArray()) {
                    addArraySuperClasses(child, queue);
                } else {
                    final SingleReferenceType superClass = child.getSuperType();
                    if (superClass != null) {
                        queue.add(superClass);
                    }
                    Collections.addAll(queue, child.getInterfaces());
                }
            }
        }
        result.add(SingleReferenceType.THE_OBJECT);
        return result;
    }

    private static void addArraySuperClasses(SingleReferenceType arrayType, Queue<SingleReferenceType> to) {
        int dimensions = 0;
        Type componentType = arrayType;
        do {
            componentType = ((SingleReferenceType) componentType).getComponentType();
            to.add(SingleReferenceType.THE_OBJECT.asArray(dimensions));
            to.add(SingleReferenceType.THE_CLONEABLE.asArray(dimensions));
            to.add(SingleReferenceType.THE_SERIALIZABLE.asArray(dimensions));
            dimensions++;
        } while (componentType.isArray());
        if (!componentType.isPrimitive()) {
            final SingleReferenceType referenceType = (SingleReferenceType) componentType;
            final SingleReferenceType superClass = referenceType.getSuperType();
            if (superClass != null) {
                to.add(superClass.asArray(dimensions));
            }
            for (SingleReferenceType _interface : referenceType.getInterfaces()) {
                to.add(_interface.asArray(dimensions));
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
            for (ReferenceType referenceType : ((ReferenceIntersectionType) type).getTypes()) {
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
