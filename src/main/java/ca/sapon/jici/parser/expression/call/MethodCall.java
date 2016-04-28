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

import ca.sapon.jici.evaluator.member.Callable;
import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.evaluator.type.ReferenceType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.type.TypeArgument;
import ca.sapon.jici.evaluator.type.WildcardType;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.lexer.Identifier;
import ca.sapon.jici.parser.expression.Expression;
import ca.sapon.jici.parser.expression.reference.AmbiguousReference;
import ca.sapon.jici.parser.expression.reference.VariableAccess;
import ca.sapon.jici.parser.name.TypeArgumentName;
import ca.sapon.jici.parser.statement.Statement;
import ca.sapon.jici.util.StringUtil;

public class MethodCall implements Expression, Statement {
    private final Expression object;
    private final Identifier method;
    private final List<TypeArgumentName> typeArguments;
    private final List<Expression> arguments;
    private Callable callable = null;

    public MethodCall(Expression object, Identifier method, List<Expression> arguments) {
        this(object, method, Collections.<TypeArgumentName>emptyList(), arguments);
    }

    public MethodCall(Expression object, Identifier method, List<TypeArgumentName> typeArguments, List<Expression> arguments) {
        this.object = object;
        this.method = method;
        this.typeArguments = typeArguments;
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
            if (!(objectType instanceof ReferenceType)) {
                throw new EvaluatorException("Not a class type " + objectType.getName(), object);
            }
            // Check type arguments
            final int typeArgumentCount = typeArguments.size();
            final TypeArgument[] typeArguments = new TypeArgument[typeArgumentCount];
            for (int i = 0; i < typeArgumentCount; i++) {
                final TypeArgument typeArgument = this.typeArguments.get(i).getType(environment);
                if (typeArgument instanceof WildcardType) {
                    throw new EvaluatorException("Cannot use wildcards as type arguments in method calls", this.typeArguments.get(i));
                }
                typeArguments[i] = typeArgument;
            }
            // Check argument types
            final int argumentCount = arguments.size();
            final Type[] argumentTypes = new Type[argumentCount];
            for (int i = 0; i < argumentCount; i++) {
                Type argumentType = arguments.get(i).getType(environment);
                // Must capture variables subject to invocation conversion
                if (arguments.get(i) instanceof VariableAccess) {
                    argumentType = argumentType.capture();
                }
                argumentTypes[i] = argumentType;
            }
            // Get method to call
            try {
                callable = ((ReferenceType) objectType).getMethod(method.getSource(), typeArguments, argumentTypes);
            } catch (UnsupportedOperationException exception) {
                throw new EvaluatorException(exception.getMessage(), method);
            }
            if (!callable.isStatic() && object instanceof AmbiguousReference.StaticAccess) {
                throw new EvaluatorException("Cannot access a non-static member directly from the type name", this);
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
        return "MethodCall(" + object + "." + (!typeArguments.isEmpty() ? '<' + StringUtil.toString(typeArguments, ", ") + '>' : "")
                + method + "(" + StringUtil.toString(arguments, ", ") + "))";
    }
}
