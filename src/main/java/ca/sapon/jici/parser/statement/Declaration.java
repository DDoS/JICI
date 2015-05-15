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
package ca.sapon.jici.parser.statement;

import java.util.List;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.evaluator.value.type.ValueType;
import ca.sapon.jici.lexer.Identifier;
import ca.sapon.jici.lexer.literal.number.IntLiteral;
import ca.sapon.jici.parser.expression.Expression;
import ca.sapon.jici.parser.type.Type;
import ca.sapon.jici.util.StringUtil;

public class Declaration implements Statement {
    private final Type type;
    private final List<Variable> variables;
    private ValueType valueType = null;

    public Declaration(Type type, List<Variable> variables) {
        this.type = type;
        this.variables = variables;
    }

    @Override
    public void execute(Environment environment) {
        if (valueType == null) {
            final ValueType declarationType = type.getValueType(environment);
            if (declarationType.isVoid()) {
                throw new IllegalArgumentException("Illegal type: void");
            }
            for (Variable variable : variables) {
                final ValueType variableType = variable.getValueType(environment);
                if (variableType != null && !variableType.convertibleTo(declarationType.getTypeClass())) {
                    if (variable.getValueExpression() instanceof IntLiteral) {
                        final IntLiteral intLiteral = (IntLiteral) variable.getValueExpression();
                        if (!declarationType.unbox().canNarrowFrom(intLiteral.asInt())) {
                            throw new IllegalArgumentException("Cannot narrow " + intLiteral + " to " + declarationType.getName());
                        }
                    } else {
                        throw new IllegalArgumentException("Cannot convert " + variableType.getName() + " to " + declarationType.getName());
                    }
                }
            }
            valueType = declarationType;
        }
        for (Variable variable : variables) {
            final Identifier name = variable.getName();
            environment.declareVariable(name, valueType, variable.getValue(environment));
        }
    }

    @Override
    public String toString() {
        return "Declaration(" + type + " " + StringUtil.toString(variables, ", ") + ")";
    }

    public static class Variable {
        private final Identifier name;
        private final Expression value;
        private ValueType valueType = null;

        public Variable(Identifier name) {
            this(name, null);
        }

        public Variable(Identifier name, Expression value) {
            this.name = name;
            this.value = value;
        }

        public Identifier getName() {
            return name;
        }

        public Expression getValueExpression() {
            return value;
        }

        public Value getValue(Environment environment) {
            return value == null ? null : value.getValue(environment);
        }

        private ValueType getValueType(Environment environment) {
            if (value == null) {
                return null;
            }
            if (valueType == null) {
                valueType = value.getValueType(environment);
            }
            return valueType;
        }

        @Override
        public String toString() {
            return name + (value != null ? " = " + value : "");
        }
    }
}
