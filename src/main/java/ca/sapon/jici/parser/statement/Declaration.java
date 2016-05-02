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
package ca.sapon.jici.parser.statement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.evaluator.type.LiteralReferenceType;
import ca.sapon.jici.evaluator.type.LiteralType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.lexer.Identifier;
import ca.sapon.jici.lexer.literal.number.IntLiteral;
import ca.sapon.jici.parser.expression.ArrayConstructor.ArrayInitializer;
import ca.sapon.jici.parser.expression.Expression;
import ca.sapon.jici.parser.expression.reference.VariableAccess;
import ca.sapon.jici.parser.name.ArrayTypeName;
import ca.sapon.jici.parser.name.TypeName;
import ca.sapon.jici.util.StringUtil;
import ca.sapon.jici.util.TypeUtil;

public class Declaration implements Statement {
    private final TypeName typeName;
    private final List<Variable> variables;
    private Map<Variable, LiteralType> declaredTypes = null;

    public Declaration(TypeName typeName, List<Variable> variables) {
        this.typeName = typeName;
        this.variables = variables;
    }

    @Override
    public void execute(Environment environment) {
        try {
            if (declaredTypes == null) {
                final LiteralType declarationType;
                try {
                    declarationType = typeName.getType(environment);
                } catch (UnsupportedOperationException exception) {
                    throw new EvaluatorException(exception.getMessage(), typeName);

                }
                // find the declared type of each variable, which can change by array dimensions
                final Map<Variable, LiteralType> declaredTypes = new HashMap<>();
                for (Variable variable : variables) {
                    int dimensions = variable.getDimensions();
                    if (dimensions == 0) {
                        // no extra dimensions, use the declaration type
                        declaredTypes.put(variable, declarationType);
                    } else {
                        // else add the dimensions and compute the new array type
                        final LiteralType componentType;
                        if (typeName instanceof ArrayTypeName) {
                            // ReflectionUtil.asArrayType doesn't support array types for component type, so get the base one
                            final ArrayTypeName arrayTypeName = (ArrayTypeName) typeName;
                            componentType = arrayTypeName.getComponentType();
                            dimensions += arrayTypeName.getDimensions();
                        } else {
                            componentType = declarationType;
                        }
                        final LiteralReferenceType declaredType = componentType.asArray(dimensions);
                        declaredTypes.put(variable, declaredType);
                    }
                }
                // validate that the variable value type can be converted to the declared type
                for (Entry<Variable, LiteralType> entry : declaredTypes.entrySet()) {
                    final Variable variable = entry.getKey();
                    final Type declaredType = entry.getValue();
                    Type valueType = variable.getType(environment, declaredType);
                    // Must capture variables subject to assignment conversion
                    if (variable.getValueExpression() instanceof VariableAccess) {
                        valueType = valueType.capture();
                    }
                    // Check validity of the assignment
                    if (!valueType.convertibleTo(declaredType)) {
                        if (variable.getValueExpression() instanceof IntLiteral) {
                            final IntLiteral intLiteral = (IntLiteral) variable.getValueExpression();
                            if (!TypeUtil.coerceToPrimitive(variable.getValueExpression(), declaredType).canNarrowFrom(intLiteral.asInt())) {
                                throw new EvaluatorException("Cannot narrow " + intLiteral + " to " + declaredType.getName(), intLiteral);
                            }
                        } else {
                            throw new EvaluatorException("Cannot convert " + valueType.getName() + " to " + declaredType.getName(), variable.getValueExpression());
                        }
                    }
                }
                this.declaredTypes = declaredTypes;
            }
            for (Entry<Variable, LiteralType> entry : declaredTypes.entrySet()) {
                final Variable variable = entry.getKey();
                final LiteralType declaredType = entry.getValue();
                final Identifier name = variable.getName();
                try {
                    environment.declareVariable(name, declaredType, variable.getValue(environment));
                } catch (UnsupportedOperationException exception) {
                    throw new EvaluatorException(exception.getMessage(), name);
                }
            }
        } catch (EvaluatorException exception) {
            throw exception;
        } catch (UnsupportedOperationException exception) {
            throw new EvaluatorException(exception.getMessage(), this);
        } catch (Exception exception) {
            throw new EvaluatorException(exception, this);
        }
    }

    @Override
    public int getStart() {
        return typeName.getStart();
    }

    @Override
    public int getEnd() {
        return variables.get(variables.size() - 1).getEnd();
    }

    @Override
    public String toString() {
        return "Declaration(" + typeName + " " + StringUtil.toString(variables, ", ") + ")";
    }

    public static class Variable {
        private final Identifier name;
        private final int dimensions;
        private final Expression value;
        private Type type = null;

        public Variable(Identifier name, int dimensions) {
            this(name, dimensions, null);
        }

        public Variable(Identifier name, int dimensions, Expression value) {
            this.name = name;
            this.dimensions = dimensions;
            this.value = value;
        }

        private Identifier getName() {
            return name;
        }

        private int getDimensions() {
            return dimensions;
        }

        private Expression getValueExpression() {
            return value;
        }

        private Value getValue(Environment environment) {
            return value == null ? null : value.getValue(environment);
        }

        private Type getType(Environment environment, Type declaredType) {
            if (type == null) {
                if (value == null) {
                    type = declaredType;
                } else if (value instanceof ArrayInitializer) {
                    ((ArrayInitializer) value).setType(environment, declaredType);
                    type = declaredType;
                } else {
                    type = value.getType(environment);
                }
            }
            return type;
        }

        private int getEnd() {
            return value == null ? name.getEnd() : value.getEnd();
        }

        @Override
        public String toString() {
            return name + StringUtil.repeat("[]", dimensions) + (value != null ? " = " + value : "");
        }
    }
}
