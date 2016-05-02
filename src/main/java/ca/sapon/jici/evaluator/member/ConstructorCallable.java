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

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import ca.sapon.jici.evaluator.type.LiteralReferenceType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.type.TypeArgument;
import ca.sapon.jici.evaluator.type.TypeVariable;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.util.TypeUtil;

/**
 *
 */
public class ConstructorCallable extends DeclaredCallable {
    private final Constructor<?> constructor;

    private ConstructorCallable(Constructor<?> constructor, LiteralReferenceType declaror, TypeArgument[] typeArguments)
            throws IncompatibleTypeArgumentsException {
        super(declaror, TypeUtil.wrap(constructor.getTypeParameters()), declaror, TypeUtil.wrap(constructor.getGenericParameterTypes()),
                typeArguments, Modifier.isStatic(constructor.getModifiers()));
        this.constructor = constructor;
    }

    private ConstructorCallable(Constructor<?> constructor, LiteralReferenceType declaror, TypeVariable[] typeParameters, Type[] parameterTypes, Type returnType, boolean varargEnabled) {
        super(declaror, typeParameters, parameterTypes, returnType, Modifier.isStatic(constructor.getModifiers()), varargEnabled);
        this.constructor = constructor;
    }

    @Override
    public String getName() {
        return constructor.getName();
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
    public ConstructorCallable eraseReturnType() {
        return this;
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
        // Try to create a constructor callable, can fail because of wrong type arguments
        try {
            return new ConstructorCallable(constructor, declaror, typeArguments);
        } catch (IncompatibleTypeArgumentsException exception) {
            return null;
        }
    }
}
