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
import ca.sapon.jici.evaluator.value.DoubleValue;
import ca.sapon.jici.evaluator.value.FloatValue;
import ca.sapon.jici.evaluator.value.IntValue;
import ca.sapon.jici.evaluator.value.LongValue;
import ca.sapon.jici.evaluator.value.ObjectValue;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.evaluator.value.ValueKind;
import ca.sapon.jici.evaluator.value.type.ObjectValueType;
import ca.sapon.jici.evaluator.value.type.ValueType;
import ca.sapon.jici.lexer.Symbol;
import ca.sapon.jici.lexer.TokenID;
import ca.sapon.jici.parser.expression.Expression;

public class Arithmetic implements Expression {
    private final Expression left;
    private final Expression right;
    private final Symbol operator;
    private boolean stringConcatenation = false;
    private ValueType valueType = null;

    public Arithmetic(Expression left, Expression right, Symbol operator) {
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    @Override
    public ValueType getValueType(Environment environment) {
        if (valueType == null) {
            final ValueType leftType = left.getValueType(environment).unbox();
            final ValueType rightType = right.getValueType(environment).unbox();
            if (operator.getID() == TokenID.SYMBOL_PLUS && (leftType.is(ObjectValueType.THE_STRING) || rightType.is(ObjectValueType.THE_STRING))) {
                valueType = ObjectValueType.THE_STRING;
                stringConcatenation = true;
            } else {
                if (!leftType.isNumeric()) {
                    throw new EvaluatorException("Not a numeric type: " + leftType.getName(), left);
                }
                if (!rightType.isNumeric()) {
                    throw new EvaluatorException("Not a numeric type: " + rightType.getName(), right);
                }
                valueType = leftType.binaryWiden(rightType);
            }
        }
        return valueType;
    }

    @Override
    public Value getValue(Environment environment) {
        final Value leftValue = left.getValue(environment);
        final Value rightValue = right.getValue(environment);
        if (stringConcatenation) {
            return doStringConcatenation(leftValue, rightValue);
        }
        final ValueKind widenKind = valueType.getKind();
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
