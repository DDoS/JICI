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
import java.lang.reflect.Modifier;
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
import ca.sapon.jici.evaluator.type.ComponentType;
import ca.sapon.jici.evaluator.type.IntersectionType;
import ca.sapon.jici.evaluator.type.LiteralReferenceType;
import ca.sapon.jici.evaluator.type.NullType;
import ca.sapon.jici.evaluator.type.ParametrizedType;
import ca.sapon.jici.evaluator.type.PrimitiveType;
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
    private TypeUtil() {
    }

    public static Type[] wrap(Class<?>[] types) {
        final Type[] wrapped = new Type[types.length];
        for (int i = 0; i < types.length; i++) {
            wrapped[i] = wrap(types[i]);
        }
        return wrapped;
    }

    public static Type wrap(Class<?> type) {
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
        if (type == null) {
            throw new NullPointerException("type");
        }
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
            if (wrapped instanceof ComponentType) {
                return ((ComponentType) wrapped).asArray(dimensions);
            }
            throw new UnsupportedOperationException("Invalid component type: " + wrapped.getName());
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
            final List<SingleReferenceType> wrappedUpper = wrapBounds(typeVariable.getBounds(), new ArrayList<SingleReferenceType>());
            return TypeVariable.of(typeVariable.getName(), wrappedUpper);
        }
        if (type instanceof java.lang.reflect.WildcardType) {
            final java.lang.reflect.WildcardType wildcardType = (java.lang.reflect.WildcardType) type;
            final Set<SingleReferenceType> wrappedLower = wrapBounds(wildcardType.getLowerBounds(), new HashSet<SingleReferenceType>());
            final Set<SingleReferenceType> wrappedUpper = wrapBounds(wildcardType.getLowerBounds(), new HashSet<SingleReferenceType>());
            return WildcardType.of(wrappedLower, wrappedUpper);
        }
        throw new UnsupportedOperationException(type.getClass().getCanonicalName());
    }

    private static <C extends Collection<SingleReferenceType>> C wrapBounds(java.lang.reflect.Type[] types, C to) {
        for (java.lang.reflect.Type type : types) {
            final Type wrap = wrap(type);
            if (!(wrap instanceof SingleReferenceType)) {
                throw new UnsupportedOperationException("Invalid type for bound: " + wrap.getName());
            }
            to.add((SingleReferenceType) wrap);
        }
        return to;
    }

    public static <T extends Type> Set<T> removeSubTypes(Collection<T> types) {
        return filterTypes(types, SubTypeFilter.INSTANCE, SuperTypeFilter.INSTANCE);
    }

    public static <T extends Type> Set<T> removeSuperTypes(Collection<T> types) {
        return filterTypes(types, SuperTypeFilter.INSTANCE, SubTypeFilter.INSTANCE);
    }

    private static <T extends Type> Set<T> filterTypes(Collection<T> types, Filter<Type, Type> potentialFilter, Filter<Type, Type> existingFilter) {
        if (types.isEmpty()) {
            // Fast track trivial cases
            return Collections.emptySet();
        }
        if (types.size() == 1) {
            // Fast track trivial cases
            return Collections.singleton(types.iterator().next());
        }
        // Discard any member that is a super type of another
        final Set<T> filteredTypes = new HashSet<>();
        potentialCandidates:
        for (T potentialCandidate : types) {
            for (Iterator<T> iterator = filteredTypes.iterator(); iterator.hasNext(); ) {
                final T existingCandidate = iterator.next();
                if (potentialFilter.test(potentialCandidate, existingCandidate)) {
                    // potential candidate is filtered for existing one, don't add
                    continue potentialCandidates;
                } else if (existingFilter.test(potentialCandidate, existingCandidate)) {
                    // existing candidate is filtered for potential one, remove it
                    iterator.remove();
                }
            }
            filteredTypes.add(potentialCandidate);
        }
        return filteredTypes;
    }

    public static HashSet<SingleReferenceType> expandIntersectionTypes(Collection<? extends ReferenceType> intersection) {
        final HashSet<SingleReferenceType> expandedTypes = new HashSet<>(intersection.size());
        for (ReferenceType type : intersection) {
            if (type instanceof IntersectionType) {
                expandedTypes.addAll(((IntersectionType) type).getTypes());
            } else {
                expandedTypes.add((SingleReferenceType) type);
            }
        }
        return expandedTypes;
    }

    public static IntersectionType lowestUpperBound(ReferenceType... types) {
        return lowestUpperBound(Arrays.asList(types));
    }

    public static IntersectionType lowestUpperBound(Collection<? extends ReferenceType> types) {
        return lowestUpperBound(expandIntersectionTypes(types));
    }

    public static IntersectionType lowestUpperBound(Set<SingleReferenceType> types) {
        return lowestUpperBound(types, new HashMap<Set<SingleReferenceType>, Integer>());
    }

    private static IntersectionType lowestUpperBound(Set<SingleReferenceType> types, Map<Set<SingleReferenceType>, Integer> recursions) {
        // Simplify the argument set to standardize it and make things faster
        types = TypeUtil.removeSubTypes(types);
        if (types.isEmpty()) {
            // Fast track trivial cases
            return IntersectionType.NOTHING;
        }
        // Get the number of recursive calls so far with the same arguments
        Integer count = recursions.get(types);
        if (count == null) {
            count = 0;
        }
        if (++count > 2) {
            // Recursive lowest upper bound calls can lead to infinite types, don't compute a bound after two recursions (like javac does)
            return IntersectionType.NOTHING;
        }
        recursions.put(types, count);
        if (types.size() == 1) {
            // Fast track trivial cases
            return IntersectionType.of(types.iterator().next());
        }
        // Get the super type sets ST(U)
        final Map<ReferenceType, Set<LiteralReferenceType>> superTypeSets = new HashMap<>();
        final Collection<Set<LiteralReferenceType>> superTypes = superTypeSets.values();
        for (ReferenceType type : types) {
            if (type.convertibleTo(NullType.THE_NULL)) {
                // The null type has a universal set of super types, we can skip it
                continue;
            }
            superTypeSets.put(type, getSuperTypes(type));
        }
        // Intersect the erased super type sets EST(U) to generate the erased candidate set EC
        final Set<LiteralReferenceType> erasedCandidates = new HashSet<>();
        final Iterator<Set<LiteralReferenceType>> superTypesIterator = superTypes.iterator();
        erasedCandidates.addAll(getErasedTypes(superTypesIterator.next()));
        while (superTypesIterator.hasNext()) {
            erasedCandidates.retainAll(getErasedTypes(superTypesIterator.next()));
        }
        // Get the minimal erased candidate set MEC
        final Set<LiteralReferenceType> minimalErasedCandidates = removeSuperTypes(erasedCandidates);
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
        return IntersectionType.of(lowestUpperBound);
    }

    private static ParametrizedType leastContainingInvocation(Set<ParametrizedType> invocations, Map<Set<SingleReferenceType>, Integer> recursions) {
        final Iterator<ParametrizedType> iterator = invocations.iterator();
        ParametrizedType left = iterator.next();
        if (invocations.size() == 1) {
            // For a single invocation we get the least containing type argument of each invocation argument
            final List<TypeArgument> arguments = left.getArguments();
            final List<TypeArgument> leastArguments = new ArrayList<>(arguments.size());
            for (TypeArgument argument : arguments) {
                leastArguments.add(leastContainingTypeArgument(argument, recursions));
            }
            return ParametrizedType.of(left.getErasure(), leastArguments);
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
            left = ParametrizedType.of(left.getErasure(), leastArguments);
        }
        return left;
    }

    private static TypeArgument leastContainingTypeArgument(TypeArgument left, TypeArgument right, Map<Set<SingleReferenceType>, Integer> recursions) {
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
        final Set<SingleReferenceType> combinedUpperBound = new HashSet<>();
        combinedUpperBound.add((SingleReferenceType) left);
        combinedUpperBound.add((SingleReferenceType) right);
        return left.equals(right) ? left : WildcardType.of(IntersectionType.EVERYTHING, lowestUpperBound(combinedUpperBound, recursions));
    }

    private static TypeArgument leastContainingTypeArgument(WildcardType left, WildcardType right, Map<Set<SingleReferenceType>, Integer> recursions) {
        final Set<SingleReferenceType> combinedUpperBound = new HashSet<>(left.getUpperBound().getTypes());
        final Set<SingleReferenceType> combinedLowerBound = new HashSet<>(left.getLowerBound().getTypes());
        combinedUpperBound.addAll(right.getUpperBound().getTypes());
        combinedLowerBound.addAll(right.getLowerBound().getTypes());
        return combinedUpperBound.equals(combinedLowerBound) ? IntersectionType.of(combinedUpperBound)
                : WildcardType.of(IntersectionType.of(combinedLowerBound), lowestUpperBound(combinedUpperBound, recursions));
    }

    private static TypeArgument leastContainingTypeArgument(TypeArgument left, WildcardType right, Map<Set<SingleReferenceType>, Integer> recursions) {
        final Set<SingleReferenceType> combinedUpperBound = new HashSet<>(right.getUpperBound().getTypes());
        final Set<SingleReferenceType> combinedLowerBound = new HashSet<>(right.getLowerBound().getTypes());
        combinedUpperBound.add((SingleReferenceType) left);
        combinedLowerBound.add((SingleReferenceType) left);
        return WildcardType.of(IntersectionType.of(combinedLowerBound), lowestUpperBound(combinedUpperBound, recursions));
    }

    private static TypeArgument leastContainingTypeArgument(TypeArgument argument, Map<Set<SingleReferenceType>, Integer> recursions) {
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
        combinedUpperBound.add(LiteralReferenceType.THE_OBJECT);
        combinedLowerBound.add(NullType.THE_NULL);
        return WildcardType.of(IntersectionType.of(combinedLowerBound), lowestUpperBound(combinedUpperBound, recursions));
    }

    private static Set<ParametrizedType> relevantInvocations(Collection<Set<LiteralReferenceType>> superTypeSets, LiteralReferenceType type) {
        // Search for an invocation of the erased type in any of the super type sets
        final Set<ParametrizedType> invocations = new HashSet<>();
        for (Set<LiteralReferenceType> superTypes : superTypeSets) {
            for (LiteralReferenceType superType : superTypes) {
                if (superType instanceof ParametrizedType) {
                    final ParametrizedType parametrizedType = (ParametrizedType) superType;
                    if (parametrizedType.getErasure().equals(type)) {
                        invocations.add(parametrizedType);
                    }
                }
            }
        }
        return invocations;
    }

    private static Set<LiteralReferenceType> getErasedTypes(Set<LiteralReferenceType> types) {
        final Set<LiteralReferenceType> erased = new HashSet<>();
        for (LiteralReferenceType type : types) {
            erased.add(type.getErasure());
        }
        return erased;
    }

    public static Set<LiteralReferenceType> getSuperTypes(ReferenceType type) {
        final Set<LiteralReferenceType> result = new HashSet<>();
        final Queue<ReferenceType> queue = new ArrayDeque<>();
        queue.add(type);
        while (!queue.isEmpty()) {
            final ReferenceType child = queue.remove();
            final LiteralReferenceType literalChild = child instanceof LiteralReferenceType ? (LiteralReferenceType) child : null;
            // Non literal types are always scanned because they cannot be in the super type set (since it only contains literals)
            if (literalChild == null || result.add(literalChild)) {
                queue.addAll(child.getDirectSuperTypes());
            }
        }
        return result;
    }

    public static PrimitiveType coerceToPrimitive(Environment environment, Expression expression) {
        return coerceToPrimitive(expression, expression.getType(environment));
    }

    public static PrimitiveType coerceToPrimitive(Expression expression, Type type) {
        final PrimitiveType primitiveType;
        if (type instanceof PrimitiveType) {
            primitiveType = (PrimitiveType) type;
        } else if (type instanceof LiteralReferenceType && ((LiteralReferenceType) type).isBox()) {
            primitiveType = ((LiteralReferenceType) type).unbox();
        } else {
            throw new EvaluatorException("Not a primitive type: " + type.getName(), expression);
        }
        return primitiveType;
    }

    public static boolean isValidReferenceCast(ReferenceType sourceType, ReferenceType targetType) {
        if (sourceType.isNull()) {
            // source type is null, can always cast to anything
            return true;
        }
        if (sourceType instanceof LiteralReferenceType) {
            // source is a literal reference type, which has sub-cases
            final LiteralReferenceType source = (LiteralReferenceType) sourceType;
            if (source.isArray()) {
                // source is an array type, which has sub-cases
                if (targetType instanceof LiteralReferenceType) {
                    // target is a literal reference type, which has sub-cases
                    final LiteralReferenceType target = (LiteralReferenceType) targetType;
                    if (target.isArray()) {
                        // target is an array type, allow if same primitive type else apply recursively on components
                        final ComponentType sourceComponent = source.getComponentType();
                        final ComponentType targetComponent = target.getComponentType();
                        return sourceComponent.isPrimitive() && targetComponent.isPrimitive() && sourceComponent.equals(targetComponent)
                                || isValidReferenceCast((ReferenceType) sourceComponent, (ReferenceType) targetComponent);
                    }
                    // target is a class or interface type, only allow object, serializable and cloneable
                    return target.equals(LiteralReferenceType.THE_OBJECT) || target.equals(LiteralReferenceType.THE_SERIALIZABLE)
                            || target.equals(LiteralReferenceType.THE_CLONEABLE);
                }
                if (targetType instanceof TypeVariable) {
                    // target is a type variable, apply recursively to upper bound
                    final TypeVariable target = (TypeVariable) targetType;
                    return isValidReferenceCast(source, target.getUpperBound());
                }
                // any other target type is undefined
                return false;
            }
            if (source.isInterface()) {
                // source is an interface type, which has sub-cases
                if (targetType instanceof LiteralReferenceType) {
                    // target is a literal reference type, which has sub-cases
                    final LiteralReferenceType target = (LiteralReferenceType) targetType;
                    if (target.isArray()) {
                        // target is an array type, source must be serializable or cloneable
                        return source.equals(LiteralReferenceType.THE_SERIALIZABLE) || source.equals(LiteralReferenceType.THE_CLONEABLE);
                    }
                    if (Modifier.isFinal(target.getTypeClass().getModifiers())) {
                        // target is final, which has sub-cases
                        if (target instanceof ParametrizedType || target.isRaw()) {
                            // target is parametrized or raw, target must be an invocation of source generic declaration
                            return hasInvocationParent(target, source);
                        }
                        // target is neither parametrized nor raw, target must implement source
                        return target.convertibleTo(source);
                    }
                    // target is not final, generic parents cannot conflict
                    return !haveGenericallyDistinctParents(source, target);
                }
                // any other target type is always valid
                return true;
            }
            // source is a class type, which has sub-cases
            if (targetType instanceof LiteralReferenceType) {
                // target is a literal reference type, which has sub-cases
                final LiteralReferenceType target = (LiteralReferenceType) targetType;
                if (target.isArray()) {
                    // target is an array type, source must be object
                    return source.equals(LiteralReferenceType.THE_OBJECT);
                }
                if (target.isInterface()) {
                    // target is an interface type, which has sub-cases
                    if (Modifier.isFinal(source.getTypeClass().getModifiers())) {
                        // source is final, source must implement target
                        return source.convertibleTo(target);
                    }
                    // source is not final, generic parents cannot conflict
                    return !haveGenericallyDistinctParents(source, target);
                }
                // target is a class type, erasures must be super or sub types and generic parents cannot conflict
                final LiteralReferenceType sourceErasure = source.getErasure();
                final LiteralReferenceType targetErasure = target.getErasure();
                return (sourceErasure.convertibleTo(targetErasure) || targetErasure.convertibleTo(targetErasure))
                        && !haveGenericallyDistinctParents(source, target);
            }
            if (targetType instanceof TypeVariable) {
                // target is a type variable, apply recursively to upper bound
                final TypeVariable target = (TypeVariable) targetType;
                return isValidReferenceCast(source, target.getUpperBound());
            }
        }
        if (sourceType instanceof TypeVariable) {
            // source is a type variable, apply recursively to upper bound
            final TypeVariable source = (TypeVariable) sourceType;
            return isValidReferenceCast(source.getUpperBound(), targetType);
        }
        if (sourceType instanceof IntersectionType) {
            // source is an intersection type, apply to each member individually
            final IntersectionType source = (IntersectionType) sourceType;
            for (SingleReferenceType type : source.getTypes()) {
                if (!isValidReferenceCast(type, targetType)) {
                    return false;
                }
            }
            return true;
        }
        // any other source type is undefined
        return false;
    }

    private static boolean haveGenericallyDistinctParents(LiteralReferenceType left, LiteralReferenceType right) {
        final Map<LiteralReferenceType, LiteralReferenceType> leftSuperTypes = new HashMap<>();
        final Map<LiteralReferenceType, LiteralReferenceType> rightSuperTypes = new HashMap<>();
        // Get maps of erased types to full ones for left and right types
        for (LiteralReferenceType superType : getSuperTypes(left)) {
            leftSuperTypes.put(superType.getErasure(), superType);
        }
        for (LiteralReferenceType superType : getSuperTypes(right)) {
            rightSuperTypes.put(superType.getErasure(), superType);
        }
        // Intersect the erased super types to get all common between left and right
        leftSuperTypes.keySet().retainAll(rightSuperTypes.keySet());
        // Left and right can have super types with the same erasures, but not with the same type arguments
        for (Map.Entry<LiteralReferenceType, LiteralReferenceType> leftEntry : leftSuperTypes.entrySet()) {
            if (!leftEntry.getValue().equals(rightSuperTypes.get(leftEntry.getKey()))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasInvocationParent(LiteralReferenceType type, LiteralReferenceType invocation) {
        // We need an invocation of the same declaration in the super types (that is, same erasures) with the same arguments
        final LiteralReferenceType declaration = invocation.getErasure();
        for (LiteralReferenceType superType : getSuperTypes(type)) {
            if (superType.getErasure().equals(declaration)) {
                return superType.equals(invocation);
            }
        }
        return false;
    }

    public static Class<?> findNameMatch(ReferenceType type, List<Identifier> name) {
        if (type instanceof LiteralReferenceType) {
            final LiteralReferenceType singleClass = (LiteralReferenceType) type;
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
        if (type instanceof IntersectionType) {
            for (ReferenceType referenceType : ((IntersectionType) type).getTypes()) {
                final Class<?> match = findNameMatch(referenceType, name);
                if (match != null) {
                    return match;
                }
            }
            return null;
        }
        return null;
    }

    private interface Filter<S, T> {
        boolean test(S s, T t);
    }

    private static class SubTypeFilter implements Filter<Type, Type> {
        private static final SubTypeFilter INSTANCE = new SubTypeFilter();

        @Override
        public boolean test(Type left, Type right) {
            return left.convertibleTo(right);
        }
    }

    private static class SuperTypeFilter implements Filter<Type, Type> {
        private static final SuperTypeFilter INSTANCE = new SuperTypeFilter();

        @Override
        public boolean test(Type left, Type right) {
            return right.convertibleTo(left);
        }
    }
}
