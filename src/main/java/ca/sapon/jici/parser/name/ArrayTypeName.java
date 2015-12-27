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
import ca.sapon.jici.evaluator.type.LiteralReferenceType;
import ca.sapon.jici.evaluator.type.LiteralType;
import ca.sapon.jici.util.StringUtil;

public class ArrayTypeName implements TypeName {
    private final TypeName componentTypeName;
    private final int dimensions;
    private LiteralType componentType = null;
    private LiteralType type = null;

    public ArrayTypeName(TypeName componentTypeName, int dimensions) {
        this.componentTypeName = componentTypeName;
        this.dimensions = dimensions;
    }

    @Override
    public LiteralType getType(Environment environment) {
        if (type == null) {
            final LiteralType componentType = componentTypeName.getType(environment);
            final LiteralReferenceType arrayType = componentType.asArray(dimensions);
            if (arrayType == null) {
                throw new EvaluatorException("Class not found: array of " + componentType.getName() + " with dimensions " + dimensions, this);
            }
            this.componentType = componentType;
            type = arrayType;
        }
        return type;
    }

    public LiteralType getComponentType() {
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
