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
package ca.sapon.jici.parser.expression.arithmetic;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.evaluator.type.PrimitiveType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.value.DoubleValue;
import ca.sapon.jici.evaluator.value.FloatValue;
import ca.sapon.jici.evaluator.value.IntValue;
import ca.sapon.jici.evaluator.value.LongValue;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.evaluator.value.ValueKind;
import ca.sapon.jici.lexer.Symbol;
import ca.sapon.jici.parser.expression.Expression;
import ca.sapon.jici.util.TypeUtil;

public class Sign implements Expression {
    private final Expression inner;
    private final Symbol operator;
    private Type type = null;

    public Sign(Expression inner, Symbol operator) {
        this.inner = inner;
        this.operator = operator;
    }

    @Override
    public Type getType(Environment environment) {
        if (type == null) {
            final PrimitiveType innerType = TypeUtil.coerceToPrimitive(environment, inner);
            if (!innerType.isNumeric()) {
                throw new EvaluatorException("Not a numeric type: " + innerType.getName(), inner);
            }
            type = innerType.unaryWiden();
        }
        return type;
    }

    @Override
    public Value getValue(Environment environment) {
        final Value innerValue = inner.getValue(environment);
        final ValueKind widenKind = type.getKind();
        switch (operator.getID()) {
            case SYMBOL_PLUS:
                return doReaffirm(innerValue, widenKind);
            case SYMBOL_MINUS:
                return doNegate(innerValue, widenKind);
            default:
                throw new EvaluatorException("Invalid operator for sign: " + operator, operator);
        }
    }

    @Override
    public int getStart() {
        return operator.getStart();
    }

    @Override
    public int getEnd() {
        return inner.getEnd();
    }

    private Value doNegate(Value innerValue, ValueKind widenKind) {
        switch (widenKind) {
            case INT:
                return IntValue.of(-innerValue.asInt());
            case LONG:
                return LongValue.of(-innerValue.asLong());
            case FLOAT:
                return FloatValue.of(-innerValue.asFloat());
            case DOUBLE:
                return DoubleValue.of(-innerValue.asDouble());
            default:
                throw new EvaluatorException("Invalid type for negate, got " + widenKind, this);
        }
    }

    private Value doReaffirm(Value innerValue, ValueKind widenKind) {
        if (innerValue.getKind() == widenKind) {
            return innerValue;
        }
        switch (widenKind) {
            case INT:
            case LONG:
            case FLOAT:
            case DOUBLE:
                return widenKind.convert(innerValue);
            default:
                throw new EvaluatorException("Invalid type for reaffirm, got " + widenKind, this);
        }
    }

    @Override
    public String toString() {
        return "Sign(" + operator.toString() + inner.toString() + ")";
    }
}
