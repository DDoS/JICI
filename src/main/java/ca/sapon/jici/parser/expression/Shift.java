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
package ca.sapon.jici.parser.expression;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.value.IntValue;
import ca.sapon.jici.evaluator.value.LongValue;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.evaluator.value.ValueKind;
import ca.sapon.jici.evaluator.value.type.ValueType;
import ca.sapon.jici.lexer.Symbol;

public class Shift implements Expression {
    private final Expression left;
    private final Expression right;
    private final Symbol operator;
    private ValueType valueType = null;
    private ValueType shiftType = null;

    public Shift(Expression left, Expression right, Symbol operator) {
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    @Override
    public ValueType geValueType(Environment environment) {
        if (valueType == null) {
            final ValueType leftType = left.geValueType(environment).unbox();
            final ValueType rightType = right.geValueType(environment).unbox();
            if (!leftType.isIntegral()) {
                throw new IllegalArgumentException("Not an integral type: " + leftType.getName());
            }
            if (!rightType.isIntegral()) {
                throw new IllegalArgumentException("Not an integral type: " + rightType.getName());
            }
            valueType = leftType.unaryWiden();
            shiftType = rightType.unaryWiden();
        }
        return valueType;
    }

    @Override
    public Value getValue(Environment environment) {
        final Value leftValue = left.getValue(environment);
        final Value rightValue = right.getValue(environment);
        final ValueKind leftWidenKind = valueType.getKind();
        final ValueKind rightWidenKind = shiftType.getKind();
        switch (operator.getID()) {
            case SYMBOL_LOGICAL_LEFT_SHIFT:
                return doLogicalLeftShift(leftValue, leftWidenKind, rightValue, rightWidenKind);
            case SYMBOL_LOGICAL_RIGHT_SHIFT:
                return doLogicalRightShift(leftValue, leftWidenKind, rightValue, rightWidenKind);
            case SYMBOL_ARITHMETIC_RIGHT_SHIFT:
                return doArithmeticRightShift(leftValue, leftWidenKind, rightValue, rightWidenKind);
            default:
                throw new IllegalArgumentException("Invalid operator for shift: " + operator);
        }
    }

    private Value doLogicalLeftShift(Value leftValue, ValueKind leftWidenKind, Value rightValue, ValueKind rightWidenKind) {
        switch (leftWidenKind) {
            case INT:
                switch (rightWidenKind) {
                    case INT:
                        return IntValue.of(leftValue.asInt() << rightValue.asInt());
                    case LONG:
                        return IntValue.of(leftValue.asInt() << rightValue.asLong());
                    default:
                        throw new IllegalArgumentException("Invalid type for logical-left-shift right operand: " + rightWidenKind);
                }
            case LONG:
                switch (rightWidenKind) {
                    case INT:
                        return LongValue.of(leftValue.asLong() << rightValue.asInt());
                    case LONG:
                        return LongValue.of(leftValue.asLong() << rightValue.asLong());
                    default:
                        throw new IllegalArgumentException("Invalid type for logical-left-shift right operand: " + rightWidenKind);
                }
            default:
                throw new IllegalArgumentException("Invalid type for logical-left-shift left operand: " + leftWidenKind);
        }
    }

    private Value doLogicalRightShift(Value leftValue, ValueKind leftWidenKind, Value rightValue, ValueKind rightWidenKind) {
        switch (leftWidenKind) {
            case INT:
                switch (rightWidenKind) {
                    case INT:
                        return IntValue.of(leftValue.asInt() >>> rightValue.asInt());
                    case LONG:
                        return IntValue.of(leftValue.asInt() >>> rightValue.asLong());
                    default:
                        throw new IllegalArgumentException("Invalid type for logical-right-shift right operand: " + rightWidenKind);
                }
            case LONG:
                switch (rightWidenKind) {
                    case INT:
                        return LongValue.of(leftValue.asLong() >>> rightValue.asInt());
                    case LONG:
                        return LongValue.of(leftValue.asLong() >>> rightValue.asLong());
                    default:
                        throw new IllegalArgumentException("Invalid type for logical-right-shift right operand: " + rightWidenKind);
                }
            default:
                throw new IllegalArgumentException("Invalid type for logical-right-shift left operand: " + leftWidenKind);
        }
    }

    private Value doArithmeticRightShift(Value leftValue, ValueKind leftWidenKind, Value rightValue, ValueKind rightWidenKind) {
        switch (leftWidenKind) {
            case INT:
                switch (rightWidenKind) {
                    case INT:
                        return IntValue.of(leftValue.asInt() >> rightValue.asInt());
                    case LONG:
                        return IntValue.of(leftValue.asInt() >> rightValue.asLong());
                    default:
                        throw new IllegalArgumentException("Invalid type for arithmetic-right-shift right operand: " + rightWidenKind);
                }
            case LONG:
                switch (rightWidenKind) {
                    case INT:
                        return LongValue.of(leftValue.asLong() >> rightValue.asInt());
                    case LONG:
                        return LongValue.of(leftValue.asLong() >> rightValue.asLong());
                    default:
                        throw new IllegalArgumentException("Invalid type for arithmetic-right-shift right operand: " + rightWidenKind);
                }
            default:
                throw new IllegalArgumentException("Invalid type for arithmetic-right-shift left operand: " + leftWidenKind);
        }
    }

    @Override
    public String toString() {
        return "Shift(" + left + " " + operator + " " + right + ")";
    }
}
