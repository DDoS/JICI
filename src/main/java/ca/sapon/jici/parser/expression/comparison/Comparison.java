/*
 * This file is part of JICI, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015-2016 Aleksi Sapon <http://sapon.ca/jici/>
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
package ca.sapon.jici.parser.expression.comparison;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.evaluator.type.PrimitiveType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.value.BooleanValue;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.evaluator.value.ValueKind;
import ca.sapon.jici.lexer.Symbol;
import ca.sapon.jici.parser.expression.Expression;
import ca.sapon.jici.util.TypeUtil;

public class Comparison implements Expression {
    private final Expression left;
    private final Expression right;
    private final Symbol operator;
    private int start;
    private int end;
    private Type type = null;
    private Type widenType = null;

    public Comparison(Expression left, Expression right, Symbol operator) {
        this.left = left;
        this.right = right;
        this.operator = operator;
        this.start = left.getStart();
        this.end = right.getEnd();
    }

    @Override
    public Type getType(Environment environment) {
        if (type == null) {
            final PrimitiveType leftType = TypeUtil.coerceToPrimitive(environment, left);
            final PrimitiveType rightType = TypeUtil.coerceToPrimitive(environment, right);
            if (!leftType.isNumeric()) {
                throw new EvaluatorException("Not a numeric type: " + leftType.getName(), left);
            }
            if (!rightType.isNumeric()) {
                throw new EvaluatorException("Not a numeric type: " + rightType.getName(), right);
            }
            widenType = leftType.binaryWiden(rightType);
            type = PrimitiveType.THE_BOOLEAN;
        }
        return type;
    }

    @Override
    public Value getValue(Environment environment) {
        final Value leftValue = left.getValue(environment);
        final Value rightValue = right.getValue(environment);
        final ValueKind widenKind = widenType.getKind();
        switch (operator.getID()) {
            case SYMBOL_LESSER:
                return doLesserThan(leftValue, rightValue, widenKind);
            case SYMBOL_LESSER_OR_EQUAL:
                return doLesserOrEqualTo(leftValue, rightValue, widenKind);
            case SYMBOL_GREATER:
                return doGreaterThan(leftValue, rightValue, widenKind);
            case SYMBOL_GREATER_OR_EQUAL:
                return doGreaterOrEqualTo(leftValue, rightValue, widenKind);
            default:
                throw new EvaluatorException("Invalid operator for comparison: " + operator, operator);
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

    private Value doLesserThan(Value leftValue, Value rightValue, ValueKind widenKind) {
        switch (widenKind) {
            case INT:
                return BooleanValue.of(leftValue.asInt() < rightValue.asInt());
            case LONG:
                return BooleanValue.of(leftValue.asLong() < rightValue.asLong());
            case FLOAT:
                return BooleanValue.of(leftValue.asFloat() < rightValue.asFloat());
            case DOUBLE:
                return BooleanValue.of(leftValue.asDouble() < rightValue.asDouble());
            default:
                throw new EvaluatorException("Invalid type for less than: " + widenKind, this);
        }
    }

    private Value doLesserOrEqualTo(Value leftValue, Value rightValue, ValueKind widenKind) {
        switch (widenKind) {
            case INT:
                return BooleanValue.of(leftValue.asInt() <= rightValue.asInt());
            case LONG:
                return BooleanValue.of(leftValue.asLong() <= rightValue.asLong());
            case FLOAT:
                return BooleanValue.of(leftValue.asFloat() <= rightValue.asFloat());
            case DOUBLE:
                return BooleanValue.of(leftValue.asDouble() <= rightValue.asDouble());
            default:
                throw new EvaluatorException("Invalid type for lesser or equal to: " + widenKind, this);
        }
    }

    private Value doGreaterThan(Value leftValue, Value rightValue, ValueKind widenKind) {
        switch (widenKind) {
            case INT:
                return BooleanValue.of(leftValue.asInt() > rightValue.asInt());
            case LONG:
                return BooleanValue.of(leftValue.asLong() > rightValue.asLong());
            case FLOAT:
                return BooleanValue.of(leftValue.asFloat() > rightValue.asFloat());
            case DOUBLE:
                return BooleanValue.of(leftValue.asDouble() > rightValue.asDouble());
            default:
                throw new EvaluatorException("Invalid type for greater than: " + widenKind, this);
        }
    }

    private Value doGreaterOrEqualTo(Value leftValue, Value rightValue, ValueKind widenKind) {
        switch (widenKind) {
            case INT:
                return BooleanValue.of(leftValue.asInt() >= rightValue.asInt());
            case LONG:
                return BooleanValue.of(leftValue.asLong() >= rightValue.asLong());
            case FLOAT:
                return BooleanValue.of(leftValue.asFloat() >= rightValue.asFloat());
            case DOUBLE:
                return BooleanValue.of(leftValue.asDouble() >= rightValue.asDouble());
            default:
                throw new EvaluatorException("Invalid type for greater or equal to: " + widenKind, this);
        }
    }

    @Override
    public String toString() {
        return "Comparison(" + left + " " + operator + " " + right + ")";
    }
}
