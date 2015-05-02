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
import ca.sapon.jici.lexer.literal.number.IntLiteral;

public class Conditional implements Expression {
    private final Expression test;
    private final Expression left;
    private final Expression right;
    private Value value = null;

    public Conditional(Expression test, Expression left, Expression right) {
        this.test = test;
        this.left = left;
        this.right = right;
    }

    @Override
    public Class<?> getTypeClass(Environment environment) {
        return null;
    }

    @Override
    public Value getValue(Environment environment) {
        if (value == null) {
            final Value tesValue = test.getValue(environment);
            final Value leftValue = left.getValue(environment);
            final Value rightValue = right.getValue(environment);
            final ValueKind leftKind = leftValue.getKind();
            final ValueKind rightKind = rightValue.getKind();
            final ValueKind widenKind;
            if (leftKind == rightKind) {
                // both same kind to that kind
                widenKind = leftKind;
            } else if (leftValue instanceof IntLiteral && ValueKind.canNarrowTo(rightKind, leftValue.asInt())) {
                // left constant numeric that narrows to right, use right
                widenKind = rightKind;
            } else if (rightValue instanceof IntLiteral && ValueKind.canNarrowTo(leftKind, rightValue.asInt())) {
                // right constant numeric that narrows to left, use left
                widenKind = leftKind;
            } else if ((1 << leftKind.ordinal() | 1 << rightKind.ordinal()) == 0b110) {
                // one BYTE and other SHORT to SHORT
                widenKind = ValueKind.SHORT;
            } else if (leftKind == ValueKind.OBJECT || rightKind == ValueKind.OBJECT) {
                // one OBJECT and a boxed type to OBJECT
                widenKind = ValueKind.OBJECT;
            } else {
                // else use binary widening
                widenKind = ValueKind.binaryWidensTo(leftKind, rightKind);
            }
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
