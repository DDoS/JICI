/*
 * This file is part of JICI, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015-2016 Aleksi Sapon <http://sapon.ca/jici/>
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

    public static TypeVariable[] wrap(java.lang.reflect.TypeVariable<?>[] typeVariables) {
        final TypeVariable[] wrapped = new TypeVariable[typeVariables.length];
        for (int i = 0; i < typeVariables.length; i++) {
            wrapped[i] = (TypeVariable) wrap(typeVariables[i]);
        }
        return wrapped;
    }

    public static Type wrap(java.lang.reflect.Type type) {
        return wrap(type, null);
    }

    public static Type wrap(java.lang.reflect.Type type, Set<java.lang.reflect.TypeVariable<?>> cycles) {
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
            final Type wrapped = wrap(componentType, cycles);
            if (wrapped instanceof ComponentType) {
                return ((ComponentType) wrapped).asArray(dimensions);
            }
            throw new UnsupportedOperationException("Invalid component type: " + wrapped.getName());
        }
        if (type instanceof ParameterizedType) {
            final ParameterizedType paramType = (ParameterizedType) type;
            final java.lang.reflect.Type[] args = paramType.getActualTypeArguments();
            final List<TypeArgument> wrappedArgs;
            if (args.length <= 0) {
                wrappedArgs = Collections.emptyList();
            } else {
                wrappedArgs = new ArrayList<>(args.length);
                for (java.lang.reflect.Type param : args) {
                    final Type wrap = wrap(param, cycles);
                    if (!(wrap instanceof TypeArgument)) {
                        throw new UnsupportedOperationException("Invalid type for generic parameter: " + wrap.getName());
                    }
                    wrappedArgs.add(((TypeArgument) wrap));
                }
            }
            final java.lang.reflect.Type ownerType = paramType.getOwnerType();
            final ParametrizedType wrappedOwner;
            if (ownerType != null) {
                final Type wrapped = wrap(ownerType, cycles);
                wrappedOwner = wrapped instanceof ParametrizedType ? (ParametrizedType) wrapped : null;
            } else {
                wrappedOwner = null;
            }
            if (!wrappedArgs.isEmpty() || wrappedOwner != null) {
                return ParametrizedType.of(wrappedOwner, (Class<?>) paramType.getRawType(), wrappedArgs, cycles);
            }
            return LiteralReferenceType.of((Class<?>) paramType.getRawType());
        }
        if (type instanceof java.lang.reflect.TypeVariable) {
            final java.lang.reflect.TypeVariable<?> typeVariable = (java.lang.reflect.TypeVariable<?>) type;
            if (cycles == null) {
                cycles = new HashSet<>();
            } else if (cycles.contains(typeVariable)) {
                return TypeVariable.of(typeVariable.getName(), IntersectionType.EVERYTHING, IntersectionType.NOTHING);
            }
            cycles.add(typeVariable);
            final List<SingleReferenceType> wrappedUpper = wrapBounds(typeVariable.getBounds(), new ArrayList<SingleReferenceType>(), cycles);
            return TypeVariable.of(typeVariable.getName(), wrappedUpper);
        }
        if (type instanceof java.lang.reflect.WildcardType) {
            final java.lang.reflect.WildcardType wildcardType = (java.lang.reflect.WildcardType) type;
            final Set<SingleReferenceType> wrappedLower = wrapBounds(wildcardType.getLowerBounds(), new HashSet<SingleReferenceType>(), cycles);
            final Set<SingleReferenceType> wrappedUpper = wrapBounds(wildcardType.getUpperBounds(), new HashSet<SingleReferenceType>(), cycles);
            return WildcardType.of(wrappedLower, wrappedUpper);
        }
        throw new UnsupportedOperationException(type.getClass().getCanonicalName());
    }

    private static <C extends Collection<SingleReferenceType>> C wrapBounds(java.lang.reflect.Type[] types, C to, Set<java.lang.reflect.TypeVariable<?>> cycles) {
        for (java.lang.reflect.Type type : types) {
            final Type wrap = wrap(type, cycles);
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

    private static <T extends Type> Set<T> filterTypes(Collection<T> types, BiPredicate<Type, Type> potentialFilter, BiPredicate<Type, Type> existingFilter) {
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

    public static Set<SingleReferenceType> expandIntersectionTypes(Collection<? extends ReferenceType> intersection) {
        final Set<SingleReferenceType> expandedTypes = new LinkedHashSet<>(intersection.size());
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
            return IntersectionType.of(types);
        }
        // Get the super type sets ST(U)
        final Map<ReferenceType, Set<LiteralReferenceType>> superTypeSets = new HashMap<>();
        final Collection<Set<LiteralReferenceType>> superTypes = superTypeSets.values();
        for (SingleReferenceType type : types) {
            if (type.convertibleTo(NullType.THE_NULL)) {
                // The null type has a universal set of super types, we can skip it
                continue;
            }
            superTypeSets.put(type, type.getSuperTypes());
        }
        // Intersect the erased super type sets EST(U) to generate the erased candidate set EC
        final Set<LiteralReferenceType> erasedCandidates = new HashSet<>();
        final Iterator<Set<LiteralReferenceType>> superTypesIterator = superTypes.iterator();
        erasedCandidates.addAll(getErasures(superTypesIterator.next()));
        while (superTypesIterator.hasNext()) {
            erasedCandidates.retainAll(getErasures(superTypesIterator.next()));
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

    private static Set<LiteralReferenceType> getErasures(Set<LiteralReferenceType> types) {
        final Set<LiteralReferenceType> erased = new HashSet<>();
        for (LiteralReferenceType type : types) {
            erased.add(type.getErasure());
        }
        return erased;
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
                            return hasInvocationSuperType(target, source);
                        }
                        // target is neither parametrized nor raw, target must implement source
                        return target.convertibleTo(source);
                    }
                    // target is not final, generic parents cannot conflict
                    return !haveProvablyDistinctInvocationsInSuperTypes(source, target);
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
                    return !haveProvablyDistinctInvocationsInSuperTypes(source, target);
                }
                // target is a class type, erasures must be super or sub types and generic parents cannot conflict
                final LiteralReferenceType sourceErasure = source.getErasure();
                final LiteralReferenceType targetErasure = target.getErasure();
                return (sourceErasure.convertibleTo(targetErasure) || targetErasure.convertibleTo(targetErasure))
                        && !haveProvablyDistinctInvocationsInSuperTypes(source, target);
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

    public static boolean haveProvablyDistinctInvocationsInSuperTypes(LiteralReferenceType... types) {
        return haveProvablyDistinctInvocationsInSuperTypes(Arrays.asList(types));
    }

    public static boolean haveProvablyDistinctInvocationsInSuperTypes(Collection<LiteralReferenceType> types) {
        return validateInvocationsInSuperTypes(types, ProvablyDistinctFilter.INSTANCE);
    }

    public static boolean haveDifferentInvocationsInSuperTypes(LiteralReferenceType... types) {
        return haveDifferentInvocationsInSuperTypes(Arrays.asList(types));
    }

    public static boolean haveDifferentInvocationsInSuperTypes(Collection<LiteralReferenceType> types) {
        return validateInvocationsInSuperTypes(types, NotEqualsFilter.<ParametrizedType, ParametrizedType>getInstance());
    }

    public static boolean validateInvocationsInSuperTypes(Collection<LiteralReferenceType> types,
                                                          BiPredicate<ParametrizedType, ParametrizedType> test) {
        if (types.size() <= 1) {
            // Fast track trivial cases
            return false;
        }
        // Keep a map of all super types that have the same erasure, excluding non-parametrized types
        final Map<LiteralReferenceType, ParametrizedType> commonErasedSuperTypes = new HashMap<>();
        final Iterator<LiteralReferenceType> iterator = types.iterator();
        // Add all parametrized parents and their erasures for the first type
        for (LiteralReferenceType superType : iterator.next().getSuperTypes()) {
            if (superType instanceof ParametrizedType) {
                commonErasedSuperTypes.put(superType.getErasure(), (ParametrizedType) superType);
            }
        }
        // For the other types, look for an entry in the common set and ensure they have a valid parametrization
        // If not, fail; if so, add to the common set
        do {
            for (LiteralReferenceType superType : iterator.next().getSuperTypes()) {
                if (!(superType instanceof ParametrizedType)) {
                    continue;
                }
                final ParametrizedType parametrizedSuperType = (ParametrizedType) superType;
                final LiteralReferenceType erasure = parametrizedSuperType.getErasure();
                final ParametrizedType commonSuperType = commonErasedSuperTypes.get(erasure);
                if (commonSuperType != null) {
                    if (test.test(parametrizedSuperType, commonSuperType)) {
                        return true;
                    }
                } else {
                    commonErasedSuperTypes.put(erasure, parametrizedSuperType);
                }
            }
        } while (iterator.hasNext());
        // No conflict found, we're good
        return false;
    }

    private static boolean hasInvocationSuperType(LiteralReferenceType type, LiteralReferenceType invocation) {
        // We need an invocation of the same declaration in the super types (that is, same erasures) with the same arguments
        final LiteralReferenceType declaration = invocation.getErasure();
        for (LiteralReferenceType superType : type.getSuperTypes()) {
            if (superType.getErasure().equals(declaration)) {
                return superType.equals(invocation);
            }
        }
        return false;
    }

    private interface BiPredicate<S, T> {
        boolean test(S s, T t);
    }

    private static class SubTypeFilter implements BiPredicate<Type, Type> {
        private static final SubTypeFilter INSTANCE = new SubTypeFilter();

        @Override
        public boolean test(Type left, Type right) {
            return left.convertibleTo(right);
        }
    }

    private static class SuperTypeFilter implements BiPredicate<Type, Type> {
        private static final SuperTypeFilter INSTANCE = new SuperTypeFilter();

        @Override
        public boolean test(Type left, Type right) {
            return right.convertibleTo(left);
        }
    }

    private static class NotEqualsFilter<S, T> implements BiPredicate<S, T> {
        private static final NotEqualsFilter<?, ?> INSTANCE = new NotEqualsFilter<>();

        @Override
        public boolean test(S left, T right) {
            return !left.equals(right);
        }

        @SuppressWarnings("unchecked")
        private static <S, T> NotEqualsFilter<S, T> getInstance() {
            return (NotEqualsFilter<S, T>) INSTANCE;
        }
    }

    private static class ProvablyDistinctFilter implements BiPredicate<ParametrizedType, ParametrizedType> {
        private static final ProvablyDistinctFilter INSTANCE = new ProvablyDistinctFilter();

        @Override
        public boolean test(ParametrizedType left, ParametrizedType right) {
            return left.provablyDistinct(right);
        }
    }
}
