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

import java.util.Collections;
import java.util.List;

import ca.sapon.jici.evaluator.Callable;
import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.evaluator.type.ClassType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.lexer.Identifier;
import ca.sapon.jici.parser.expression.Expression;
import ca.sapon.jici.parser.name.TypeParameterName;
import ca.sapon.jici.parser.statement.Statement;
import ca.sapon.jici.util.StringUtil;

public class MethodCall implements Expression, Statement {
    private final Expression object;
    private final Identifier method;
    private final List<TypeParameterName> typeParameters;
    private final List<Expression> arguments;
    private Callable callable = null;

    public MethodCall(Expression object, Identifier method, List<Expression> arguments) {
        this(object, method, Collections.<TypeParameterName>emptyList(), arguments);
    }

    public MethodCall(Expression object, Identifier method, List<TypeParameterName> typeParameters, List<Expression> arguments) {
        this.object = object;
        this.method = method;
        this.typeParameters = typeParameters;
        this.arguments = arguments;
    }

    @Override
    public void execute(Environment environment) {
        try {
            getType(environment);
            getValue(environment);
        } catch (EvaluatorException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new EvaluatorException(exception, this);
        }
    }

    @Override
    public Type getType(Environment environment) {
        if (callable == null) {
            final Type objectType = object.getType(environment);
            if (!(objectType instanceof ClassType)) {
                throw new EvaluatorException("Not a class type " + objectType.getName(), object);
            }
            final int size = arguments.size();
            final Type[] argumentTypes = new Type[size];
            for (int i = 0; i < size; i++) {
                argumentTypes[i] = arguments.get(i).getType(environment);
            }
            try {
                callable = ((ClassType) objectType).getMethod(method.getSource(), argumentTypes);
            } catch (UnsupportedOperationException exception) {
                throw new EvaluatorException(exception.getMessage(), method);
            }
        }
        return callable.getReturnType();
    }

    @Override
    public Value getValue(Environment environment) {
        final Value value = object.getValue(environment);
        final int size = arguments.size();
        final Value[] values = new Value[size];
        for (int i = 0; i < size; i++) {
            values[i] = arguments.get(i).getValue(environment);
        }
        try {
            return callable.call(value, values);
        } catch (Exception exception) {
            throw new EvaluatorException("Could not call method", exception, method);
        }
    }

    @Override
    public int getStart() {
        return object.getStart();
    }

    @Override
    public int getEnd() {
        return arguments.isEmpty() ? method.getEnd() : arguments.get(arguments.size() - 1).getEnd();
    }

    @Override
    public String toString() {
        return "MethodCall(" + object + "." + (!typeParameters.isEmpty() ? '<' + StringUtil.toString(typeParameters, ", ") + '>' : "")
                + method + "(" + StringUtil.toString(arguments, ", ") + "))";
    }
}
