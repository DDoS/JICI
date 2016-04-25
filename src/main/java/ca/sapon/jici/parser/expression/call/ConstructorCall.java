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
import ca.sapon.jici.evaluator.type.LiteralReferenceType;
import ca.sapon.jici.evaluator.type.ParametrizedType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.type.TypeArgument;
import ca.sapon.jici.evaluator.type.WildcardType;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.parser.expression.Expression;
import ca.sapon.jici.parser.name.ClassTypeName;
import ca.sapon.jici.parser.name.TypeArgumentName;
import ca.sapon.jici.parser.statement.Statement;
import ca.sapon.jici.util.StringUtil;

public class ConstructorCall implements Statement, Expression {
    private final ClassTypeName typeName;
    private final List<TypeArgumentName> typeArguments;
    private final List<Expression> arguments;
    private Callable callable = null;

    public ConstructorCall(ClassTypeName typeName, List<Expression> arguments) {
        this(typeName, Collections.<TypeArgumentName>emptyList(), arguments);
    }

    public ConstructorCall(ClassTypeName typeName, List<TypeArgumentName> typeArguments, List<Expression> arguments) {
        this.arguments = arguments;
        this.typeArguments = typeArguments;
        this.typeName = typeName;
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
            final LiteralReferenceType type = typeName.getType(environment);
            // Check that parametrized types don't have wildcards
            if (type instanceof ParametrizedType) {
                for (TypeArgument argument : ((ParametrizedType) type).getArguments()) {
                    if (argument instanceof WildcardType) {
                        throw new EvaluatorException("Cannot use wildcards as type arguments in a constructor call", typeName);
                    }
                }
            }
            final int size = arguments.size();
            final Type[] argumentTypes = new Type[size];
            for (int i = 0; i < size; i++) {
                argumentTypes[i] = arguments.get(i).getType(environment);
            }
            try {
                callable = type.getConstructor(argumentTypes);
            } catch (UnsupportedOperationException exception) {
                throw new EvaluatorException(exception.getMessage(), this);
            }
        }
        return callable.getReturnType();
    }

    @Override
    public Value getValue(Environment environment) {
        final int size = arguments.size();
        final Value[] values = new Value[size];
        for (int i = 0; i < size; i++) {
            values[i] = arguments.get(i).getValue(environment);
        }
        try {
            return callable.call(null, values);
        } catch (Exception exception) {
            throw new EvaluatorException("Could not call constructor", exception, this);
        }
    }

    @Override
    public int getStart() {
        return typeName.getStart();
    }

    @Override
    public int getEnd() {
        return arguments.isEmpty() ? typeName.getEnd() : arguments.get(arguments.size() - 1).getEnd();
    }

    @Override
    public String toString() {
        return "ConstructorCall(new " + (!typeArguments.isEmpty() ? '<' + StringUtil.toString(typeArguments, ", ") + '>' : "") +
                typeName + "(" + StringUtil.toString(arguments, ", ") + "))";
    }
}
