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

import java.lang.reflect.Field;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.evaluator.value.type.ObjectValueType;
import ca.sapon.jici.evaluator.value.type.PrimitiveValueType;
import ca.sapon.jici.evaluator.value.type.ValueType;
import ca.sapon.jici.lexer.Identifier;
import ca.sapon.jici.parser.expression.Expression;

public class FieldAccess implements Reference {
    private final Expression object;
    private final Identifier field;
    private ValueType valueType = null;
    private Field member = null;

    public FieldAccess(Expression object, Identifier field) {
        this.object = object;
        this.field = field;
    }

    @Override
    public ValueType geValueType(Environment environment) {
        if (valueType == null) {
            member = object.geValueType(environment).getField(field.getSource());
            final Class<?> type = member.getType();
            valueType = type.isPrimitive() ? PrimitiveValueType.of(type) : new ObjectValueType(type);
        }
        return valueType;
    }

    @Override
    public Value getValue(Environment environment) {
        try {
            return valueType.getKind().wrap(member.get(object.getValue(environment)));
        } catch (IllegalAccessException exception) {
            throw new IllegalArgumentException("Could not access field: " + field.getSource());
        }
    }

    @Override
    public void setValue(Environment environment, Value value) {
        try {
            member.set(object.getValue(environment), value.asObject());
        } catch (IllegalAccessException exception) {
            throw new IllegalArgumentException("Could not access field: " + field.getSource());
        }
    }

    @Override
    public String toString() {
        return "FieldAccess(" + object + "." + field + ")";
    }
}
