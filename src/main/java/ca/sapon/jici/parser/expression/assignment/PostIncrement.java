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
package ca.sapon.jici.parser.expression.assignment;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.evaluator.value.type.ValueType;
import ca.sapon.jici.lexer.Symbol;
import ca.sapon.jici.lexer.literal.number.IntLiteral;
import ca.sapon.jici.parser.expression.Expression;
import ca.sapon.jici.parser.expression.arithmetic.Arithmetic;
import ca.sapon.jici.parser.expression.reference.Reference;
import ca.sapon.jici.parser.statement.Statement;

public class PostIncrement implements Expression, Statement {
    private static final IntLiteral ONE = new IntLiteral("1", 0) {
        @Override
        public int asInt() {
            return 1;
        }
    };
    protected final Reference inner;
    protected final Expression increment;
    protected final Symbol operator;
    protected ValueType valueType = null;

    public PostIncrement(Reference inner, Symbol operator) {
        this.inner = inner;
        this.operator = operator;
        increment = new Arithmetic(inner, ONE, operator.getCompoundAssignOperator());
    }

    @Override
    public void execute(Environment environment) {
        try {
            getValueType(environment);
            getValue(environment);
        } catch (Exception exception) {
            if (exception instanceof EvaluatorException) {
                throw exception;
            }
            throw new EvaluatorException(exception, this);
        }
    }

    @Override
    public ValueType getValueType(Environment environment) {
        if (valueType == null) {
            increment.getValueType(environment);
            final ValueType innerType = inner.getValueType(environment).unbox();
            if (!innerType.isNumeric()) {
                throw new EvaluatorException("Not a numeric type: " + innerType.getName(), inner);
            }
            valueType = innerType;
        }
        return valueType;
    }

    @Override
    public Value getValue(Environment environment) {
        final Value value = inner.getValue(environment);
        final Value result = increment.getValue(environment);
        inner.setValue(environment, valueType.getKind().convert(result));
        return value;
    }

    @Override
    public int getStart() {
        return inner.getStart();
    }

    @Override
    public int getEnd() {
        return operator.getEnd();
    }

    @Override
    public String toString() {
        return "PostIncrement(" + inner.toString() + operator.toString() + ")";
    }
}
