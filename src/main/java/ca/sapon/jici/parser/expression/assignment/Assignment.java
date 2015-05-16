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
package ca.sapon.jici.parser.expression.assignment;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.evaluator.value.type.ValueType;
import ca.sapon.jici.lexer.Symbol;
import ca.sapon.jici.lexer.literal.number.IntLiteral;
import ca.sapon.jici.parser.expression.Expression;
import ca.sapon.jici.parser.expression.Shift;
import ca.sapon.jici.parser.expression.arithmetic.Arithmetic;
import ca.sapon.jici.parser.expression.logic.BitwiseLogic;
import ca.sapon.jici.parser.expression.reference.Reference;
import ca.sapon.jici.parser.statement.Statement;

public class Assignment implements Expression, Statement {
    private final Reference assignee;
    private final Expression value;
    private boolean simpleAssign = false;
    private ValueType valueType = null;

    public Assignment(Reference assignee, Expression value, Symbol operator) {
        this.assignee = assignee;
        switch (operator.getID()) {
            case SYMBOL_ASSIGN:
                simpleAssign = true;
                this.value = value;
                break;
            case SYMBOL_ADD_ASSIGN:
            case SYMBOL_SUBTRACT_ASSIGN:
            case SYMBOL_MULTIPLY_ASSIGN:
            case SYMBOL_DIVIDE_ASSIGN:
            case SYMBOL_REMAINDER_ASSIGN:
                this.value = new Arithmetic(assignee, value, operator.getCompoundAssignOperator());
                break;
            case SYMBOL_BITWISE_AND_ASSIGN:
            case SYMBOL_BITWISE_OR_ASSIGN:
            case SYMBOL_BITWISE_XOR_ASSIGN:
                this.value = new BitwiseLogic(assignee, value, operator.getCompoundAssignOperator());
                break;
            case SYMBOL_LOGICAL_LEFT_SHIFT_ASSIGN:
            case SYMBOL_ARITHMETIC_RIGHT_SHIFT_ASSIGN:
            case SYMBOL_LOGICAL_RIGHT_SHIFT_ASSIGN:
                this.value = new Shift(assignee, value, operator.getCompoundAssignOperator());
                break;
            default:
                throw new EvaluatorException("Not a valid operator for assign: " + operator, operator);
        }
    }

    @Override
    public void execute(Environment environment) {
        try {
            getValueType(environment);
            getValue(environment);
        } catch (EvaluatorException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new EvaluatorException(exception, this);
        }
    }

    @Override
    public ValueType getValueType(Environment environment) {
        if (valueType == null) {
            final ValueType type = value.getValueType(environment);
            final ValueType assigneeType = assignee.getValueType(environment);
            if ((simpleAssign || !type.isNumeric() || !assigneeType.isNumeric()) && !type.convertibleTo(assigneeType.getTypeClass())) {
                if (value instanceof IntLiteral) {
                    final IntLiteral intLiteral = (IntLiteral) value;
                    if (!assigneeType.unbox().canNarrowFrom(intLiteral.asInt())) {
                        throw new EvaluatorException("Cannot narrow " + intLiteral + " to " + assigneeType.getName(), intLiteral);
                    }
                } else {
                    throw new EvaluatorException("Cannot convert " + type.getName() + " to " + assigneeType.getName(), value);
                }
            }
            valueType = assigneeType;
        }
        return valueType;
    }

    @Override
    public Value getValue(Environment environment) {
        final Value result = value.getValue(environment);
        assignee.setValue(environment, simpleAssign ? result : valueType.getKind().convert(result));
        return result;
    }

    @Override
    public int getStart() {
        return assignee.getStart();
    }

    @Override
    public int getEnd() {
        return value.getEnd();
    }

    @Override
    public String toString() {
        return "Assignment(" + assignee + " = " + value + ")";
    }
}
