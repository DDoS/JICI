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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import ca.sapon.jici.evaluator.type.LiteralReferenceType;
import ca.sapon.jici.evaluator.type.ReferenceType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.type.TypeArgument;
import ca.sapon.jici.evaluator.type.TypeCache;
import ca.sapon.jici.evaluator.type.TypeVariable;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.evaluator.value.VoidValue;

/**
 *
 */
public class MethodCallable extends DeclaredCallable {
    private final Method method;

    private MethodCallable(Method method, LiteralReferenceType declaror, TypeArgument[] typeArguments)
            throws IncompatibleTypeArgumentsException {
        super(declaror, TypeCache.wrapTypeVariables(method.getTypeParameters()), TypeCache.wrapType(method.getGenericReturnType()), TypeCache.wrapTypes(method.getGenericParameterTypes()),
                typeArguments, Modifier.isStatic(method.getModifiers()));
        this.method = method;
    }

    private MethodCallable(Method method, LiteralReferenceType declaror, TypeVariable[] typeParameters, Type[] parameterTypes, Type returnType, boolean varargEnabled) {
        super(declaror, typeParameters, parameterTypes, returnType, Modifier.isStatic(method.getModifiers()), varargEnabled);
        this.method = method;
    }

    @Override
    public String getName() {
        return method.getName();
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
    public MethodCallable eraseReturnType() {
        Type returnType = getReturnType();
        if (returnType instanceof ReferenceType) {
            returnType = ((ReferenceType) returnType).getErasure();
        }
        return new MethodCallable(method, getDeclaringType(), getTypeParameters(), getParameterTypes(), returnType, isVarargEnabled());
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

    public static MethodCallable of(LiteralReferenceType declaror, Method method, TypeArgument[] typeArguments) {
        // Make sure the method actually belongs to the declaror
        if (method.getDeclaringClass() != declaror.getTypeClass()) {
            throw new IllegalArgumentException("The given method " + method + " was not declared by class " + declaror);
        }
        // Try to create a method callable, can fail because of wrong type arguments
        try {
            return new MethodCallable(method, declaror, typeArguments);
        } catch (IncompatibleTypeArgumentsException exception) {
            return null;
        }
    }
}
