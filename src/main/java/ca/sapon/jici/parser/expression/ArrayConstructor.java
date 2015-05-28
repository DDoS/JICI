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

import java.lang.reflect.Array;
import java.util.List;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.evaluator.type.ClassType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.value.ObjectValue;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.parser.name.ArrayTypeName;
import ca.sapon.jici.util.StringUtil;

/**
 *
 */
public class ArrayConstructor implements Expression {
    private final ArrayTypeName typeName;
    private final ArrayInitializer initializer;

    public ArrayConstructor(ArrayTypeName typeName, ArrayInitializer initializer) {
        this.typeName = typeName;
        this.initializer = initializer;
    }

    @Override
    public Type getType(Environment environment) {
        return null;
    }

    @Override
    public Value getValue(Environment environment) {
        return null;
    }

    @Override
    public int getStart() {
        return 0;
    }

    @Override
    public int getEnd() {
        return 0;
    }

    public static class ArrayInitializer implements Expression {
        private final List<Expression> elements;
        private final int start;
        private final int end;
        private Type type = null;
        private Type componentType = null;

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
                final Type componentType = ClassType.of(type.getTypeClass().getComponentType());
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
            throw new IllegalArgumentException("Cannot reset array initializer type");
        }

        @Override
        public Type getType(Environment environment) {
            if (type == null) {
                throw new IllegalArgumentException("Array initializer type has not been set");
            }
            return type;
        }

        @Override
        public Value getValue(Environment environment) {
            final int size = elements.size();
            final Object array = Array.newInstance(componentType.getTypeClass(), size);
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
        public String toString() {
            return '{' + StringUtil.toString(elements, ", ") + '}';
        }
    }
}
