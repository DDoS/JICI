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
package ca.sapon.jici.evaluator;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import ca.sapon.jici.evaluator.type.ComponentType;
import ca.sapon.jici.evaluator.type.LiteralReferenceType;
import ca.sapon.jici.evaluator.type.ReferenceType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.type.TypeArgument;
import ca.sapon.jici.evaluator.type.TypeVariable;
import ca.sapon.jici.evaluator.value.ObjectValue;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.evaluator.value.VoidValue;
import ca.sapon.jici.util.ReflectionUtil;
import ca.sapon.jici.util.TypeUtil;

/**
 *
 */
public interface Callable {
    LiteralReferenceType getDeclaringType();

    TypeVariable[] getTypeParameters();

    Type[] getParameterTypes();

    Type getReturnType();

    boolean isStatic();

    boolean supportsVararg();

    boolean isVarargEnabled();

    Callable useVararg();

    boolean isApplicable(Type[] argumentTypes);

    boolean isMoreApplicableThan(Callable other, Type[] argumentTypes);

    Value call(Value target, Value... arguments);

    abstract class CommonCallable implements Callable {
        private final LiteralReferenceType declaror;
        private final TypeVariable[] typeParameters;
        private final Type[] parameterTypes;
        private final Type returnType;
        private boolean varargEnabled;

        protected CommonCallable(LiteralReferenceType declaror, TypeVariable[] typeParameters, Type returnType,
                                 java.lang.reflect.Type[] genericParameterTypes, Substitutions substitutions) {
            this(declaror, typeParameters, getParameterTypes(genericParameterTypes, substitutions), returnType, false);
        }

        protected CommonCallable(LiteralReferenceType declaror, TypeVariable[] typeParameters, Type[] parameterTypes, Type returnType, boolean varargEnabled) {
            this.declaror = declaror;
            this.typeParameters = typeParameters;
            this.parameterTypes = parameterTypes;
            this.returnType = returnType;
            this.varargEnabled = varargEnabled;
        }

        @Override
        public LiteralReferenceType getDeclaringType() {
            return declaror;
        }

        @Override
        public TypeVariable[] getTypeParameters() {
            return typeParameters;
        }

        @Override
        public Type[] getParameterTypes() {
            return parameterTypes;
        }

        @Override
        public Type getReturnType() {
            return returnType;
        }

        @Override
        public boolean isVarargEnabled() {
            return varargEnabled;
        }

        @Override
        public abstract CommonCallable useVararg();

