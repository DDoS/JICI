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

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.evaluator.member.Callable;
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
    private final boolean diamondOperator;
    private final List<Expression> arguments;
    private int start;
    private int end;
    private Callable callable = null;

    public ConstructorCall(ClassTypeName typeName, boolean diamondOperator, List<Expression> arguments, int start, int end) {
        this(typeName, Collections.<TypeArgumentName>emptyList(), diamondOperator, arguments, start, end);
    }

    public ConstructorCall(ClassTypeName typeName, List<TypeArgumentName> typeArguments, boolean diamondOperator, List<Expression> arguments, int start, int end) {
        if (diamondOperator && typeName.hasArguments()) {
            throw new IllegalArgumentException("Cannot use the diamond operator and have class type arguments at the same time");
        }
        this.typeName = typeName;
        this.typeArguments = typeArguments;
        this.diamondOperator = diamondOperator;
        this.arguments = arguments;
        this.start = start;
        this.end = end;
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
        if (callable != null) {
            return callable.getReturnType();
        }
        // Check the type to be instantiated
        final LiteralReferenceType type = checkInstantiationType(environment);
        // Check type arguments
        final TypeArgument[] typeArguments = checkTypeArguments(environment, this.typeArguments, diamondOperator);
        // Check argument types
        final Type[] argumentTypes = MethodCall.checkArguments(environment, arguments);
        // Get the constructor to call
        try {
            callable = type.getConstructor(typeArguments, argumentTypes);
        } catch (UnsupportedOperationException exception) {
            throw new EvaluatorException(exception.getMessage(), this);
        }
        return callable.getReturnType();
    }

    private LiteralReferenceType checkInstantiationType(Environment environment) {
        final LiteralReferenceType type = typeName.getType(environment);
        // The class should be top level or static, which means it is not a inner class
        if (!type.isInnerClassOf(null)) {
            throw new EvaluatorException("Cannot instantiate the inner class " + type + " outside its enclosing class", typeName);
        }
        // Now check that we can actually instantiate the type
        if (type.isEnum()) {
            throw new EvaluatorException("Cannot instantiate the enum class " + type, typeName);
        }
        if (type.isAbstract()) {
            throw new EvaluatorException("Cannot instantiate the abstract class " + type, typeName);
        }
        if (!type.isPublic()) {
            throw new EvaluatorException("Cannot access class " + type, typeName);
        }
        if (type instanceof ParametrizedType) {
            // Check that the diamond operator is not being used when type arguments are already given
            if (diamondOperator) {
                throw new EvaluatorException("Cannot use the diamond operator when type arguments are provided", typeName);
            }
            // Check that parametrized types don't have wildcards
            final List<TypeArgument> arguments = ((ParametrizedType) type).getArguments();
            for (int i = 0; i < arguments.size(); i++) {
                if (arguments.get(i) instanceof WildcardType) {
                    throw new EvaluatorException("Cannot use wildcards as type arguments in constructor calls", typeName.getArgument(i));
                }
            }
        } else if (diamondOperator && !type.isRaw()) {
            // Check that the diamond operator is being used on a generic type
            throw new EvaluatorException("Cannot use the diamond operator on the non-generic type " + type, typeName);
        }
        return type;
    }

    public static TypeArgument[] checkTypeArguments(Environment environment, List<TypeArgumentName> typeArgumentNames, boolean diamondOperator) {
        final int typeArgumentCount = typeArgumentNames.size();
        if (typeArgumentCount > 0 && diamondOperator) {
            throw new EvaluatorException("Cannot use constructor type arguments with the diamond operator",
                    typeArgumentNames.get(0).getStart(), typeArgumentNames.get(typeArgumentCount - 1).getEnd());
        }
        final TypeArgument[] typeArguments = new TypeArgument[typeArgumentCount];
        for (int i = 0; i < typeArgumentCount; i++) {
            final TypeArgument typeArgument = typeArgumentNames.get(i).getType(environment);
            if (typeArgument instanceof WildcardType) {
                throw new EvaluatorException("Cannot use wildcards as type arguments in constructor calls", typeArgumentNames.get(i));
            }
            typeArguments[i] = typeArgument;
        }
        return typeArguments;
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
        return "ConstructorCall(new " + (!typeArguments.isEmpty() ? '<' + StringUtil.toString(typeArguments, ", ") + '>' : "") +
                typeName + (diamondOperator ? "<>" : "") + "(" + StringUtil.toString(arguments, ", ") + ")" + ")";
    }
}
