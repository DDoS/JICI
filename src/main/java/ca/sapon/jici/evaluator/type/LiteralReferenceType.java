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
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import ca.sapon.jici.evaluator.Substitutions;
import ca.sapon.jici.evaluator.member.ArrayCloneCallable;
import ca.sapon.jici.evaluator.member.ArrayLengthVariable;
import ca.sapon.jici.evaluator.member.Callable;
import ca.sapon.jici.evaluator.member.ClassVariable;
import ca.sapon.jici.evaluator.member.ConstructorCallable;
import ca.sapon.jici.evaluator.member.InstanceVariable;
import ca.sapon.jici.evaluator.member.MethodCallable;
import ca.sapon.jici.util.ReflectionUtil;
import ca.sapon.jici.util.StringUtil;
import ca.sapon.jici.util.TypeUtil;

/**
 * A reference type literally used in the code, such as {@code String}, {@code Outer.Inner}, {@code Outer.Inner<Integer>}, {@code Outer<String>.Inner} or {@code Outer<String>.Inner<Integer>}, {@code
 * Set<T>}, {@code Map<? extends CharSequence, Integer>}, {@code Map.Entry<String, Integer>} or {@code int[]}.
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
    private java.lang.reflect.TypeVariable<?>[] parameters = null;

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
        return getTypeParameters().length > 0;
    }

    public boolean isInterface() {
        return type.isInterface();
    }

    public boolean isStatic() {
        return isInterface() || Modifier.isStatic(type.getModifiers());
    }

    @Override
    public Class<?> getTypeClass() {
        return type;
    }

    protected java.lang.reflect.TypeVariable<?>[] getTypeParameters() {
        if (this.parameters == null) {
            // If this is an array, we need to get to the base component type
            Class<?> base = type;
            while (base.isArray()) {
                base = base.getComponentType();
            }
            this.parameters = base.getTypeParameters();
        }
        return parameters;
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

    @Override
    public LiteralReferenceType getErasure() {
        return this;
    }

    @Override
    public Set<LiteralReferenceType> getDirectSuperTypes() {
        final Set<LiteralReferenceType> superTypes = new HashSet<>();
        if (isArray()) {
            // Find the number of dimensions of the array and the base component type
            int dimensions = 0;
            ComponentType componentType = this;
            do {
                if (!(componentType instanceof ReferenceType)) {
                    break;
                }
                componentType = ((ReferenceType) componentType).getComponentType();
                dimensions++;
            } while (componentType.isArray());
            if (componentType.equals(LiteralReferenceType.THE_OBJECT)) {
                // For an object component type we use the actual array direct super types
                superTypes.add(LiteralReferenceType.THE_OBJECT.asArray(dimensions - 1));
                superTypes.add(LiteralReferenceType.THE_SERIALIZABLE.asArray(dimensions - 1));
                superTypes.add(LiteralReferenceType.THE_CLONEABLE.asArray(dimensions - 1));
            } else {
                // Add all the component direct super types as arrays of the same dimension
                if (componentType instanceof LiteralReferenceType) {
                    for (LiteralReferenceType superType : ((LiteralReferenceType) componentType).getDirectSuperTypes()) {
                        superTypes.add(superType.asArray(dimensions));
                    }
                }
            }
        } else {
            // Add the direct super class and the directly implemented interfaces
            if (isInterface()) {
                // Interfaces have object as an implicit direct super class
                superTypes.add(LiteralReferenceType.THE_OBJECT);
            } else {
                // This will always return something unless this class is object
                final LiteralReferenceType superClass = getDirectSuperClass();
                if (superClass != null) {
                    superTypes.add(superClass);
                }
            }
            Collections.addAll(superTypes, getDirectlyImplementedInterfaces());
        }
        return superTypes;
    }

    @Override
    public LinkedHashSet<LiteralReferenceType> getSuperTypes() {
        final LinkedHashSet<LiteralReferenceType> result = new LinkedHashSet<>();
        final Queue<LiteralReferenceType> queue = new ArrayDeque<>();
        queue.add(capture());
        final boolean raw = isRaw();
        while (!queue.isEmpty()) {
            LiteralReferenceType child = queue.remove();
            if (raw) {
                child = child.getErasure();
            }
            if (result.add(child)) {
                queue.addAll(child.getDirectSuperTypes());
            }
        }
        return result;
    }

    @Override
    public LiteralReferenceType substituteTypeVariables(Substitutions substitution) {
        return this;
    }

    @Override
    public Set<TypeVariable> getTypeVariables() {
        return new HashSet<>();
    }

    public LiteralReferenceType getDirectSuperClass() {
        final java.lang.reflect.Type superClass = type.getGenericSuperclass();
        if (superClass != null) {
            final LiteralReferenceType wrappedClass = (LiteralReferenceType) TypeUtil.wrap(superClass);
            return isRaw() ? wrappedClass.getErasure() : wrappedClass;
        }
        return null;
    }

    public LiteralReferenceType[] getDirectlyImplementedInterfaces() {
        final java.lang.reflect.Type[] interfaces = type.getGenericInterfaces();
        final boolean raw = isRaw();
        final LiteralReferenceType[] wrapped = new LiteralReferenceType[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            final LiteralReferenceType wrappedInterface = (LiteralReferenceType) TypeUtil.wrap(interfaces[i]);
            wrapped[i] = raw ? wrappedInterface.getErasure() : wrappedInterface;
        }
        return wrapped;
    }

    @Override
    public LiteralReferenceType capture() {
        return this;
    }

    @Override
    public ComponentType getComponentType() {
        final Class<?> componentType = type.getComponentType();
        if (componentType == null) {
            throw new UnsupportedOperationException("Not an array type");
        }
        return (ComponentType) TypeUtil.wrap(componentType);
    }

    @Override
    public LiteralReferenceType asArray(int dimensions) {
        return of(ReflectionUtil.asArrayType(type, dimensions));
    }

    @Override
    public Object newArray(int length) {
        return Array.newInstance(type, length);
    }

    @Override
    public Object newArray(int[] lengths) {
        return Array.newInstance(type, lengths);
    }

    @Override
    public boolean contains(TypeArgument other) {
        return equals(other);
    }

    @Override
    public boolean convertibleTo(Type to) {
        // Literal class types might be convertible to a primitive if they can be unboxed
        if (to.isPrimitive()) {
            return isBox() && unbox().convertibleTo(to);
        }
        // If the target literal type is parametrized, this type must have a parent with
        // the same erasure and compatible type arguments
        if (to instanceof ParametrizedType) {
            final ParametrizedType parametrized = (ParametrizedType) to;
            final LiteralReferenceType erasure = parametrized.getErasure();
            for (LiteralReferenceType superType : getSuperTypes()) {
                if (superType.getErasure().equals(erasure)) {
                    return !(superType instanceof ParametrizedType)
                            || parametrized.argumentsContain(((ParametrizedType) superType).getArguments());
                }
            }
            return false;
        }
        // Else they can be converted to another literal class if they are a subtype
        if (to instanceof LiteralReferenceType) {
            final LiteralReferenceType target = (LiteralReferenceType) to;
            return target.type.isAssignableFrom(type);
        }
        // They can also be converted to a type variable lower bound
        if (to instanceof TypeVariable) {
            final TypeVariable target = (TypeVariable) to;
            return convertibleTo(target.getLowerBound());
        }
        // They can also be converted to an intersection if they can be converted to each member
        if (to instanceof IntersectionType) {
            final IntersectionType target = (IntersectionType) to;
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
    public boolean isUncheckedConversion(Type to) {
        if (!(to instanceof ParametrizedType)) {
            return false;
        }
        final ParametrizedType parametrized = (ParametrizedType) to;
        final LiteralReferenceType erasure = parametrized.getErasure();
        for (LiteralReferenceType superType : getSuperTypes()) {
            if (superType.getErasure().equals(erasure)) {
                return !(superType instanceof ParametrizedType);
            }
        }
        return false;
    }

    public ClassVariable getDeclaredField(String name) {
        // Array length field must be handled specially because the reflection API doesn't declare it
        if (isArray() && "length".equals(name)) {
            return ArrayLengthVariable.of(this);
        }
        // Only one field can match the name, not overloads are possible
        for (Field field : type.getDeclaredFields()) {
            // Ignore non-public fields and synthetic fields generated by the compiler
            if (!Modifier.isPublic(field.getModifiers()) || field.isSynthetic()) {
                continue;
            }
            // Check name match
            if (!field.getName().equals(name)) {
                continue;
            }
            return InstanceVariable.of(this, field);
        }
        return null;
    }

    public Set<ConstructorCallable> getDeclaredConstructors(TypeArgument[] typeArguments) {
        final Set<ConstructorCallable> constructors = new HashSet<>();
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            // Ignore non-public constructors and synthetic constructors generated by the compiler
            if (!Modifier.isPublic(constructor.getModifiers()) || constructor.isSynthetic()) {
                continue;
            }
            // Try to create a callable for the constructor, might fail if the type arguments are out of bounds
            final ConstructorCallable callable = ConstructorCallable.of(this, constructor, typeArguments);
            if (callable != null) {
                constructors.add(callable);
            }
        }
        return constructors;
    }

    public Set<? extends Callable> getDeclaredMethods(String name, TypeArgument[] typeArguments) {
        // Array clone method must be handled specially because the reflection API doesn't declare it
        if (isArray() && "clone".equals(name)) {
            return Collections.singleton(ArrayCloneCallable.of(this));
        }
        final Set<MethodCallable> methods = new HashSet<>();
        for (Method method : type.getDeclaredMethods()) {
            // Ignore non-public methods and synthetic methods generated by the compiler
            if (!Modifier.isPublic(method.getModifiers()) || method.isSynthetic()) {
                continue;
            }
            // Check name match
            if (!method.getName().equals(name)) {
                continue;
            }
            // Try to create a callable for the method, might fail if the type arguments are out of bounds
            final MethodCallable callable = MethodCallable.of(this, method, typeArguments);
            if (callable != null) {
                methods.add(callable);
            }
        }
        return methods;
    }

    @Override
    public ClassVariable getField(String name) {
        final LinkedHashSet<LiteralReferenceType> superTypes = getSuperTypes();
        for (LiteralReferenceType type : superTypes) {
            final ClassVariable field = type.getDeclaredField(name);
            if (field != null) {
                return field;
            }
        }
        throw new UnsupportedOperationException("No field named " + name + " in " + getName());
    }

    @Override
    public ConstructorCallable getConstructor(TypeArgument[] typeArguments, Type[] arguments) {
        // Get the declared constructors and look for applicable candidates with and without using vararg
        final Set<ConstructorCallable> regularCandidates = new HashSet<>();
        final Set<ConstructorCallable> varargCandidates = new HashSet<>();
        final Set<ConstructorCallable> constructors = getDeclaredConstructors(typeArguments);
        for (ConstructorCallable constructor : constructors) {
            if (constructor.isApplicable(arguments)) {
                regularCandidates.add(constructor);
            }
            if (!constructor.supportsVararg()) {
                continue;
            }
            final ConstructorCallable varargConstructor = constructor.useVararg();
            if (varargConstructor.isApplicable(arguments)) {
                varargCandidates.add(varargConstructor);
            }
        }
        // Resolve overloads
        ConstructorCallable callable = reduceCallableCandidates(regularCandidates, arguments);
        // If the regular callables don't work, try with vararg
        if (callable == null) {
            callable = reduceCallableCandidates(varargCandidates, arguments);
        }
        if (callable != null) {
            return callable.requiresUncheckedConversion(arguments) ? callable.eraseReturnType() : callable;
        }
        throw new UnsupportedOperationException("No constructor for signature: " +
                (typeArguments.length > 0 ? '<' + StringUtil.toString(typeArguments, ", ") + '>' : "") +
                type.getSimpleName() + "(" + StringUtil.toString(Arrays.asList(arguments), ", ") + ") in " + getName());
    }

    @Override
    public Callable getMethod(String name, TypeArgument[] typeArguments, Type[] arguments) {
        // Get the declared methods and look for applicable candidates with and without using vararg
        final Set<Callable> regularCandidates = new HashSet<>();
        final Set<Callable> varargCandidates = new HashSet<>();
        final LinkedHashSet<LiteralReferenceType> superTypes = getSuperTypes();
        for (LiteralReferenceType type : superTypes) {
            for (Callable method : type.getDeclaredMethods(name, typeArguments)) {
                if (method.isApplicable(arguments)) {
                    regularCandidates.add(method);
                }
                if (!method.supportsVararg()) {
                    continue;
                }
                final Callable varargMethod = method.useVararg();
                if (varargMethod.isApplicable(arguments)) {
                    varargCandidates.add(varargMethod);
                }
            }
        }
        // Resolve overloads
        Callable callable = reduceCallableCandidates(regularCandidates, arguments);
        // If the regular callables don't work, try with vararg
        if (callable == null) {
            callable = reduceCallableCandidates(varargCandidates, arguments);
        }
        if (callable != null) {
            return callable.requiresUncheckedConversion(arguments) ? callable.eraseReturnType() : callable;
        }
        throw new UnsupportedOperationException("No method for signature: " +
                (typeArguments.length > 0 ? '<' + StringUtil.toString(typeArguments, ", ") + '>' : "") +
                name + "(" + StringUtil.toString(Arrays.asList(arguments), ", ") + ") in " + getName());
    }

    private static <C extends Callable> C reduceCallableCandidates(Set<C> candidates, Type[] arguments) {
        // Reduce the regular candidates to keep only the most applicable ones
        for (Iterator<C> iterator = candidates.iterator(); iterator.hasNext(); ) {
            final C candidate1 = iterator.next();
            for (C candidate2 : candidates) {
                // Don't compare with itself
                if (candidate1 == candidate2) {
                    continue;
                }
                // Remove candidate1 is candidate2 is more applicable
                if (candidate2.isMoreApplicableThan(candidate1, arguments)) {
                    iterator.remove();
                    break;
                }
            }
        }
        return candidates.size() != 1 ? null : candidates.iterator().next();
    }

    public Substitutions getSubstitutions() {
        return Substitutions.NONE;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof LiteralReferenceType) && this.type == ((LiteralReferenceType) other).type;
    }

    @Override
    public int hashCode() {
        return type.getName().hashCode();
    }

    public static LiteralReferenceType of(Class<?> type) {
        checkOwner(null, type);
        return new LiteralReferenceType(type);
    }

    protected static ParametrizedType checkOwner(ParametrizedType paramOwner, Class<?> inner) {
        return checkOwner(paramOwner, inner, Collections.<TypeArgument>emptyList());
    }

    protected static ParametrizedType checkOwner(ParametrizedType paramOwner, Class<?> inner, List<TypeArgument> arguments) {
        final LiteralReferenceType owner;
        final Class<?> enclosingClass = inner.getEnclosingClass();
        if (paramOwner != null) {
            // If the owner is given, check that it matches what is actually declared in the code
            if (!paramOwner.getTypeClass().equals(enclosingClass)) {
                throw new UnsupportedOperationException("Mismatch between given owner and actual one: " + paramOwner + " and " + enclosingClass);
            }
            // Also check that the inner type is selectable (that is, not static)
            if (Modifier.isStatic(inner.getModifiers())) {
                throw new UnsupportedOperationException("Inner type " + inner.getSimpleName() + " cannot be used in a static context");
            }
            owner = paramOwner;
        } else if (enclosingClass != null) {
            // If it's not given, check if enclosing class is applicable: the inner class cannot be static
            if (!Modifier.isStatic(inner.getModifiers())) {
                owner = LiteralReferenceType.of(enclosingClass);
            } else {
                // The enclosing class isn't an owner type
                owner = null;
            }
        } else {
            // No enclosing class
            owner = null;
        }
        if (owner != null) {
            // Now that we found an owner, make sure we don't have a partially raw owner/inner combination
            // Owner and inner cannot have one be raw and one not raw
            final boolean ownerParam = owner instanceof ParametrizedType;
            final boolean ownerRaw = owner.isRaw();
            final boolean innerParam = !arguments.isEmpty();
            final boolean innerRaw = inner.getTypeParameters().length > 0 && arguments.isEmpty();
            if ((ownerParam || ownerRaw) && (innerParam || innerRaw) && ownerRaw != innerRaw) {
                throw new UnsupportedOperationException("Cannot have a mix of raw and generic outer and inner classes");
            }
            // We only care of the owner if it is parametrized, as the type arguments are accessible by inner types
            // We want to access those, the rest doesn't matter
            return ownerParam ? (ParametrizedType) owner : null;
        }
        return null;
    }
}
