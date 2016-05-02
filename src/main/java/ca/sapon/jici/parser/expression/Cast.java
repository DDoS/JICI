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
package ca.sapon.jici.parser.expression;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.evaluator.type.LiteralReferenceType;
import ca.sapon.jici.evaluator.type.LiteralType;
import ca.sapon.jici.evaluator.type.PrimitiveType;
import ca.sapon.jici.evaluator.type.ReferenceType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.parser.expression.reference.VariableAccess;
import ca.sapon.jici.parser.name.TypeName;
import ca.sapon.jici.util.TypeUtil;

public class Cast implements Expression {
    private final TypeName typeName;
    private final Expression object;
    private int start;
    private int end;
    private LiteralType type = null;

    public Cast(TypeName typeName, Expression object, int start) {
        this.typeName = typeName;
        this.object = object;
        this.start = start;
        end = object.getEnd();
    }

    @Override
    public Type getType(Environment environment) {
        if (type != null) {
            return type;
        }
        final LiteralType castType = typeName.getType(environment);
        Type objectType = object.getType(environment);
        // Must capture variables subject to casting conversion
        if (object instanceof VariableAccess) {
            objectType = objectType.capture();
        }
        // Check the validity of the cast
        if (objectType.isVoid() || castType.isVoid()) {
            failCast(castType, objectType);
        }
        // apply boxing or unboxing if possible to have
        // a pair primitive-primitive or object-object
        if (castType.isPrimitive()) {
            if (objectType instanceof LiteralReferenceType) {
                objectType = ((LiteralReferenceType) objectType).tryUnbox();
            }
        } else if (objectType instanceof PrimitiveType) {
            objectType = ((PrimitiveType) objectType).box();
        }
        // cast primitive to primitive
        // cast boolean to boolean
        // more complicated rules for reference types
        if (castType.isPrimitive()) {
            if (!objectType.isPrimitive()) {
                failCast(castType, objectType);
            }
            if (castType.isBoolean() ^ objectType.isBoolean()) {
                failCast(castType, objectType);
            }
        } else if (objectType.isPrimitive()) {
            failCast(castType, objectType);
        } else if (!TypeUtil.isValidReferenceCast((ReferenceType) objectType, (ReferenceType) castType)) {
            failCast(castType, objectType);
        }
        return type = castType.capture();
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
        return start;
    }

    @Override
    public int getEnd() {
        return end;
    }

    @Override
    public void setStart(int start) {
        this.start = start;
    }

    @Override
    public void setEnd(int end) {
        this.end = end;
    }

    @Override
    public String toString() {
        return "Cast((" + typeName + ") " + object + ")";
    }
}
