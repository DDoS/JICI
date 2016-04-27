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

import java.util.Set;

import ca.sapon.jici.evaluator.ClassVariable;
import ca.sapon.jici.evaluator.Callable;

/**
 * The null type, for the {@code null} literal.
 */
public class NullType extends SingleReferenceType {
    public static final NullType THE_NULL = new NullType();

    private NullType() {
    }

    @Override
    public String getName() {
        return "null";
    }

    @Override
    public boolean isArray() {
        return false;
    }

    @Override
    public boolean isReifiable() {
        return true;
    }

    @Override
    public boolean convertibleTo(Type to) {
        // Null can be converted to any reference type (so anything that isn't a primitive type)
        return !to.isPrimitive();
    }

    @Override
    public boolean isNull() {
        return true;
    }

    @Override
    public NullType getErasure() {
        return this;
    }

    @Override
    public NullType capture() {
        return this;
    }

    @Override
    public Set<SingleReferenceType> getDirectSuperTypes() {
        throw new UnsupportedOperationException("Can't list the direct super types of the null types as it is the universal set");
    }

    @Override
    public SingleReferenceType asArray(int dimensions) {
        throw new UnsupportedOperationException("Cannot have an array of the null type");
    }

    @Override
    public Object newArray(int length) {
        throw new UnsupportedOperationException("Cannot instantiate an array of the null type");
    }

    @Override
    public Object newArray(int[] lengths) {
        throw new UnsupportedOperationException("Cannot instantiate an array of the null type");
    }

    @Override
    public ComponentType getComponentType() {
        throw new UnsupportedOperationException("Not an array type");
    }

    @Override
    public Callable getConstructor(TypeArgument[] typeArguments, Type[] arguments) {
        throw new UnsupportedOperationException("Cannot dereference the null type");
    }

    @Override
    public ClassVariable getField(String name) {
        throw new UnsupportedOperationException("Cannot dereference the null type");
    }

    @Override
    public Callable getMethod(String name, TypeArgument[] typeArguments, Type[] arguments) {
        throw new UnsupportedOperationException("Cannot dereference the null type");
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof NullType;
    }

    @Override
    public int hashCode() {
        return 9849851;
    }
}
