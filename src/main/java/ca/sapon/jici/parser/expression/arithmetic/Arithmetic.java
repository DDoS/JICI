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
package ca.sapon.jici.parser.expression.arithmetic;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.evaluator.type.PrimitiveType;
import ca.sapon.jici.evaluator.type.SingleReferenceType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.value.DoubleValue;
import ca.sapon.jici.evaluator.value.FloatValue;
import ca.sapon.jici.evaluator.value.IntValue;
import ca.sapon.jici.evaluator.value.LongValue;
import ca.sapon.jici.evaluator.value.ObjectValue;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.evaluator.value.ValueKind;
import ca.sapon.jici.lexer.Symbol;
import ca.sapon.jici.lexer.TokenID;
import ca.sapon.jici.parser.expression.Expression;
import ca.sapon.jici.util.TypeUtil;

public class Arithmetic implements Expression {
    private final Expression left;
    private final Expression right;
    private final Symbol operator;
    private boolean stringConcatenation = false;
    private Type leftType = null;
    private Type type = null;

    public Arithmetic(Expression left, Expression right, Symbol operator) {
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    @Override
    public Type getType(Environment environment) {
        if (type == null) {
            final Type leftType = left.getType(environment);
            final Type rightType = right.getType(environment);
            if (operator.getID() == TokenID.SYMBOL_PLUS && (leftType.equals(SingleReferenceType.THE_STRING) || rightType.equals(SingleReferenceType.THE_STRING))) {
                if (leftType.isVoid()) {
                    throw new EvaluatorException("Cannot convert void to java.lang.String", left);
                }
                if (rightType.isVoid()) {
                    throw new EvaluatorException("Cannot convert void to java.lang.String", right);
                }
                type = SingleReferenceType.THE_STRING;
                stringConcatenation = true;
                this.leftType = leftType;
            } else {
                final PrimitiveType leftPrimitiveType = TypeUtil.coerceToPrimitive(left, leftType);
                final PrimitiveType rightPrimitiveType = TypeUtil.coerceToPrimitive(right, rightType);
                if (!leftPrimitiveType.isNumeric()) {
                    throw new EvaluatorException("Not a numeric type: " + leftPrimitiveType.getName(), left);
                }
                if (!rightPrimitiveType.isNumeric()) {
                    throw new EvaluatorException("Not a numeric type: " + rightPrimitiveType.getName(), right);
                }
                type = leftPrimitiveType.binaryWiden(rightPrimitiveType);
                this.leftType = leftPrimitiveType;
            }
        }
        return type;
    }

    public Type getLeftType() {
        return leftType;
    }

    @Override
    public Value getValue(Environment environment) {
        final Value leftValue = left.getValue(environment);
        final Value rightValue = right.getValue(environment);
        if (stringConcatenation) {
            return doStringConcatenation(leftValue, rightValue);
        }
        final ValueKind widenKind = type.getKind();
        switch (operator.getID()) {
            case SYMBOL_PLUS:
                return doAdd(leftValue, rightValue, widenKind);
            case SYMBOL_MINUS:
                return doSubtract(leftValue, rightValue, widenKind);
            case SYMBOL_MULTIPLY:
                return doMultiply(leftValue, rightValue, widenKind);
            case SYMBOL_DIVIDE:
                return doDivide(leftValue, rightValue, widenKind);
            case SYMBOL_MODULO:
                return doModulo(leftValue, rightValue, widenKind);
            default:
                throw new EvaluatorException("Invalid operator for arithmetic: " + operator, operator);
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

    private Value doStringConcatenation(Value leftValue, Value rightValue) {
        return ObjectValue.of(leftValue.asString() + rightValue.asString());
    }

    private Value doAdd(Value leftValue, Value rightValue, ValueKind widenKind) {
        switch (widenKind) {
            case INT:
                return IntValue.of(leftValue.asInt() + rightValue.asInt());
            case LONG:
                return LongValue.of(leftValue.asLong() + rightValue.asLong());
            case FLOAT:
                return FloatValue.of(leftValue.asFloat() + rightValue.asFloat());
            case DOUBLE:
                return DoubleValue.of(leftValue.asDouble() + rightValue.asDouble());
            default:
                throw new EvaluatorException("Invalid type for add: " + widenKind, this);
        }
    }

    private Value doSubtract(Value leftValue, Value rightValue, ValueKind widenKind) {
        switch (widenKind) {
            case INT:
                return IntValue.of(leftValue.asInt() - rightValue.asInt());
            case LONG:
                return LongValue.of(leftValue.asLong() - rightValue.asLong());
            case FLOAT:
                return FloatValue.of(leftValue.asFloat() - rightValue.asFloat());
            case DOUBLE:
                return DoubleValue.of(leftValue.asDouble() - rightValue.asDouble());
            default:
                throw new EvaluatorException("Invalid type for subtract: " + widenKind, this);
        }
    }

    private Value doMultiply(Value leftValue, Value rightValue, ValueKind widenKind) {
        switch (widenKind) {
            case INT:
                return IntValue.of(leftValue.asInt() * rightValue.asInt());
            case LONG:
                return LongValue.of(leftValue.asLong() * rightValue.asLong());
            case FLOAT:
                return FloatValue.of(leftValue.asFloat() * rightValue.asFloat());
            case DOUBLE:
                return DoubleValue.of(leftValue.asDouble() * rightValue.asDouble());
            default:
                throw new EvaluatorException("Invalid type for multiply: " + widenKind, this);
        }
    }

    private Value doDivide(Value leftValue, Value rightValue, ValueKind widenKind) {
        switch (widenKind) {
            case INT:
                return IntValue.of(leftValue.asInt() / rightValue.asInt());
            case LONG:
                return LongValue.of(leftValue.asLong() / rightValue.asLong());
            case FLOAT:
                return FloatValue.of(leftValue.asFloat() / rightValue.asFloat());
            case DOUBLE:
                return DoubleValue.of(leftValue.asDouble() / rightValue.asDouble());
            default:
                throw new EvaluatorException("Invalid type for divide: " + widenKind, this);
        }
    }

    private Value doModulo(Value leftValue, Value rightValue, ValueKind widenKind) {
        switch (widenKind) {
            case INT:
                return IntValue.of(leftValue.asInt() % rightValue.asInt());
            case LONG:
                return LongValue.of(leftValue.asLong() % rightValue.asLong());
            case FLOAT:
                return FloatValue.of(leftValue.asFloat() % rightValue.asFloat());
            case DOUBLE:
                return DoubleValue.of(leftValue.asDouble() % rightValue.asDouble());
            default:
                throw new EvaluatorException("Invalid type for modulo: " + widenKind, this);
        }
    }

    @Override
    public String toString() {
        return "Arithmetic(" + left + " " + operator + " " + right + ")";
    }
}
