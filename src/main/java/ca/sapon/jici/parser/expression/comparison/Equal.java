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
import ca.sapon.jici.util.ReflectionUtil;

public class Equal implements Expression {
    private final Expression left;
    private final Expression right;
    private final Symbol operator;
    private ValueKind widenKind = null;

    public Equal(Expression left, Expression right, Symbol operator) {
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    @Override
    public Type getType(Environment environment) {
        if (widenKind == null) {
            final Type leftType = left.getType(environment);
            final Type rightType = right.getType(environment);
            if (leftType.isObject() && rightType.isObject()) {
                widenKind = ValueKind.OBJECT;
                return PrimitiveType.THE_BOOLEAN;
            }
            final PrimitiveType primitiveLeftType = ReflectionUtil.coerceToPrimitive(left, leftType);
            final PrimitiveType primitiveRightType = ReflectionUtil.coerceToPrimitive(right, rightType);
            if (primitiveLeftType.isBoolean() && primitiveRightType.isBoolean()) {
                widenKind = ValueKind.BOOLEAN;
                return PrimitiveType.THE_BOOLEAN;
            }
            if (primitiveLeftType.isNumeric() && primitiveRightType.isNumeric()) {
                widenKind = primitiveLeftType.binaryWiden(primitiveRightType).getKind();
                return PrimitiveType.THE_BOOLEAN;
            }
            throw new EvaluatorException("Invalid types for equality operation: " + leftType.getName() + " and " + rightType.getName(), this);
        }
        return PrimitiveType.THE_BOOLEAN;
    }

    @Override
    public Value getValue(Environment environment) {
        final Value leftValue = left.getValue(environment);
        final Value rightValue = right.getValue(environment);
        switch (operator.getID()) {
            case SYMBOL_EQUAL:
                return doEqual(leftValue, rightValue, widenKind);
            case SYMBOL_NOT_EQUAL:
                return doNotEqual(leftValue, rightValue, widenKind);
            default:
                throw new EvaluatorException("Invalid operator for equal: " + operator, operator);
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

    private Value doEqual(Value leftValue, Value rightValue, ValueKind widenKind) {
        switch (widenKind) {
            case BOOLEAN:
                return BooleanValue.of(leftValue.asBoolean() == rightValue.asBoolean());
            case INT:
                return BooleanValue.of(leftValue.asInt() == rightValue.asInt());
            case LONG:
                return BooleanValue.of(leftValue.asLong() == rightValue.asLong());
            case FLOAT:
                return BooleanValue.of(leftValue.asFloat() == rightValue.asFloat());
            case DOUBLE:
                return BooleanValue.of(leftValue.asDouble() == rightValue.asDouble());
            case OBJECT:
                return BooleanValue.of(leftValue.asObject() == rightValue.asObject());
            default:
                throw new EvaluatorException("Invalid type for equal: " + widenKind, this);
        }
    }

    private Value doNotEqual(Value leftValue, Value rightValue, ValueKind widenKind) {
        switch (widenKind) {
            case BOOLEAN:
                return BooleanValue.of(leftValue.asBoolean() != rightValue.asBoolean());
            case INT:
                return BooleanValue.of(leftValue.asInt() != rightValue.asInt());
            case LONG:
                return BooleanValue.of(leftValue.asLong() != rightValue.asLong());
            case FLOAT:
                return BooleanValue.of(leftValue.asFloat() != rightValue.asFloat());
            case DOUBLE:
                return BooleanValue.of(leftValue.asDouble() != rightValue.asDouble());
            case OBJECT:
                return BooleanValue.of(leftValue.asObject() != rightValue.asObject());
            default:
                throw new EvaluatorException("Invalid type for not equal: " + widenKind, this);
        }
    }

    @Override
    public String toString() {
        return "Equal(" + left + " " + operator + " " + right + ")";
    }
}
