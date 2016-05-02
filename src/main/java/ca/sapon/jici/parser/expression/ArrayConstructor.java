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
package ca.sapon.jici.parser.expression;

import java.lang.reflect.Array;
import java.util.List;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.evaluator.type.ComponentType;
import ca.sapon.jici.evaluator.type.LiteralType;
import ca.sapon.jici.evaluator.type.PrimitiveType;
import ca.sapon.jici.evaluator.type.ReferenceType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.value.ObjectValue;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.parser.name.ArrayTypeName;
import ca.sapon.jici.parser.name.TypeName;
import ca.sapon.jici.util.StringUtil;

/**
 *
 */
public class ArrayConstructor implements Expression {
    private final TypeName typeName;
    private final ArrayInitializer initializer;
    private final List<Expression> sizes;
    private int start;
    private int end;
    private ComponentType componentType = null;
    private int sizedDimensions = 0;
    private Type type = null;

    public ArrayConstructor(ArrayTypeName typeName, ArrayInitializer initializer, int start, int end) {
        this(typeName, null, initializer, start, end);
    }

    public ArrayConstructor(TypeName componentTypeName, List<Expression> sizes, int start, int end) {
        this(componentTypeName, sizes, null, start, end);
    }

    private ArrayConstructor(TypeName typeName, List<Expression> sizes, ArrayInitializer initializer, int start, int end) {
        this.typeName = typeName;
        this.initializer = initializer;
        this.sizes = sizes;
        this.start = start;
        this.end = end;
    }

    @Override
    public Type getType(Environment environment) {
        if (type != null) {
            return type;
        }
        if (initializer == null) {
            int sizedDimensions = 0;
            for (Expression size : sizes) {
                if (size == null) {
                    break;
                }
                final Type sizeType = size.getType(environment);
                if (!sizeType.convertibleTo(PrimitiveType.THE_INT)) {
                    throw new EvaluatorException("Cannot convert " + sizeType.getName() + " to int", size);
                }
                sizedDimensions++;
            }
            final int dimensions = sizes.size();
            final int unsizedDimensions = dimensions - sizedDimensions;
            final LiteralType baseType = typeName.getType(environment);
            if (!baseType.isReifiable()) {
                throw new EvaluatorException("Array component type must be reifiable", typeName);
            }
            final ComponentType componentType;
            if (unsizedDimensions != 0) {
                componentType = baseType.asArray(unsizedDimensions);
            } else {
                componentType = baseType;
            }
            this.componentType = componentType;
            this.sizedDimensions = sizedDimensions;
            type = baseType.asArray(dimensions);
        } else {
            final Type arrayType = typeName.getType(environment);
            initializer.setType(environment, arrayType);
            type = arrayType;
        }
        return type;
    }

    @Override
    public Value getValue(Environment environment) {
        if (initializer == null) {
            final int[] dimensionSizes = new int[sizedDimensions];
            for (int i = 0; i < sizedDimensions; i++) {
                dimensionSizes[i] = sizes.get(i).getValue(environment).asInt();
            }
            return ObjectValue.of(componentType.newArray(dimensionSizes));
        }
        return initializer.getValue(environment);
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
        return "ArrayConstructor(new " + typeName + (initializer == null ? sizesToString() : initializer) + ")";
    }

    private String sizesToString() {
        final StringBuilder builder = new StringBuilder();
        for (Expression size : sizes) {
            if (size == null) {
                builder.append("[]");
            } else {
                builder.append('[').append(size).append(']');
            }
        }
        return builder.toString();
    }

    public static class ArrayInitializer implements Expression {
        private final List<Expression> elements;
        private int start;
        private int end;
        private Type type = null;
        private ComponentType componentType = null;

        public ArrayInitializer(List<Expression> elements, int start, int end) {
            this.elements = elements;
            this.start = start;
            this.end = end;
        }

        public void setType(Environment environment, Type type) {
            if (this.type == null) {
                if (!type.isArray()) {
                    throw new EvaluatorException("Cannot convert array type to " + type.getName(), this);
                }
                final ComponentType componentType = ((ReferenceType) type).getComponentType();
                for (final Expression element : elements) {
                    if (element instanceof ArrayInitializer) {
                        ((ArrayInitializer) element).setType(environment, componentType);
                    } else {
                        final Type elementType = element.getType(environment);
                        if (!elementType.convertibleTo(componentType)) {
                            throw new EvaluatorException("Cannot convert " + elementType.getName() + " to " + componentType.getName(), this);
                        }
                    }
                }
                this.componentType = componentType;
                this.type = type;
                return;
            }
            throw new UnsupportedOperationException("Cannot reset array initializer type");
        }

        @Override
        public Type getType(Environment environment) {
            if (type == null) {
                throw new UnsupportedOperationException("Array initializer type has not been set");
            }
            return type;
        }

        @Override
        public Value getValue(Environment environment) {
            final int size = elements.size();
            final Object array = componentType.newArray(size);
            for (int i = 0; i < size; i++) {
                Array.set(array, i, elements.get(i).getValue(environment).asObject());
            }
            return ObjectValue.of(array);
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
            return '{' + StringUtil.toString(elements, ", ") + '}';
        }
    }
}
