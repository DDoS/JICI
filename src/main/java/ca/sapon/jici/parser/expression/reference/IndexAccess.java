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

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.evaluator.value.type.PrimitiveType;
import ca.sapon.jici.evaluator.value.type.Type;
import ca.sapon.jici.parser.expression.Expression;
import ca.sapon.jici.util.ReflectionUtil;

public class IndexAccess implements Reference {
    private final Expression object;
    private final Expression index;
    private Type type = null;

    public IndexAccess(Expression object, Expression index) {
        this.object = object;
        this.index = index;
    }

    @Override
    public Type getType(Environment environment) {
        if (type == null) {
            final Type objectType = object.getType(environment);
            final Type indexType = index.getType(environment);
            if (!objectType.isArray()) {
                throw new EvaluatorException("Not an array: " + objectType.getName(), object);
            }
            if (!indexType.convertibleTo(PrimitiveType.THE_INT)) {
                throw new EvaluatorException("Cannot convert " + indexType.getName() + " to int", index);
            }
            final Class<?> componentType = objectType.getTypeClass().getComponentType();
            type = ReflectionUtil.wrap(componentType);
        }
        return type;
    }

    @Override
    public Value getValue(Environment environment) {
        final Value objectValue = object.getValue(environment);
        final Value indexValue = index.getValue(environment);
        try {
            return type.getKind().wrap(Array.get(objectValue.asObject(), indexValue.asInt()));
        } catch (Exception exception) {
            throw new EvaluatorException("Could not get array item", exception, this);
        }
    }

    @Override
    public void setValue(Environment environment, Value value) {
        final Value objectValue = object.getValue(environment);
        final Value indexValue = index.getValue(environment);
        try {
            Array.set(objectValue.asObject(), indexValue.asInt(), value.asObject());
        } catch (Exception exception) {
            throw new EvaluatorException("Could not set array item", exception, this);
        }
    }

    @Override
    public int getStart() {
        return object.getStart();
    }

    @Override
    public int getEnd() {
        return index.getEnd();
    }

    @Override
    public String toString() {
        return "IndexAccess(" + object + "[" + index + "])";
    }
}
