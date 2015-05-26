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
package ca.sapon.jici.parser.expression.call;

import java.lang.reflect.Constructor;
import java.util.List;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.evaluator.value.ObjectValue;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.evaluator.value.type.ValueType;
import ca.sapon.jici.parser.expression.Expression;
import ca.sapon.jici.parser.statement.Statement;
import ca.sapon.jici.parser.type.ClassTypeName;
import ca.sapon.jici.util.ReflectionUtil;
import ca.sapon.jici.util.StringUtil;

public class ConstructorCall implements Statement, Expression {
    private final ClassTypeName type;
    private final List<Expression> arguments;
    private ValueType valueType = null;
    private Constructor<?> constructor = null;
    private int varargIndex = -1;
    private Class<?> varargType = null;

    public ConstructorCall(ClassTypeName type, List<Expression> arguments) {
        this.arguments = arguments;
        this.type = type;
    }

    @Override
    public void execute(Environment environment) {
        try {
            getValueType(environment);
            getValue(environment);
        } catch (EvaluatorException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new EvaluatorException(exception, this);
        }
    }

    @Override
    public ValueType getValueType(Environment environment) {
        if (valueType == null) {
            valueType = type.getValueType(environment);
            final int size = arguments.size();
            final ValueType[] argumentTypes = new ValueType[size];
            for (int i = 0; i < size; i++) {
                argumentTypes[i] = arguments.get(i).getValueType(environment);
            }
            try {
                constructor = valueType.getConstructor(argumentTypes);
            } catch (IllegalArgumentException ignored) {
                try {
                    constructor = valueType.getVarargConstructor(argumentTypes);
                    final Class<?>[] parameters = constructor.getParameterTypes();
                    varargIndex = parameters.length - 1;
                    varargType = parameters[varargIndex].getComponentType();
                } catch (IllegalArgumentException exception) {
                    throw new EvaluatorException(exception.getMessage(), this);
                }
            }
        }
        return valueType;
    }

    @Override
    public Value getValue(Environment environment) {
        final int size = arguments.size();
        Object[] values = new Object[size];
        for (int i = 0; i < size; i++) {
            values[i] = arguments.get(i).getValue(environment).asObject();
        }
        if (varargIndex >= 0) {
            values = ReflectionUtil.compactVarargs(varargType, varargIndex, values);
        }
        try {
            return ObjectValue.of(constructor.newInstance(values));
        } catch (Exception exception) {
            throw new EvaluatorException("Could not call constructor", exception, this);
        }
    }

    @Override
    public int getStart() {
        return type.getStart();
    }

    @Override
    public int getEnd() {
        return arguments.isEmpty() ? type.getEnd() : arguments.get(arguments.size() - 1).getEnd();
    }

    @Override
    public String toString() {
        return "ConstructorCall(new " + type + "(" + StringUtil.toString(arguments, ", ") + "))";
    }
}
