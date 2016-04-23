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

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import ca.sapon.jici.evaluator.type.PrimitiveType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.type.TypeArgument;
import ca.sapon.jici.evaluator.value.IntValue;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.util.TypeUtil;

/**
 *
 */
public abstract class ClassVariable {
    protected final Type type;

    protected ClassVariable(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public abstract boolean isStatic();

    public abstract Value getValue(Value target);

    public abstract void setValue(Value target, Value value);

    public static ClassVariable forField(Substitutions substitutions, Field field) {
        return new InstanceVariable(substitutions, field);
    }

    public static ClassVariable forArrayLength() {
        return ArrayLengthField.INSTANCE;
    }

    private static class InstanceVariable extends ClassVariable {
        private final Field field;

        private InstanceVariable(Substitutions substitutions, Field field) {
            super(getType(substitutions, field));
            this.field = field;
        }

        @Override
        public boolean isStatic() {
            return Modifier.isStatic(field.getModifiers());
        }

        @Override
        public Value getValue(Value target) {
            try {
                return type.getKind().wrap(field.get(target.asObject()));
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }

        @Override
        public void setValue(Value target, Value value) {
            try {
                field.set(target.asObject(), value.asObject());
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }

        private static Type getType(Substitutions substitutions, Field field) {
            Type type = TypeUtil.wrap(field.getGenericType());
            if (type instanceof TypeArgument) {
                type = ((TypeArgument) type).substituteTypeVariables(substitutions);
            }
            return type;
        }
    }

    private static class ArrayLengthField extends ClassVariable {
        private static final ArrayLengthField INSTANCE = new ArrayLengthField();

        private ArrayLengthField() {
            super(PrimitiveType.THE_INT);
        }

        @Override
        public boolean isStatic() {
            return false;
        }

        @Override
        public Value getValue(Value target) {
            try {
                return IntValue.of(Array.getLength(target.asObject()));
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }

        @Override
        public void setValue(Value target, Value value) {
            throw new UnsupportedOperationException("Array length field is final");
        }
    }
}
