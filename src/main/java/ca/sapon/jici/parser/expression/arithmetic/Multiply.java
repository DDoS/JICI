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
package ca.sapon.jici.parser.expression.arithmetic;

import ca.sapon.jici.evaluator.DoubleValue;
import ca.sapon.jici.evaluator.FloatValue;
import ca.sapon.jici.evaluator.IntValue;
import ca.sapon.jici.evaluator.LongValue;
import ca.sapon.jici.evaluator.Value;
import ca.sapon.jici.evaluator.ValueKind;
import ca.sapon.jici.lexer.Symbol;
import ca.sapon.jici.parser.expression.Expression;

public class Multiply implements Expression {
    private final Expression left;
    private final Expression right;
    private final Symbol operator;
    private Value value = null;

    public Multiply(Expression left, Expression right, Symbol operator) {
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    @Override
    public Value getValue() {
        if (value == null) {
            final Value leftValue = left.getValue();
            final Value rightValue = right.getValue();
            final ValueKind resultKind = ValueKind.binaryWidensTo(leftValue.getKind(), rightValue.getKind());
            switch (operator.getID()) {
                case SYMBOL_MULTIPLY:
                    value = doMultiply(leftValue, rightValue, resultKind);
                    break;
                case SYMBOL_DIVIDE:
                    value = doDivide(leftValue, rightValue, resultKind);
                    break;
                case SYMBOL_MODULO:
                    value = doModulo(leftValue, rightValue, resultKind);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid operator for multiply: " + operator);
            }
        }
        return value;
    }

    private Value doMultiply(Value leftValue, Value rightValue, ValueKind resultKind) {
        switch (resultKind) {
            case INT:
                return IntValue.of(leftValue.asInt() * rightValue.asInt());
            case LONG:
                return LongValue.of(leftValue.asLong() * rightValue.asLong());
            case FLOAT:
                return FloatValue.of(leftValue.asFloat() * rightValue.asFloat());
            case DOUBLE:
                return DoubleValue.of(leftValue.asDouble() * rightValue.asDouble());
            default:
                throw new IllegalArgumentException("Invalid result type for multiply, got " + resultKind);
        }
    }

    private Value doDivide(Value leftValue, Value rightValue, ValueKind resultKind) {
        switch (resultKind) {
            case INT:
                return IntValue.of(leftValue.asInt() / rightValue.asInt());
            case LONG:
                return LongValue.of(leftValue.asLong() / rightValue.asLong());
            case FLOAT:
                return FloatValue.of(leftValue.asFloat() / rightValue.asFloat());
            case DOUBLE:
                return DoubleValue.of(leftValue.asDouble() / rightValue.asDouble());
            default:
                throw new IllegalArgumentException("Invalid result type for divide, got " + resultKind);
        }
    }

    private Value doModulo(Value leftValue, Value rightValue, ValueKind resultKind) {
        switch (resultKind) {
            case INT:
                return IntValue.of(leftValue.asInt() % rightValue.asInt());
            case LONG:
                return LongValue.of(leftValue.asLong() % rightValue.asLong());
            case FLOAT:
                return FloatValue.of(leftValue.asFloat() % rightValue.asFloat());
            case DOUBLE:
                return DoubleValue.of(leftValue.asDouble() % rightValue.asDouble());
            default:
                throw new IllegalArgumentException("Invalid result type for modulo, got " + resultKind);
        }
    }

    @Override
    public String toString() {
        return "Multiply(" + left + " " + operator + " " + right + ")";
    }
}
