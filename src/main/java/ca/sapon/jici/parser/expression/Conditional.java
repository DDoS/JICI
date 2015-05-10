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
import ca.sapon.jici.evaluator.value.ObjectValue;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.evaluator.value.ValueKind;
import ca.sapon.jici.evaluator.value.type.ObjectUnionValueType;
import ca.sapon.jici.evaluator.value.type.PrimitiveValueType;
import ca.sapon.jici.evaluator.value.type.ValueType;
import ca.sapon.jici.lexer.literal.number.IntLiteral;

public class Conditional implements Expression {
    private final Expression test;
    private final Expression left;
    private final Expression right;
    private ValueType valueType = null;
    private Value value = null;

    public Conditional(Expression test, Expression left, Expression right) {
        this.test = test;
        this.left = left;
        this.right = right;
    }

    @Override
    public ValueType geValueType(Environment environment) {
        if (valueType == null) {
            final ValueType testType = test.geValueType(environment).unbox();
            final ValueType leftType = left.geValueType(environment).unbox();
            final ValueType rightType = right.geValueType(environment).unbox();
            if (!testType.isBoolean()) {
                throw new IllegalArgumentException("Not a boolean: " + testType.getName());
            }
            if (leftType.is(rightType.getClassType())) {
                // both same type to that type
                valueType = leftType;
            } else if (leftType.isObject() || rightType.isObject()) {
                // for objects return a union
                valueType = new ObjectUnionValueType(leftType.box(),  rightType.box());
            } else if (left instanceof IntLiteral && rightType.canNarrowTo(((IntLiteral) left).asInt())) {
                // left constant numeric that narrows to right, use right
                valueType = rightType;
            } else if (right instanceof IntLiteral && leftType.canNarrowTo(((IntLiteral) right).asInt())) {
                // right constant numeric that narrows to left, use left
                valueType = leftType;
            } else if (leftType.is(byte.class) && rightType.is(short.class) || leftType.is(short.class) && rightType.is(byte.class)) {
                // one byte and other short to short
                valueType = PrimitiveValueType.of(short.class);
            } else {
                // else use binary widening
                valueType = leftType.binaryWiden(rightType.getClassType());
            }
        }
        return valueType;
    }

    @Override
    public Value getValue(Environment environment) {
        if (value == null) {
            final Value tesValue = test.getValue(environment);
            final Value leftValue = left.getValue(environment);
            final Value rightValue = right.getValue(environment);
            final ValueKind widenKind = valueType.getKind();
            value = doConditional(tesValue, leftValue, rightValue, widenKind);
        }
        return value;
    }

    private Value doConditional(Value testValue, Value leftValue, Value rightValue, ValueKind widenKind) {
        final Value resultValue = testValue.asBoolean() ? leftValue : rightValue;
        if (resultValue.getKind() == widenKind) {
            // prevent useless conversion
            return resultValue;
        }
        if (resultValue instanceof ObjectValue) {
            // prevent useless reboxing
            return resultValue;
        }
        return widenKind.convert(resultValue);
    }

    @Override
    public String toString() {
        return "Conditional(" + test + " ? " + left + " : " + right + ")";
    }
}