        @Override
        public boolean isApplicable(Type[] argumentTypes) {
            final Type[] expandedParameter;
            if (varargEnabled) {
                // When we use vararg, we can expand the last type parameter to have the required count
                if (argumentTypes.length < parameterTypes.length - 1) {
                    return false;
                }
                expandedParameter = ReflectionUtil.expandsVarargs(parameterTypes, argumentTypes.length);
            } else {
                if (argumentTypes.length != parameterTypes.length) {
                    return false;
                }
                expandedParameter = parameterTypes;
            }
            // Argument types must be convertible to the parameter types
            for (int i = 0; i < expandedParameter.length; i++) {
                if (!argumentTypes[i].convertibleTo(expandedParameter[i])) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean isMoreApplicableThan(Callable other, Type[] argumentTypes) {
            final Type[] otherParameterTypes = other.getParameterTypes();
            // When we use vararg, we compare extra arguments with the last one
            if (!varargEnabled && this.parameterTypes.length != otherParameterTypes.length) {
                return true;
            }
            // Parameter types must be convertible to the other parameter types
            final int largestLength = Math.max(this.parameterTypes.length, otherParameterTypes.length);
            for (int i = 0; i < largestLength; i++) {
                if (!isNarrowerParameter(
                        this.parameterTypes[Math.min(i, this.parameterTypes.length - 1)],
                        otherParameterTypes[Math.min(i, otherParameterTypes.length - 1)],
                        argumentTypes.length > 0 && argumentTypes[Math.min(i, argumentTypes.length - 1)].isPrimitive()
                )) {
                    return false;
                }
            }
            return true;
        }

        protected Object[] unwrapArguments(Value[] arguments) {
            final int argumentCount = arguments.length;
            Object[] values = new Object[argumentCount];
            for (int i = 0; i < argumentCount; i++) {
                values[i] = arguments[i].asObject();
            }
            final int parameterCount = parameterTypes.length;
            if (varargEnabled) {
                final int varargIndex = parameterCount - 1;
                if (argumentCount < varargIndex) {
                    throw new UnsupportedOperationException("Not enough argument values, expected at least " + varargIndex + " but got " + argumentCount);
                }
                final ComponentType varargType = ((ReferenceType) parameterTypes[varargIndex]).getComponentType();
                values = ReflectionUtil.compactVarargs(varargType, varargIndex, values);
            } else {
                if (argumentCount != parameterCount) {
                    throw new UnsupportedOperationException("Wrong argument value count, expected " + parameterCount + " but got " + argumentCount);
                }
            }
            return values;
        }

        private static boolean isNarrowerParameter(Type parameterA, Type parameterB, boolean primitiveArgument) {
            // if A is primitive
            //   if B is primitive
            //     A < B
            //   else
            //     argument is primitive
            // else
            //   if B is primitive
            //     argument is not primitive
            //   else
            //     A < B
            if (parameterA.isPrimitive()) {
                return parameterB.isPrimitive() ? parameterA.convertibleTo(parameterB) : primitiveArgument;
            }
            return parameterB.isPrimitive() ? !primitiveArgument : parameterA.convertibleTo(parameterB);
        }

        private static Type[] getParameterTypes(java.lang.reflect.Type[] genericParameterTypes, Substitutions substitutions) {
            // Get the parameter types by applying declaror and type argument substitutions
            final Type[] parameterTypes = TypeUtil.wrap(genericParameterTypes);
            for (int i = 0; i < parameterTypes.length; i++) {
                final Type parameterType = parameterTypes[i];
                if (parameterType instanceof TypeArgument) {
                    parameterTypes[i] = ((TypeArgument) parameterType).substituteTypeVariables(substitutions);
                }
            }
            return parameterTypes;
        }
    }

    class ConstructorCallable extends CommonCallable {
        private final Constructor<?> constructor;

        private ConstructorCallable(Constructor<?> constructor, LiteralReferenceType declaror, TypeVariable[] typeParameters, Substitutions substitutions) {
            super(declaror, typeParameters, declaror.capture(), constructor.getGenericParameterTypes(), substitutions);
            this.constructor = constructor;
        }

        private ConstructorCallable(Constructor<?> constructor, LiteralReferenceType declaror, TypeVariable[] typeParameters, Type[] parameterTypes, Type returnType, boolean varargEnabled) {
            super(declaror, typeParameters, parameterTypes, returnType, varargEnabled);
            this.constructor = constructor;
        }

        @Override
        public boolean isStatic() {
            return Modifier.isStatic(constructor.getModifiers());
        }

        @Override
        public boolean supportsVararg() {
            return constructor.isVarArgs();
        }

        @Override
        public ConstructorCallable useVararg() {
            if (!supportsVararg()) {
                throw new UnsupportedOperationException("This constructor does not support vararg");
            }
            return new ConstructorCallable(constructor, getDeclaringType(), getTypeParameters(), getParameterTypes(), getReturnType(), true);
        }

        @Override
        public Value call(Value target, Value... arguments) {
            final Object[] values = unwrapArguments(arguments);
            try {
                return getReturnType().getKind().wrap(constructor.newInstance(values));
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }

        @Override
        public String toString() {
            return constructor.toString();
        }

        public static ConstructorCallable of(LiteralReferenceType declaror, Constructor<?> constructor, TypeArgument[] typeArguments) {
            // Make sure the constructor actually belongs to the declaror
            if (constructor.getDeclaringClass() != declaror.getTypeClass()) {
                throw new IllegalArgumentException("The given constructor " + constructor + " was not declared by class " + declaror);
            }
            // Get the substitutions for the declaring type
            final Substitutions substitutions = declaror.capture().getSubstitutions();
            // Get the type parameters of the constructor
            final java.lang.reflect.TypeVariable<? extends Constructor<?>>[] typeParameters = constructor.getTypeParameters();
            // Check the validity of the type arguments and get the combined substitutions from the declaror and type arguments
            if (typeParameters.length == 0) {
                // It's allowed to call a non-parametrized constructor with type arguments
                return new ConstructorCallable(constructor, declaror, new TypeVariable[0], substitutions);
            }
            // Check the type arguments
            final TypeArgumentChecker checker = new TypeArgumentChecker(typeArguments, substitutions, typeParameters);
            if (!checker.check()) {
                return null;
            }
            // Create an return the constructor callable
            return new ConstructorCallable(constructor, declaror, checker.getTypeParameters(), checker.getSubstitutions());
        }
    }

    class MethodCallable extends CommonCallable {
        private final Method method;

        private MethodCallable(Method method, LiteralReferenceType declaror, TypeVariable[] typeParameters, Substitutions substitutions) {
            super(declaror, typeParameters, getReturnType(method, substitutions), method.getGenericParameterTypes(), substitutions);
            this.method = method;
        }

        private MethodCallable(Method method, LiteralReferenceType declaror, TypeVariable[] typeParameters, Type[] parameterTypes, Type returnType, boolean varargEnabled) {
            super(declaror, typeParameters, parameterTypes, returnType, varargEnabled);
            this.method = method;
        }

        @Override
        public boolean isStatic() {
            return Modifier.isStatic(method.getModifiers());
        }

        @Override
        public boolean supportsVararg() {
            return method.isVarArgs();
        }

        @Override
        public MethodCallable useVararg() {
            if (!supportsVararg()) {
                throw new UnsupportedOperationException("This method does not support vararg");
            }
            return new MethodCallable(method, getDeclaringType(), getTypeParameters(), getParameterTypes(), getReturnType(), true);
        }

        @Override
        public Value call(Value target, Value... arguments) {
            final Object value = target.asObject();
            final Object[] values = unwrapArguments(arguments);
            try {
                final Type returnType = getReturnType();
                if (returnType.isVoid()) {
                    method.invoke(value, values);
                    return VoidValue.THE_VOID;
                }
                return returnType.getKind().wrap(method.invoke(value, values));
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }

        @Override
        public String toString() {
            return method.toString();
        }

        private static Type getReturnType(Method method, Substitutions substitutions) {
            // Get the return type by applying declaror and type argument substitutions
            Type type = TypeUtil.wrap(method.getGenericReturnType());
            if (type instanceof TypeArgument) {
                type = ((TypeArgument) type).substituteTypeVariables(substitutions);
            }
            return type.capture();
        }

        public static MethodCallable of(LiteralReferenceType declaror, Method method, TypeArgument[] typeArguments) {
            // Make sure the method actually belongs to the declaror
            if (method.getDeclaringClass() != declaror.getTypeClass()) {
                throw new IllegalArgumentException("The given method " + method + " was not declared by class " + declaror);
            }
            // Get the substitutions for the declaring type
            final Substitutions substitutions = declaror.capture().getSubstitutions();
            // Get the type parameters of the method
            final java.lang.reflect.TypeVariable<Method>[] typeParameters = method.getTypeParameters();
            // Check the validity of the type arguments and get the combined substitutions from the declaror and type arguments
            if (typeParameters.length == 0) {
                // It's allowed to call a non-parametrized method with type arguments
                return new MethodCallable(method, declaror, new TypeVariable[0], substitutions);
            }
            // Check the type arguments
            final TypeArgumentChecker checker = new TypeArgumentChecker(typeArguments, substitutions, typeParameters);
            if (!checker.check()) {
                return null;
            }
            // Create an return the method callable
            return new MethodCallable(method, declaror, checker.getTypeParameters(), checker.getSubstitutions());
        }
    }

    class ArrayCloneCallable implements Callable {
        private final LiteralReferenceType declaror;

        private ArrayCloneCallable(LiteralReferenceType declaror) {
            this.declaror = declaror;
        }

        @Override
        public LiteralReferenceType getDeclaringType() {
            return declaror;
        }

        @Override
        public TypeVariable[] getTypeParameters() {
            return new TypeVariable[0];
        }

        @Override
        public Type[] getParameterTypes() {
            return new Type[0];
        }

        @Override
        public Type getReturnType() {
            return declaror;
        }

        @Override
        public boolean isStatic() {
            return false;
        }

        @Override
        public boolean supportsVararg() {
            return false;
        }

        @Override
        public boolean isVarargEnabled() {
            return false;
        }

        @Override
        public ArrayCloneCallable useVararg() {
            throw new UnsupportedOperationException("Array constructor clone method does not support vararg");
        }

        @Override
        public boolean isApplicable(Type[] argumentTypes) {
            return argumentTypes.length == 0;
        }

        @Override
        public boolean isMoreApplicableThan(Callable other, Type[] argumentTypes) {
            return other.getParameterTypes().length > 0;
        }

        @Override
        public Value call(Value target, Value... arguments) {
            try {
                return ObjectValue.of(ReflectionUtil.cloneArray(target.asObject()));
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }

        @Override
        public String toString() {
            return "public " + declaror.getName() + "[] clone()";
        }

        public static ArrayCloneCallable of(LiteralReferenceType declaror) {
            if (!declaror.isArray()) {
                throw new IllegalArgumentException("Cannot declaror an array cloning method on the non-array type " + declaror);
            }
            return new ArrayCloneCallable(declaror);
        }
    }

    class TypeArgumentChecker {
        private TypeArgument[] typeArguments;
        private Substitutions substitutions;
        private java.lang.reflect.TypeVariable<?>[] typeParameters;
        private TypeVariable[] signatureParameters;
        private Substitutions combinedSubstitutions;
        private boolean valid;
        private boolean checked = false;

        public TypeArgumentChecker(TypeArgument[] typeArguments, Substitutions substitutions, java.lang.reflect.TypeVariable<?>[] typeParameters) {
            this.typeArguments = typeArguments;
            this.substitutions = substitutions;
            this.typeParameters = typeParameters;
        }

        public TypeVariable[] getTypeParameters() {
            return signatureParameters;
        }

        public Substitutions getSubstitutions() {
            return combinedSubstitutions;
        }

        public boolean check() {
            if (checked) {
                return valid;
            }
            checked = true;
            // Check for length match
            if (typeParameters.length != typeArguments.length) {
                return valid = false;
            }
            // Wrap the parameters
            signatureParameters = new TypeVariable[typeParameters.length];
            for (int i = 0; i < typeParameters.length; i++) {
                signatureParameters[i] = (TypeVariable) TypeUtil.wrap(typeParameters[i]);
            }
            // Generate the combined substitution of the declaring class and method parameters, following the shadowing rules
            final Map<String, TypeArgument> namesToArguments = new HashMap<>();
            // First add all declaring class arguments from the original substitutions
            namesToArguments.putAll(substitutions.getMap());
            // Now add the method ones, replacing the declaring class arguments (shadowing)
            namesToArguments.putAll(Substitutions.toSubstitutionMap(signatureParameters, typeArguments));
            combinedSubstitutions = new Substitutions(namesToArguments);
            // Apply the substitutions to the method parameter bounds to get the final method signature parameters
            for (int i = 0; i < signatureParameters.length; i++) {
                signatureParameters[i] = signatureParameters[i].substituteBoundTypeVariables(combinedSubstitutions);
            }
            // Check if arguments are within bounds
            for (int i = 0; i < signatureParameters.length; i++) {
                if (!signatureParameters[i].boundsContain(typeArguments[i])) {
                    return valid = false;
                }
            }
            return valid = true;
        }
    }
}
