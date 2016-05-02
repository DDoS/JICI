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

import ca.sapon.jici.evaluator.type.LiteralReferenceType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.type.TypeVariable;
import ca.sapon.jici.evaluator.value.ObjectValue;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.util.ReflectionUtil;

/**
 *
 */
public class ArrayCloneCallable implements Callable {
    private final LiteralReferenceType declaror;

    private ArrayCloneCallable(LiteralReferenceType declaror) {
        this.declaror = declaror;
    }

    @Override
    public LiteralReferenceType getDeclaringType() {
        return declaror;
    }

    @Override
    public String getName() {
        return "clone";
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
    public boolean requiresUncheckedConversion(Type[] argumentTypes) {
        return false;
    }

    @Override
    public ArrayCloneCallable eraseReturnType() {
        return new ArrayCloneCallable(declaror.getErasure());
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
