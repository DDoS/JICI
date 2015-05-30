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
package ca.sapon.jici.parser.expression.reference;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.evaluator.type.PrimitiveType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.value.IntValue;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.lexer.Identifier;
import ca.sapon.jici.parser.expression.Expression;
import ca.sapon.jici.util.ReflectionUtil;

public class FieldAccess implements Reference {
    private final Expression object;
    private final Identifier field;
    private Type type = null;
    private Field member = null;
    private boolean arrayLength = false;

    public FieldAccess(Expression object, Identifier field) {
        this.object = object;
        this.field = field;
    }

    @Override
    public Type getType(Environment environment) {
        if (type == null) {
            final Type objectType = object.getType(environment);
            final String name = field.getSource();
            if (objectType.isArray() && "length".equals(name)) {
                arrayLength = true;
                type = PrimitiveType.THE_INT;
            } else {
                try {
                    member = objectType.getField(name);
                } catch (UnsupportedOperationException exception) {
                    throw new EvaluatorException(exception.getMessage(), this);
                }
                type = ReflectionUtil.wrap(member.getType());
            }
        }
        return type;
    }

    @Override
    public Value getValue(Environment environment) {
        final Object objectValue = object.getValue(environment).asObject();
        if (arrayLength) {
            try {
                return IntValue.of(Array.getLength(objectValue));
            } catch (Exception exception) {
                throw new EvaluatorException("Could not access array length field", exception, field);
            }
        }
        try {
            return type.getKind().wrap(member.get(objectValue));
        } catch (Exception exception) {
            throw new EvaluatorException("Could not access field", exception, field);
        }
    }

    @Override
    public void setValue(Environment environment, Value value) {
        if (arrayLength) {
            throw new EvaluatorException("Array length field is read-only", field);
        }
        final Object objectValue = object.getValue(environment).asObject();
        try {
            member.set(objectValue, value.asObject());
        } catch (Exception exception) {
            throw new EvaluatorException("Could not access field", exception, field);
        }
    }

    @Override
    public int getStart() {
        return object.getStart();
    }

    @Override
    public int getEnd() {
        return field.getEnd();
    }

    @Override
    public String toString() {
        return "FieldAccess(" + object + "." + field + ")";
    }
}
