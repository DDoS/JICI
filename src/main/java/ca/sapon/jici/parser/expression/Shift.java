/**
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

import ca.sapon.jici.evaluator.IntValue;
import ca.sapon.jici.evaluator.LongValue;
import ca.sapon.jici.evaluator.Value;
import ca.sapon.jici.evaluator.ValueKind;
import ca.sapon.jici.lexer.Symbol;

public class Shift implements Expression {
    private final Expression left;
    private final Expression right;
    private final Symbol operator;
    private Value value = null;

    public Shift(Expression left, Expression right, Symbol operator) {
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    @Override
    public Value getValue() {
        if (value == null) {
            final Value leftValue = left.getValue();
            final Value rightValue = right.getValue();
            final ValueKind leftWidenKind = ValueKind.unaryWidensTo(leftValue.getKind());
            final ValueKind rightWidenKind = ValueKind.unaryWidensTo(rightValue.getKind());
            switch (operator.getID()) {
                case SYMBOL_LOGICAL_LEFT_SHIFT:
                    value = doLogicalLeftShift(leftValue, leftWidenKind, rightValue, rightWidenKind);
                    break;
                case SYMBOL_LOGICAL_RIGHT_SHIFT:
                    value = doLogicalRightShift(leftValue, leftWidenKind, rightValue, rightWidenKind);
                    break;
                case SYMBOL_ARITHMETIC_RIGHT_SHIFT:
                    value = doArithmeticRightShift(leftValue, leftWidenKind, rightValue, rightWidenKind);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid operator for shift: " + operator);
            }
        }
        return value;
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
