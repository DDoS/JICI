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

import java.util.ArrayList;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.evaluator.type.ClassType;
import ca.sapon.jici.evaluator.type.ClassUnionType;
import ca.sapon.jici.evaluator.type.ConcreteType;
import ca.sapon.jici.evaluator.type.SingleClassType;
import ca.sapon.jici.util.StringUtil;

public class ArrayTypeName implements TypeName, ImportedTypeName {
    private final TypeName componentTypeName;
    private final int dimensions;
    private ConcreteType componentType = null;
    private ConcreteType type = null;

    public ArrayTypeName(TypeName componentTypeName, int dimensions) {
        this.componentTypeName = componentTypeName;
        this.dimensions = dimensions;
    }

    @Override
    public ConcreteType getType(Environment environment) {
        if (type == null) {
            final ConcreteType componentType = componentTypeName.getType(environment);
            final SingleClassType arrayType = componentType.asArray(dimensions);
            if (arrayType == null) {
                throw new EvaluatorException("Class not found: array of " + componentType.getName() + " with dimensions " + dimensions, this);
            }
            this.componentType = componentType;
            type = arrayType;
        }
        return type;
    }

    @Override
    public void setTypeHint(ClassType hint) {
        if (!(componentTypeName instanceof ImportedTypeName)) {
            return;
        }
        final ImportedTypeName typeName = (ImportedTypeName) this.componentTypeName;
        if (hint instanceof SingleClassType) {
            final Class<?> validated = validateTypeHint(((SingleClassType) hint).getTypeClass());
            if (validated != null) {
                typeName.setTypeHint(SingleClassType.of(validated));
            }
        } else if (hint instanceof ClassUnionType) {
            final ClassUnionType classUnion = (ClassUnionType) hint;
            final ArrayList<ClassType> hints = new ArrayList<>();
            for (Class<?> _class : classUnion.getTypeClasses()) {
                final Class<?> validated = validateTypeHint(_class);
                if (validated != null) {
                    hints.add(SingleClassType.of(validated));
                }
            }
            switch (hints.size()) {
                case 0:
                    break;
                case 1:
                    typeName.setTypeHint(hints.get(0));
                    break;
                default:
                    typeName.setTypeHint(new ClassUnionType(hints));
                    break;
            }
        }
    }

    private Class<?> validateTypeHint(Class<?> _class) {
        int dimensions = 0;
        Class<?> componentType = _class;
        while (true) {
            final Class<?> nextComponentType = componentType.getComponentType();
            if (nextComponentType == null) {
                break;
            }
            componentType = nextComponentType;
            dimensions++;
        }
        return dimensions == this.dimensions && !componentType.isPrimitive() ? componentType : null;
    }

    public ConcreteType getComponentType() {
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
