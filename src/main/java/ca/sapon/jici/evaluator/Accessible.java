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

import ca.sapon.jici.evaluator.type.ConcreteType;
import ca.sapon.jici.evaluator.type.PrimitiveType;
import ca.sapon.jici.evaluator.value.IntValue;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.util.ReflectionUtil;

/**
 *
 */
public abstract class Accessible {
    protected final ConcreteType type;

    protected Accessible(ConcreteType type) {
        this.type = type;
    }

    public ConcreteType getType() {
        return type;
    }

    public abstract Value getValue(Value target);

    public abstract void setValue(Value target, Value value);

    public static Accessible forField(Field field) {
        return new FieldAccessible(field);
    }

    public static Accessible forArrayLength() {
        return ArrayLengthAccess.INSTANCE;
    }

    private static class FieldAccessible extends Accessible {
        private final Field field;

        private FieldAccessible(Field field) {
            super(ReflectionUtil.wrap(field.getGenericType()));
            this.field = field;
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
    }

    private static class ArrayLengthAccess extends Accessible {
        private static final ArrayLengthAccess INSTANCE = new ArrayLengthAccess();

        private ArrayLengthAccess() {
            super(PrimitiveType.THE_INT);
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
