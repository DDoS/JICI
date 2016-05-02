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
package ca.sapon.jici.evaluator.member;

import java.util.HashMap;
import java.util.Map;

import ca.sapon.jici.evaluator.Substitutions;
import ca.sapon.jici.evaluator.type.ComponentType;
import ca.sapon.jici.evaluator.type.LiteralReferenceType;
import ca.sapon.jici.evaluator.type.ReferenceType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.type.TypeArgument;
import ca.sapon.jici.evaluator.type.TypeVariable;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.util.ReflectionUtil;

/**
 * A common class for declared callables, such as methods and constructors. This excludes built-in like the clone() method for arrays.
 */
public abstract class DeclaredCallable implements Callable {
    private final LiteralReferenceType declaror;
    private final TypeVariable[] typeParameters;
    private final Type[] parameterTypes;
    private final Type returnType;
    private final boolean _static;
    private final boolean varargEnabled;

    protected DeclaredCallable(LiteralReferenceType declaror, TypeVariable[] typeParameters, Type returnType, Type[] parameterTypes, TypeArgument[] typeArguments, boolean _static)
            throws IncompatibleTypeArgumentsException {
        this.declaror = declaror;
        this._static = _static;
        // Raw types require the erasure of all type information on non-static members
        if (declaror.isRaw() && !_static) {
            if (typeArguments.length > 0) {
                throw new UnsupportedOperationException("Cannot pass type arguments to a raw type member");
            }
            this.typeParameters = new TypeVariable[0];
            this.returnType = eraseType(returnType);
            this.parameterTypes = eraseTypes(parameterTypes);
        } else {
            final TypeArgumentChecker checker = new TypeArgumentChecker(typeParameters, declaror, typeArguments);
            if (!checker.check()) {
                throw new IncompatibleTypeArgumentsException("Type arguments are not within type parameter bounds");
            }
            final Substitutions substitutions = checker.getSubstitutions();
            this.typeParameters = checker.getTypeParameters();
            this.returnType = substituteType(returnType, substitutions).capture();
            this.parameterTypes = substituteTypes(parameterTypes, substitutions);
        }
        varargEnabled = false;
    }

    protected DeclaredCallable(LiteralReferenceType declaror, TypeVariable[] typeParameters, Type[] parameterTypes, Type returnType, boolean _static, boolean varargEnabled) {
        this.declaror = declaror;
        this.typeParameters = typeParameters;
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
        this._static = _static;
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
    public boolean isStatic() {
        return _static;
    }

    @Override
    public boolean isVarargEnabled() {
        return varargEnabled;
    }

    @Override
    public abstract DeclaredCallable useVararg();

    @Override
    public boolean isApplicable(Type[] argumentTypes) {
        final Type[] expandedParameters = getExpandedParameters(argumentTypes);
        if (expandedParameters == null) {
            return false;
        }
        // Argument types must be convertible to the parameter types
        for (int i = 0; i < expandedParameters.length; i++) {
            if (!argumentTypes[i].convertibleTo(expandedParameters[i])) {
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

    @Override
    public boolean requiresUncheckedConversion(Type[] argumentTypes) {
        final Type[] expandedParameters = getExpandedParameters(argumentTypes);
        if (expandedParameters == null) {
            throw new IllegalArgumentException("Argument type count does not match the parameter count");
        }
        for (int i = 0; i < argumentTypes.length; i++) {
            final Type type = argumentTypes[i];
            if (type instanceof ReferenceType && ((ReferenceType) type).isUncheckedConversion(expandedParameters[i])) {
                return true;
            }
        }
        return false;
    }

    private Type[] getExpandedParameters(Type[] argumentTypes) {
        if (varargEnabled) {
            // When we use vararg, we can expand the last type parameter to have the required count
            if (argumentTypes.length < parameterTypes.length - 1) {
                return null;
            }
            return ReflectionUtil.expandsVarargs(parameterTypes, argumentTypes.length);
        }
        if (argumentTypes.length != parameterTypes.length) {
            return null;
        }
        return parameterTypes;
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

    private static Type[] substituteTypes(Type[] types, Substitutions substitutions) {
        for (int i = 0; i < types.length; i++) {
            final Type type = types[i];
            if (type instanceof TypeArgument) {
                types[i] = ((TypeArgument) type).substituteTypeVariables(substitutions);
            }
        }
        return types;
    }

    private static Type substituteType(Type type, Substitutions substitutions) {
        if (type instanceof TypeArgument) {
            return ((TypeArgument) type).substituteTypeVariables(substitutions);
        }
        return type;
    }

    private static Type eraseType(Type type) {
        if (type instanceof ReferenceType) {
            return ((ReferenceType) type).getErasure();
        }
        return type;
    }

    private static Type[] eraseTypes(Type[] types) {
        for (int i = 0; i < types.length; i++) {
            final Type type = types[i];
            if (type instanceof ReferenceType) {
                types[i] = ((ReferenceType) type).getErasure();
            }
        }
        return types;
    }

    public static class TypeArgumentChecker {
        private final TypeVariable[] typeParameters;
        private final LiteralReferenceType declaror;
        private final TypeArgument[] typeArguments;
        private Substitutions substitutions;
        private boolean valid;
        private boolean checked = false;

        public TypeArgumentChecker(TypeVariable[] typeParameters, LiteralReferenceType declaror, TypeArgument[] typeArguments) {
            this.typeParameters = typeParameters;
            this.declaror = declaror;
            this.typeArguments = typeArguments;
        }

        public TypeVariable[] getTypeParameters() {
            return typeParameters;
        }

        public Substitutions getSubstitutions() {
            return substitutions;
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
            // Generate the combined substitution of the declaring class and method parameters, following the shadowing rules
            final Map<String, TypeArgument> namesToArguments = new HashMap<>();
            // First add all declaring class captured arguments as the original substitutions
            namesToArguments.putAll(declaror.capture().getSubstitutions().getMap());
            // Now add the method ones, replacing the declaring class arguments (shadowing)
            namesToArguments.putAll(Substitutions.toSubstitutionMap(typeParameters, typeArguments));
            substitutions = new Substitutions(namesToArguments);
            // Apply the substitutions to the method parameter bounds to get the final method signature parameters
            for (int i = 0; i < typeParameters.length; i++) {
                typeParameters[i] = typeParameters[i].substituteBoundTypeVariables(substitutions);
            }
            // Check if arguments are within bounds
            for (int i = 0; i < typeParameters.length; i++) {
                if (!typeParameters[i].boundsContain(typeArguments[i])) {
                    return valid = false;
                }
            }
            return valid = true;
        }
    }

    public static class IncompatibleTypeArgumentsException extends Exception {
        private static final long serialVersionUID = 1;

        public IncompatibleTypeArgumentsException(String message) {
            super(message);
        }
    }
}
