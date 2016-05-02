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
package ca.sapon.jici.evaluator.type;

import java.util.Set;

import ca.sapon.jici.evaluator.member.Callable;
import ca.sapon.jici.evaluator.member.ClassVariable;
import ca.sapon.jici.evaluator.member.ConstructorCallable;

/**
 * A type backed by a class other than the primitive ones. Includes the null type.
 */
public interface ReferenceType extends Type {
    ComponentType getComponentType();

    LiteralReferenceType getInnerClass(String name, TypeArgument[] typeArguments);

    ClassVariable getField(String name);

    ConstructorCallable getConstructor(TypeArgument[] typeArguments, Type[] arguments);

    Callable getMethod(String name, TypeArgument[] typeArguments, Type[] arguments);

    Set<LiteralReferenceType> getDirectSuperTypes();

    Set<LiteralReferenceType> getSuperTypes();

    ReferenceType getErasure();

    boolean isUncheckedConversion(Type to);

    @Override
    ReferenceType capture();
}
