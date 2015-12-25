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
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.evaluator.type.PrimitiveType;
import ca.sapon.jici.evaluator.type.ReferenceType;
import ca.sapon.jici.evaluator.type.SingleReferenceType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.value.ObjectValue;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.evaluator.value.ValueKind;
import ca.sapon.jici.lexer.literal.number.IntLiteral;
import ca.sapon.jici.util.TypeUtil;

public class Conditional implements Expression {
    private final Expression test;
    private final Expression left;
    private final Expression right;
    private Type type = null;

    public Conditional(Expression test, Expression left, Expression right) {
        this.test = test;
        this.left = left;
        this.right = right;
    }

    @Override
    public Type getType(Environment environment) {
        if (type == null) {
            final Type testType = TypeUtil.coerceToPrimitive(environment, test);
            Type leftType = left.getType(environment);
            Type rightType = right.getType(environment);
            if (!testType.isBoolean()) {
                throw new EvaluatorException("Not a boolean: " + testType.getName(), test);
            }
            if (leftType.isVoid()) {
                throw new EvaluatorException("Illegal type: void", left);
            }
            if (rightType.isVoid()) {
                throw new EvaluatorException("Illegal type: void", right);
            }
            if (leftType.equals(rightType)) {
                // both same type to that type
                return type = leftType;
            }
            if (leftType.isNull()) {
                // left null to right type
                if (rightType instanceof PrimitiveType) {
                    rightType = ((PrimitiveType) rightType).box();
                }
                return type = rightType;
            }
            if (rightType.isNull()) {
                // right null to left type
                if (leftType instanceof PrimitiveType) {
                    leftType = ((PrimitiveType) leftType).box();
                }
                return type = leftType;
            }
            if (leftType instanceof SingleReferenceType) {
                leftType = ((SingleReferenceType) leftType).tryUnbox();
            }
            if (rightType instanceof SingleReferenceType) {
                rightType = ((SingleReferenceType) rightType).tryUnbox();
            }
            if (leftType.isObject() || rightType.isObject()) {
                // for objects the lowest upper bound
                return type = TypeUtil.lowestUpperBound(
                        leftType instanceof PrimitiveType ? ((PrimitiveType) leftType).box() : (ReferenceType) leftType,
                        rightType instanceof PrimitiveType ? ((PrimitiveType) rightType).box() : (ReferenceType) rightType
                );
            }
            final PrimitiveType leftPrimitiveType = (PrimitiveType) leftType;
            final PrimitiveType rightPrimitiveType = (PrimitiveType) rightType;
            if (left instanceof IntLiteral && rightPrimitiveType.canNarrowFrom(((IntLiteral) left).asInt())) {
                // left constant numeric that narrows to right, use right
                return type = rightPrimitiveType;
            }
            if (right instanceof IntLiteral && leftPrimitiveType.canNarrowFrom(((IntLiteral) right).asInt())) {
                // right constant numeric that narrows to left, use left
                return type = leftPrimitiveType;
            }
            if (leftPrimitiveType.equals(PrimitiveType.THE_BYTE) && rightPrimitiveType.equals(PrimitiveType.THE_SHORT)
                    || leftPrimitiveType.equals(PrimitiveType.THE_SHORT) && rightPrimitiveType.equals(PrimitiveType.THE_BYTE)) {
                // one byte and other short to short
                return type = PrimitiveType.THE_SHORT;
            }
            // else use binary widening
            return type = leftPrimitiveType.binaryWiden(rightPrimitiveType);
        }
        return type;
    }

    @Override
    public Value getValue(Environment environment) {
        final Value testValue = test.getValue(environment);
        final ValueKind widenKind = type.getKind();
        final Value resultValue = testValue.asBoolean() ? left.getValue(environment) : right.getValue(environment);
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
    public int getStart() {
        return test.getStart();
    }

    @Override
    public int getEnd() {
        return right.getEnd();
    }

    @Override
    public String toString() {
        return "Conditional(" + test + " ? " + left + " : " + right + ")";
    }
}
