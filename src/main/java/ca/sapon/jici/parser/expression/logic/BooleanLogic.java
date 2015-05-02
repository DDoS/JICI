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
import ca.sapon.jici.evaluator.value.BooleanValue;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.lexer.Symbol;
import ca.sapon.jici.parser.expression.Expression;

public class BooleanLogic implements Expression {
    private final Expression left;
    private final Expression right;
    private final Symbol operator;
    private Value value = null;

    public BooleanLogic(Expression left, Expression right, Symbol operator) {
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    @Override
    public Class<?> getTypeClass(Environment environment) {
        return null;
    }

    @Override
    public Value getValue(Environment environment) {
        if (value == null) {
            final Value leftValue = left.getValue(environment);
            final Value rightValue = right.getValue(environment);
            switch (operator.getID()) {
                case SYMBOL_BOOLEAN_AND:
                    value = doBooleanAND(leftValue, rightValue);
                    break;
                case SYMBOL_BOOLEAN_OR:
                    value = doBooleanOR(leftValue, rightValue);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid operator for boolean logic: " + operator);
            }
        }
        return value;
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
