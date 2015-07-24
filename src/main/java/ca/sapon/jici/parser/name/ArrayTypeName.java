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
package ca.sapon.jici.parser.name;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.evaluator.type.ClassType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.util.ReflectionUtil;
import ca.sapon.jici.util.StringUtil;

public class ArrayTypeName implements TypeName, ImportedTypeName {
    private final TypeName componentTypeName;
    private final int dimensions;
    private Class<?> componentType = null;
    private Type type = null;

    public ArrayTypeName(TypeName componentTypeName, int dimensions) {
        this.componentTypeName = componentTypeName;
        this.dimensions = dimensions;
    }

    @Override
    public Type getType(Environment environment) {
        if (type == null) {
            final Class<?> componentType = componentTypeName.getType(environment).getTypeClass();
            final Class<?> arrayType = ReflectionUtil.asArrayType(componentType, dimensions);
            if (arrayType == null) {
                throw new EvaluatorException("Class not found: array of " + componentType.getCanonicalName() + " with dimensions " + dimensions, this);
            }
            this.componentType = componentType;
            type = ClassType.of(arrayType);
        }
        return type;
    }

    @Override
    public void setTypeHint(Type hint) {
        if (componentTypeName instanceof ImportedTypeName && hint instanceof ClassType && hint.isArray()) {
            int dimensions = 0;
            Class<?> componentType = hint.getTypeClass();
            while (true) {
                final Class<?> nextComponentType = componentType.getComponentType();
                if (nextComponentType == null) {
                    break;
                }
                componentType = nextComponentType;
                dimensions++;
            }
            if (dimensions == this.dimensions) {
                ((ImportedTypeName) componentTypeName).setTypeHint(ReflectionUtil.wrap(componentType));
            }
        }
    }

    public Class<?> getComponentType() {
        return componentType;
    }

    public int getDimensions() {
        return dimensions;
    }

    @Override
    public int getStart() {
        return componentTypeName.getStart();
    }

    @Override
    public int getEnd() {
        return componentTypeName.getEnd();
    }

    @Override
    public String toString() {
        return componentTypeName.toString() + StringUtil.repeat("[]", dimensions);
    }
}
