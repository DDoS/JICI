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
import ca.sapon.jici.evaluator.value.BooleanValue;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.evaluator.value.ValueKind;
import ca.sapon.jici.lexer.Symbol;
import ca.sapon.jici.parser.expression.Expression;
import ca.sapon.jici.util.ReflectionUtil;

public class Comparison implements Expression {
    private final Expression left;
    private final Expression right;
    private final Symbol operator;
    private Class<?> typeClass = null;
    private Value value = null;

    public Comparison(Expression left, Expression right, Symbol operator) {
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    @Override
    public Class<?> getTypeClass(Environment environment, Class<?> upperObjectBound) {
        if (typeClass == null) {
            final Class<?> leftClass = left.getTypeClass(environment, null);
            final Class<?> rightClass = right.getTypeClass(environment, null);
            if (!ReflectionUtil.isNumeric(leftClass)) {
                throw new IllegalArgumentException("Not a numeric type: " + leftClass.getCanonicalName());
            }
            if (!ReflectionUtil.isNumeric(rightClass)) {
                throw new IllegalArgumentException("Not a numeric type: " + rightClass.getCanonicalName());
            }
            typeClass = ReflectionUtil.binaryWiden(ReflectionUtil.unbox(leftClass), ReflectionUtil.unbox(rightClass));
        }
        return typeClass;
    }

    @Override
    public Value getValue(Environment environment) {
        if (value == null) {
            final Value leftValue = left.getValue(environment);
            final Value rightValue = right.getValue(environment);
            final ValueKind widenKind = ValueKind.binaryWidensTo(leftValue.getKind(), rightValue.getKind());
            switch (operator.getID()) {
                case SYMBOL_EQUAL:
                    value = doEqual(leftValue, rightValue, widenKind);
                    break;
                case SYMBOL_NOT_EQUAL:
                    value = doNotEqual(leftValue, rightValue, widenKind);
                    break;
                case SYMBOL_LESSER:
                    value = doLesserThan(leftValue, rightValue, widenKind);
                    break;
                case SYMBOL_LESSER_OR_EQUAL:
                    value = doLesserOrEqualTo(leftValue, rightValue, widenKind);
                    break;
                case SYMBOL_GREATER:
                    value = doGreaterThan(leftValue, rightValue, widenKind);
                    break;
                case SYMBOL_GREATER_OR_EQUAL:
                    value = doGreaterOrEqualTo(leftValue, rightValue, widenKind);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid operator for comparison: " + operator);
            }
        }
        return value;
    }

    private Value doEqual(Value leftValue, Value rightValue, ValueKind widenKind) {
        switch (widenKind) {
            case INT:
                return BooleanValue.of(leftValue.asInt() == rightValue.asInt());
            case LONG:
                return BooleanValue.of(leftValue.asLong() == rightValue.asLong());
            case FLOAT:
                return BooleanValue.of(leftValue.asFloat() == rightValue.asFloat());
            case DOUBLE:
                return BooleanValue.of(leftValue.asDouble() == rightValue.asDouble());
            default:
                throw new IllegalArgumentException("Invalid type for equal: " + widenKind);
        }
    }

    private Value doNotEqual(Value leftValue, Value rightValue, ValueKind widenKind) {
        switch (widenKind) {
            case INT:
                return BooleanValue.of(leftValue.asInt() != rightValue.asInt());
            case LONG:
                return BooleanValue.of(leftValue.asLong() != rightValue.asLong());
            case FLOAT:
                return BooleanValue.of(leftValue.asFloat() != rightValue.asFloat());
            case DOUBLE:
                return BooleanValue.of(leftValue.asDouble() != rightValue.asDouble());
            default:
                throw new IllegalArgumentException("Invalid type for not equal: " + widenKind);
        }
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
                throw new IllegalArgumentException("Invalid type for less than: " + widenKind);
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
                throw new IllegalArgumentException("Invalid type for lesser or equal to: " + widenKind);
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
                throw new IllegalArgumentException("Invalid type for greater than: " + widenKind);
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
                throw new IllegalArgumentException("Invalid type for greater or equal to: " + widenKind);
        }
    }

    @Override
    public String toString() {
        return "Comparison(" + left + " " + operator + " " + right + ")";
    }
}
