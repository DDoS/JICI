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
package ca.sapon.jici.parser.expression.logic;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.evaluator.type.PrimitiveType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.value.BooleanValue;
import ca.sapon.jici.evaluator.value.IntValue;
import ca.sapon.jici.evaluator.value.LongValue;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.evaluator.value.ValueKind;
import ca.sapon.jici.lexer.Symbol;
import ca.sapon.jici.parser.expression.Expression;
import ca.sapon.jici.util.TypeUtil;

public class BitwiseLogic implements Expression {
    private final Expression left;
    private final Expression right;
    private final Symbol operator;
    private int start;
    private int end;
    private Type type = null;

    public BitwiseLogic(Expression left, Expression right, Symbol operator) {
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
            if (leftType.isBoolean()) {
                if (rightType.isBoolean()) {
                    type = PrimitiveType.THE_BOOLEAN;
                } else {
                    throw new EvaluatorException("Not a boolean type: " + rightType.getName(), right);
                }
            } else if (leftType.isIntegral()) {
                if (rightType.isIntegral()) {
                    type = leftType.binaryWiden(rightType);
                } else {
                    throw new EvaluatorException("Not an integral type: " + rightType.getName(), right);
                }
            } else {
                throw new EvaluatorException("Not a boolean or integral type: " + leftType.getName(), left);
            }
        }
        return type;
    }

    @Override
    public Value getValue(Environment environment) {
        final Value leftValue = left.getValue(environment);
        final Value rightValue = right.getValue(environment);
        final ValueKind widenKind = type.getKind();
        switch (operator.getID()) {
            case SYMBOL_BITWISE_AND:
                return doBitwiseAND(leftValue, rightValue, widenKind);
            case SYMBOL_BITWISE_XOR:
                return doBitwiseXOR(leftValue, rightValue, widenKind);
            case SYMBOL_BITWISE_OR:
                return doBitwiseOR(leftValue, rightValue, widenKind);
            default:
                throw new EvaluatorException("Invalid operator for bitwise logic: " + operator, operator);
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

    private Value doBitwiseAND(Value leftValue, Value rightValue, ValueKind widenKind) {
        switch (widenKind) {
            case BOOLEAN:
                return BooleanValue.of(leftValue.asBoolean() & rightValue.asBoolean());
            case INT:
                return IntValue.of(leftValue.asInt() & rightValue.asInt());
            case LONG:
                return LongValue.of(leftValue.asLong() & rightValue.asLong());
            default:
                throw new EvaluatorException("Invalid type for bitwise AND: " + widenKind, this);
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
                throw new EvaluatorException("Invalid type for bitwise XOR: " + widenKind, this);
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
                throw new EvaluatorException("Invalid type for bitwise OR: " + widenKind, this);
        }
    }

    @Override
    public String toString() {
        return "BitwiseLogic(" + left + " " + operator + " " + right + ")";
    }
}
