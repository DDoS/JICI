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
package ca.sapon.jici.parser.expression;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.evaluator.value.type.ValueType;
import ca.sapon.jici.parser.type.Type;

public class Cast implements Expression {
    private final Type type;
    private final Expression object;
    private ValueType valueType = null;

    public Cast(Type type, Expression object) {
        this.type = type;
        this.object = object;
    }

    @Override
    public ValueType geValueType(Environment environment) {
        if (valueType == null) {
            final ValueType castType = type.getValueType(environment);
            final ValueType objectType = object.geValueType(environment);
            // cast primitive to primitive
            // cast boolean to boolean
            // cannot cast to void
            // cast object to object
            // box to unbox or unbox to box
            if (castType.isPrimitive()) {
                if (!objectType.isPrimitive() && !objectType.unbox().is(castType.getTypeClass())) {
                    failCast(castType, objectType);
                }
                if (castType.isBoolean() ^ objectType.isBoolean()) {
                    failCast(castType, objectType);
                }
                if (castType.is(void.class)) {
                    failCast(castType, objectType);
                }
            } else if (objectType.isPrimitive()) {
                if (!castType.unbox().is(objectType.getTypeClass())) {
                    failCast(castType, objectType);
                }
            } else {
                // down or up casts
                final Class<?> object = objectType.getTypeClass();
                if (object != null) {
                    final Class<?> cast = castType.getTypeClass();
                    if (!cast.isAssignableFrom(object) && !object.isAssignableFrom(cast)) {
                        failCast(castType, objectType);
                    }
                }
            }
            valueType = castType;
        }
        return valueType;
    }

    private void failCast(ValueType cast, ValueType object) {
        failCast(cast, object.getTypeClass());
    }

    private void failCast(ValueType cast, Class<?> object) {
        throw new IllegalArgumentException("Cannot cast " + object.getCanonicalName() + " to " + cast.getName());
    }

    @Override
    public Value getValue(Environment environment) {
        final Value value = object.getValue(environment);
        switch (valueType.getKind()) {
            case OBJECT:
                final Object object = value.asObject();
                if (object != null && !valueType.getTypeClass().isInstance(object)) {
                    failCast(valueType, object.getClass());
                }
                return value;
            default:
                return valueType.getKind().convert(value);
        }
    }

    @Override
    public String toString() {
        return "Cast((" + type + ") " + object + ")";
    }
}
