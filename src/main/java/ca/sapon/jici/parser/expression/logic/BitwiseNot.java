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
import ca.sapon.jici.evaluator.value.IntValue;
import ca.sapon.jici.evaluator.value.LongValue;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.evaluator.value.ValueKind;
import ca.sapon.jici.parser.expression.Expression;
import ca.sapon.jici.util.TypeUtil;

public class BitwiseNot implements Expression {
    private final Expression inner;
    private int start;
    private int end;
    private Type type = null;

    public BitwiseNot(Expression inner, int start) {
        this.inner = inner;
        this.start = start;
        end = inner.getEnd();
    }

    @Override
    public Type getType(Environment environment) {
        if (type == null) {
            final PrimitiveType innerType = TypeUtil.coerceToPrimitive(environment, inner);
            if (!innerType.isIntegral()) {
                throw new EvaluatorException("Not an integral type: " + innerType.getName(), inner);
            }
            type = innerType.unaryWiden();
        }
        return type;
    }

    @Override
    public Value getValue(Environment environment) {
        final Value innerValue = inner.getValue(environment);
        final ValueKind widenKind = type.getKind();
        switch (widenKind) {
            case INT:
                return IntValue.of(~innerValue.asInt());
            case LONG:
                return LongValue.of(~innerValue.asLong());
            default:
                throw new EvaluatorException("Invalid type for bitwise not: " + widenKind, this);
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

    @Override
    public String toString() {
        return "BitwiseNot(~" + inner + ")";
    }
}
