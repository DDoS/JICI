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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.evaluator.value.VoidValue;
import ca.sapon.jici.evaluator.value.type.ValueType;
import ca.sapon.jici.lexer.Identifier;
import ca.sapon.jici.parser.expression.Expression;
import ca.sapon.jici.parser.statement.Statement;
import ca.sapon.jici.util.ReflectionUtil;
import ca.sapon.jici.util.StringUtil;

public class MethodCall implements Expression, Statement {
    private final Expression object;
    private final Identifier method;
    private final List<Expression> arguments;
    private ValueType valueType = null;
    private Method callable = null;

    public MethodCall(Expression object, Identifier method, List<Expression> arguments) {
        this.object = object;
        this.method = method;
        this.arguments = arguments;
    }

    @Override
    public void execute(Environment environment) {
        getValueType(environment);
        getValue(environment);
    }

    @Override
    public ValueType getValueType(Environment environment) {
        if (valueType == null) {
            final int size = arguments.size();
            final ValueType[] argumentTypes = new ValueType[size];
            for (int i = 0; i < size; i++) {
                argumentTypes[i] = arguments.get(i).getValueType(environment);
            }
            callable = object.getValueType(environment).getMethod(method.getSource(), argumentTypes);
            final Class<?> returnType = callable.getReturnType();
            valueType = ReflectionUtil.wrap(returnType);
        }
        return valueType;
    }

    @Override
    public Value getValue(Environment environment) {
        final Object value = object.getValue(environment).asObject();
        final int size = arguments.size();
        final Object[] values = new Object[size];
        for (int i = 0; i < size; i++) {
            values[i] = arguments.get(i).getValue(environment).asObject();
        }
        try {
            if (valueType.isVoid()) {
                callable.invoke(value, values);
                return VoidValue.THE_VOID;
            } else {
                return valueType.getKind().wrap(callable.invoke(value, values));
            }
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalArgumentException("Could not call method", exception);
        }
    }

    @Override
    public String toString() {
        return "MethodCall(" + object + "." + method + "(" + StringUtil.toString(arguments, ", ") + "))";
    }
}
