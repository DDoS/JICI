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
import ca.sapon.jici.evaluator.value.IntValue;
import ca.sapon.jici.evaluator.value.LongValue;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.evaluator.value.ValueKind;
import ca.sapon.jici.lexer.Symbol;
import ca.sapon.jici.parser.expression.Expression;
import ca.sapon.jici.util.ReflectionUtil;

public class BitwiseLogic implements Expression {
    private final Expression left;
    private final Expression right;
    private final Symbol operator;
    private Class<?> typeClass = null;
    private Value value = null;

    public BitwiseLogic(Expression left, Expression right, Symbol operator) {
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    @Override
    public Class<?> getTypeClass(Environment environment, Class<?> upperObjectBound) {
        if (typeClass == null) {
            final Class<?> leftClass = left.getTypeClass(environment, null);
            final Class<?> rightClass = right.getTypeClass(environment, null);
            if (!ReflectionUtil.isLogical(leftClass)) {
                throw new IllegalArgumentException("Not a boolean or numeric type: " + leftClass.getCanonicalName());
            }
            if (!ReflectionUtil.isLogical(rightClass)) {
                throw new IllegalArgumentException("Not a boolean or numeric type: " + rightClass.getCanonicalName());
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
                case SYMBOL_BITWISE_AND:
                    value = doBitwiseAND(leftValue, rightValue, widenKind);
                    break;
                case SYMBOL_BITWISE_XOR:
                    value = doBitwiseXOR(leftValue, rightValue, widenKind);
                    break;
                case SYMBOL_BITWISE_OR:
                    value = doBitwiseOR(leftValue, rightValue, widenKind);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid operator for bitwise logic: " + operator);
            }
        }
        return value;
    }

    private Value doBitwiseAND(Value leftValue, Value rightValue, ValueKind widenKind) {
        switch (widenKind) {
            case BOOLEAN:
                return BooleanValue.of(leftValue.asBoolean() & rightValue.asBoolean());
            case INT:
                return IntValue.of(leftValue.asInt() & rightValue.asInt());
            case LONG:
                return LongValue.of(leftValue.asLong() & rightValue.asLong());
            default:
                throw new IllegalArgumentException("Invalid type for bitwise AND: " + widenKind);
        }
    }

    private Value doBitwiseXOR(Value leftValue, Value rightValue, ValueKind widenKind) {
        switch (widenKind) {
            case BOOLEAN:
                return BooleanValue.of(leftValue.asBoolean() ^ rightValue.asBoolean());
            case INT:
                return IntValue.of(leftValue.asInt() ^ rightValue.asInt());
            case LONG:
                return LongValue.of(leftValue.asLong() ^ rightValue.asLong());
            default:
                throw new IllegalArgumentException("Invalid type for bitwise XOR: " + widenKind);
        }
    }

    private Value doBitwiseOR(Value leftValue, Value rightValue, ValueKind widenKind) {
        switch (widenKind) {
            case BOOLEAN:
                return BooleanValue.of(leftValue.asBoolean() | rightValue.asBoolean());
            case INT:
                return IntValue.of(leftValue.asInt() | rightValue.asInt());
            case LONG:
                return LongValue.of(leftValue.asLong() | rightValue.asLong());
            default:
                throw new IllegalArgumentException("Invalid type for bitwise OR: " + widenKind);
        }
    }

    @Override
    public String toString() {
        return "BitwiseLogic(" + left + " " + operator + " " + right + ")";
    }
}
