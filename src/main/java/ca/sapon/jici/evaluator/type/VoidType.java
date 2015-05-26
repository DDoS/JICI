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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import ca.sapon.jici.evaluator.value.ValueKind;

/**
 *
 */
public class VoidType implements Type {
    public static final VoidType THE_VOID = new VoidType();

    private VoidType() {
    }

    @Override
    public Class<?> getTypeClass() {
        return void.class;
    }

    @Override
    public String getName() {
        return "void";
    }

    @Override
    public ValueKind getKind() {
        return ValueKind.VOID;
    }

    @Override
    public boolean is(Type type) {
        return type.isVoid();
    }

    @Override
    public boolean isVoid() {
        return true;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public boolean isNumeric() {
        return false;
    }

    @Override
    public boolean isIntegral() {
        return false;
    }

    @Override
    public boolean isBoolean() {
        return false;
    }

    @Override
    public boolean isArray() {
        return false;
    }

    @Override
    public boolean isObject() {
        return false;
    }

    @Override
    public Type unbox() {
        return this;
    }

    @Override
    public ObjectType box() {
        return ObjectType.of(Void.class);
    }

    @Override
    public boolean canNarrowFrom(int value) {
        return false;
    }

    @Override
    public PrimitiveType unaryWiden() {
        throw new IllegalArgumentException("Cannot unary widen the void type");
    }

    @Override
    public PrimitiveType binaryWiden(Type with) {
        throw new IllegalArgumentException("Cannot unary widen the void type");
    }

    @Override
    public boolean convertibleTo(Type to) {
        throw new IllegalArgumentException("Cannot convert the void type");
    }

    @Override
    public Constructor<?> getConstructor(Type[] arguments) {
        throw new IllegalArgumentException("Cannot dereference void");
    }

    @Override
    public Constructor<?> getVarargConstructor(Type[] arguments) {
        return getConstructor(arguments);
    }

    @Override
    public Field getField(String name) {
        throw new IllegalArgumentException("Cannot dereference void");
    }

    @Override
    public Method getMethod(String name, Type[] arguments) {
        throw new IllegalArgumentException("Cannot dereference void");
    }

    @Override
    public Method getVarargMethod(String name, Type[] arguments) {
        return getMethod(name, arguments);
    }

    @Override
    public String toString() {
        return getName();
    }
}
