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
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import ca.sapon.jici.evaluator.Callable;
import ca.sapon.jici.evaluator.ClassVariable;
import ca.sapon.jici.evaluator.Substitutions;
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
    public Set<SingleReferenceType> getDirectSuperTypes() {
        final Set<SingleReferenceType> superTypes = new HashSet<>();
        if (isArray()) {
            // Find the number of dimensions of the array and the base component type
            int dimensions = 0;
            LiteralReferenceType componentType = this;
            do {
                componentType = (LiteralReferenceType) componentType.getComponentType();
                dimensions++;
            } while (componentType.isArray());
            if (componentType.equals(LiteralReferenceType.THE_OBJECT)) {
                // For an object component type we use the actual array direct super types
                superTypes.add(LiteralReferenceType.THE_OBJECT.asArray(dimensions - 1));
                superTypes.add(LiteralReferenceType.THE_SERIALIZABLE.asArray(dimensions - 1));
                superTypes.add(LiteralReferenceType.THE_CLONEABLE.asArray(dimensions - 1));
            } else {
                // Add all the component direct super types as arrays of the same dimension
                for (SingleReferenceType superType : componentType.getDirectSuperTypes()) {
                    superTypes.add(superType.asArray(dimensions));
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
            for (LiteralReferenceType superType : TypeUtil.getSuperTypes(this)) {
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
    public Callable getConstructor(TypeArgument[] typeArguments, Type[] arguments) {
        final int argumentCount = arguments.length;
        // Search for both regular and vararg candidates at the same time
        final Map<Constructor<?>, Type[]> candidates = new HashMap<>();
        final Map<Constructor<?>, Type[]> varargCandidate = new HashMap<>();
        // Generate the substitutions for this type so we can get constructors for parametrized types
        final LiteralReferenceType capture = this.capture();
        final Substitutions substitutions = capture.getSubstitutions();
        // Search all the candidate constructors
        final Constructor<?>[] constructors = type.getDeclaredConstructors();
        for (Constructor<?> candidate : constructors) {
            // Ignore non-public constructors and synthetic constructors generated by the compiler
            if (!Modifier.isPublic(candidate.getModifiers()) || candidate.isSynthetic()) {
                continue;
            }
            final java.lang.reflect.Type[] parameterTypes = candidate.getGenericParameterTypes();
            // look for matches in length
            if (parameterTypes.length == argumentCount) {
                // Now check if the type arguments are suitable, if the method declares type parameters
                final Substitutions combinedSubstitutions = checkTypeArguments(substitutions, candidate.getTypeParameters(), typeArguments);
                if (combinedSubstitutions != null) {
                    // Apply substitutions for parameters to support parametrized types
                    candidates.put(candidate, substituteParameters(combinedSubstitutions, TypeUtil.wrap(parameterTypes)));
                }
            }
            // look for varargs with matches in name and length of non-varargs
            if (candidate.isVarArgs() && parameterTypes.length - 1 <= argumentCount) {
                // Now check if the type arguments are suitable, if the method declares type parameters
                final Substitutions combinedSubstitutions = checkTypeArguments(substitutions, candidate.getTypeParameters(), typeArguments);
                if (combinedSubstitutions != null) {
                    // Apply substitutions for parameters to support parametrized types
                    final Type[] substitutedParameters = substituteParameters(combinedSubstitutions, TypeUtil.wrap(parameterTypes));
                    // expand the parameters through the vararg to match the argument count
                    varargCandidate.put(candidate, ReflectionUtil.expandsVarargs(substitutedParameters, argumentCount));
                }
            }
        }
        // try to resolve the overloads
        Constructor<?> constructor = ReflectionUtil.resolveOverloads(candidates, arguments);
        if (constructor != null) {
            return Callable.forConstructor(capture, constructor);
        }
        // try vararg candidates
        constructor = ReflectionUtil.resolveOverloads(varargCandidate, arguments);
        if (constructor != null) {
            return Callable.forVarargConstructor(capture, constructor);
        }
        throw new UnsupportedOperationException("No constructor for signature: " +
                (typeArguments.length > 0 ? '<' + StringUtil.toString(typeArguments, ", ") + '>' : "") +
                type.getSimpleName() + "(" + StringUtil.toString(Arrays.asList(arguments), ", ") + ") in " + getName());
    }

    @Override
    public ClassVariable getField(String name) {
        if (isArray() && "length".equals(name)) {
            return ClassVariable.forArrayLength();
        }
        // Search the type hierarchy from bottom to top
        final Queue<SingleReferenceType> searchQueue = new ArrayDeque<>();
        searchQueue.add(this);
        do {
            final SingleReferenceType type = searchQueue.poll();
            if (!(type instanceof LiteralReferenceType)) {
                throw new UnsupportedOperationException("Expected only literal reference types in the type hierarchy");
            }
            final LiteralReferenceType literal = (LiteralReferenceType) type;
            try {
                final Field field = literal.getTypeClass().getDeclaredField(name);
                // Ignore non-public fields and synthetic fields generated by the compiler
                if (!Modifier.isPublic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                // Pass the type variable substitutions to properly handle fields of parametrized types
                return ClassVariable.forField(literal.capture().getSubstitutions(), field);
            } catch (NoSuchFieldException ignored) {
            }
            // Search the super types after (to respect shadowing rules)
            searchQueue.addAll(type.getDirectSuperTypes());
        } while (!searchQueue.isEmpty());
        throw new UnsupportedOperationException("No field named " + name + " in " + getName());
    }

    @Override
    public Callable getMethod(String name, TypeArgument[] typeArguments, Type[] arguments) {
        final int argumentCount = arguments.length;
        if (isArray() && argumentCount == 0 && "clone".equals(name)) {
            return Callable.forArrayClone(this);
        }
        // Search for both regular and vararg candidates at the same time
        final Map<Method, Type[]> candidates = new HashMap<>();
        final Map<Method, Type[]> varargCandidate = new HashMap<>();
        // Keep a map of the substitutions for each declaring type since they will differ
        final Map<Method, Substitutions> declarorSubstitutions = new HashMap<>();
        // Search the type hierarchy from bottom to top
        final Queue<SingleReferenceType> searchQueue = new ArrayDeque<>();
        searchQueue.add(this);
        do {
            final SingleReferenceType type = searchQueue.poll();
            if (!(type instanceof LiteralReferenceType)) {
                throw new UnsupportedOperationException("Expected only literal reference types in the type hierarchy");
            }
            final LiteralReferenceType literal = (LiteralReferenceType) type;
            // Generate the substitutions for this type so we can get constructors for parametrized types
            final Substitutions substitutions = literal.capture().getSubstitutions();
            // Search the methods for the type
            final Method[] methods = literal.getTypeClass().getDeclaredMethods();
            for (Method candidate : methods) {
                // Ignore non-public methods and synthetic methods generated by the compiler
                if (!Modifier.isPublic(candidate.getModifiers()) || candidate.isSynthetic()) {
                    continue;
                }
                // Check for a name match
                if (!candidate.getName().equals(name)) {
                    continue;
                }
                final java.lang.reflect.Type[] parameterTypes = candidate.getGenericParameterTypes();
                // look for matches in length parameter types
                if (parameterTypes.length == argumentCount) {
                    // Now check if the type arguments are suitable, if the method declares type parameters
                    final Substitutions combinedSubstitutions = checkTypeArguments(substitutions, candidate.getTypeParameters(), typeArguments);
                    if (combinedSubstitutions != null) {
                        candidates.put(candidate, substituteParameters(combinedSubstitutions, TypeUtil.wrap(parameterTypes)));
                        declarorSubstitutions.put(candidate, combinedSubstitutions);
                    }
                }
                // look for varargs with matches in name and length of non-varargs
                if (candidate.isVarArgs() && parameterTypes.length - 1 <= argumentCount) {
                    // Now check if the type arguments are suitable, if the method declares type parameters
                    final Substitutions combinedSubstitutions = checkTypeArguments(substitutions, candidate.getTypeParameters(), typeArguments);
                    if (combinedSubstitutions != null) {
                        // Apply substitutions for parameters to support parametrized types
                        final Type[] substitutedParameters = substituteParameters(combinedSubstitutions, TypeUtil.wrap(parameterTypes));
                        // expand the parameters through the vararg to match the argument count
                        varargCandidate.put(candidate, ReflectionUtil.expandsVarargs(substitutedParameters, argumentCount));
                        declarorSubstitutions.put(candidate, combinedSubstitutions);
                    }
                }
            }
            // Search the super types after (to respect shadowing rules)
            searchQueue.addAll(type.getDirectSuperTypes());
        } while (!searchQueue.isEmpty());
        // generics can cause methods to only differ by the return type, so fix that
        ReflectionUtil.fixReturnTypeConflicts(candidates);
        // try to resolve the overloads
        Method method = ReflectionUtil.resolveOverloads(candidates, arguments);
        if (method != null) {
            return Callable.forMethod(declarorSubstitutions.get(method), method);
        }
        // try vararg candidates
        ReflectionUtil.fixReturnTypeConflicts(varargCandidate);
        method = ReflectionUtil.resolveOverloads(varargCandidate, arguments);
        if (method != null) {
            return Callable.forVarargMethod(declarorSubstitutions.get(method), method);
        }
        throw new UnsupportedOperationException("No method for signature: " +
                (typeArguments.length > 0 ? '<' + StringUtil.toString(typeArguments, ", ") + '>' : "") +
                name + "(" + StringUtil.toString(Arrays.asList(arguments), ", ") + ") in " + getName());
    }

    private Type[] substituteParameters(Substitutions substitutions, Type[] parameters) {
        for (int i = 0; i < parameters.length; i++) {
            final Type parameter = parameters[i];
            if (parameter instanceof TypeArgument) {
                parameters[i] = ((TypeArgument) parameter).substituteTypeVariables(substitutions);
            }
        }
        return parameters;
    }

    private Substitutions checkTypeArguments(Substitutions substitutions, java.lang.reflect.TypeVariable<?>[] parameters, TypeArgument[] arguments) {
        // It's allowed to call a non-parametrized method with type arguments
        if (parameters.length == 0) {
            return substitutions;
        }
        // Check for length match
        if (parameters.length != arguments.length) {
            return null;
        }
        // Wrap the parameters
        final TypeVariable[] signatureParameters = new TypeVariable[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            signatureParameters[i] = (TypeVariable) TypeUtil.wrap(parameters[i]);
        }
        // Generate the combined substitution of the declaring class and method parameters, following the shadowing rules
        final Map<String, TypeArgument> namesToArguments = new HashMap<>();
        // First add all declaring class arguments from the original substitutions
        namesToArguments.putAll(substitutions.getMap());
        // Now add the method ones, replacing the declaring class arguments (shadowing)
        namesToArguments.putAll(Substitutions.toSubstitutionMap(signatureParameters, arguments));
        final Substitutions combinedSubstitutions = new Substitutions(namesToArguments);
        // Apply the substitutions to the method parameter bounds to get the final method signature parameters
        for (int i = 0; i < signatureParameters.length; i++) {
            signatureParameters[i] = signatureParameters[i].substituteBoundTypeVariables(combinedSubstitutions);
        }
        // Check if arguments are within bounds
        for (int i = 0; i < signatureParameters.length; i++) {
            if (!signatureParameters[i].boundsContain(arguments[i])) {
                throw new UnsupportedOperationException("Cannot convert type argument " + arguments[i] + " to " + signatureParameters[i]);
            }
        }
        // Return the combined substitutions to be used on other types in the method signature
        return combinedSubstitutions;
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
