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
package ca.sapon.jici.parser.expression.logic;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.evaluator.value.BooleanValue;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.evaluator.type.PrimitiveType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.lexer.Symbol;
import ca.sapon.jici.parser.expression.Expression;
import ca.sapon.jici.util.TypeUtil;

public class BooleanLogic implements Expression {
    private final Expression left;
    private final Expression right;
    private final Symbol operator;
    private Type type = null;

    public BooleanLogic(Expression left, Expression right, Symbol operator) {
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    @Override
    public Type getType(Environment environment) {
        if (type == null) {
            final PrimitiveType leftType = TypeUtil.coerceToPrimitive(environment, left);
            final PrimitiveType rightType = TypeUtil.coerceToPrimitive(environment, right);
            if (!leftType.isBoolean()) {
                throw new EvaluatorException("Not a boolean: " + leftType.getName(), left);
            }
            if (!rightType.isBoolean()) {
                throw new EvaluatorException("Not a boolean: " + rightType.getName(), right);
            }
            type = PrimitiveType.THE_BOOLEAN;
        }
        return type;
    }

    @Override
    public Value getValue(Environment environment) {
        final Value leftValue = left.getValue(environment);
        final Value rightValue = right.getValue(environment);
        switch (operator.getID()) {
            case SYMBOL_BOOLEAN_AND:
                return doBooleanAND(leftValue, rightValue);
            case SYMBOL_BOOLEAN_OR:
                return doBooleanOR(leftValue, rightValue);
            default:
                throw new EvaluatorException("Invalid operator for boolean logic: " + operator, operator);
        }
    }

    @Override
    public int getStart() {
        return left.getStart();
    }

    @Override
    public int getEnd() {
        return right.getEnd();
    }

    private Value doBooleanAND(Value leftValue, Value rightValue) {
        return BooleanValue.of(leftValue.asBoolean() && rightValue.asBoolean());
    }

    private Value doBooleanOR(Value leftValue, Value rightValue) {
        return BooleanValue.of(leftValue.asBoolean() || rightValue.asBoolean());
    }

    @Override
    public String toString() {
        return "BooleanLogic(" + left + " " + operator + " " + right + ")";
    }
}
