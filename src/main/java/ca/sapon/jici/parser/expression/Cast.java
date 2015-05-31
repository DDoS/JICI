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
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.evaluator.type.ClassType;
import ca.sapon.jici.evaluator.type.ClassUnionType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.parser.name.TypeName;

public class Cast implements Expression {
    private final TypeName typeName;
    private final Expression object;
    private Type type = null;

    public Cast(TypeName typeName, Expression object) {
        this.typeName = typeName;
        this.object = object;
    }

    @Override
    public Type getType(Environment environment) {
        if (type == null) {
            final Type castType = typeName.getType(environment);
            Type objectType = object.getType(environment);
            if (objectType.isVoid()) {
                failCast(castType, objectType);
            }
            if (!objectType.isNull()) {
                objectType = castType.isPrimitive() ? objectType.unbox() : objectType.box();
            }
            // cast primitive to primitive
            // cast boolean to boolean
            // cast object or null to object
            if (castType.isPrimitive()) {
                if (!objectType.isPrimitive() && !objectType.is(castType)) {
                    failCast(castType, objectType);
                }
                if (castType.isBoolean() ^ objectType.isBoolean()) {
                    failCast(castType, objectType);
                }
            } else if (objectType.isPrimitive()) {
                if (!castType.is(objectType)) {
                    failCast(castType, objectType);
                }
            } else {
                // down or up casts
                if (objectType instanceof ClassUnionType) {
                    final Class<?> cast = castType.getTypeClass();
                    boolean oneValid = false;
                    for (ClassType bound : ((ClassUnionType) objectType).getLowestUpperBound()) {
                        final Class<?> object = bound.getTypeClass();
                        if (cast.isAssignableFrom(object) || object.isAssignableFrom(cast)) {
                            oneValid = true;
                            break;
                        }
                    }
                    if (!oneValid) {
                        failCast(castType, objectType);
                    }
                } else {
                    final Class<?> object = objectType.getTypeClass();
                    if (object != null) {
                        final Class<?> cast = castType.getTypeClass();
                        if (!cast.isAssignableFrom(object) && !object.isAssignableFrom(cast)) {
                            failCast(castType, objectType);
                        }
                    }
                }
            }
            type = castType;
        }
        return type;
    }

    private void failCast(Type cast, Type object) {
        failCast(cast.getName(), object.getName());
    }

    private void failCast(Type cast, Class<?> object) {
        failCast(cast.getName(), object.getCanonicalName());
    }

    private void failCast(String cast, String object) {
        throw new EvaluatorException("Cannot cast " + object + " to " + cast, this);
    }

    @Override
    public Value getValue(Environment environment) {
        final Value value = object.getValue(environment);
        switch (type.getKind()) {
            case OBJECT:
                final Object object = value.asObject();
                if (object != null && !type.getTypeClass().isInstance(object)) {
                    failCast(type, object.getClass());
                }
                return value;
            default:
                return type.getKind().convert(value);
        }
    }

    @Override
    public int getStart() {
        return typeName.getStart();
    }

    @Override
    public int getEnd() {
        return object.getEnd();
    }

    @Override
    public String toString() {
        return "Cast((" + typeName + ") " + object + ")";
    }
}
