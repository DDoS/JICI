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
import ca.sapon.jici.evaluator.type.ConcreteType;

/**
 *
 */
public class TypeParameterName implements TypeName {
    private final TypeName type;
    private final BoundKind bound;

    public TypeParameterName() {
        this(null, BoundKind.NONE);
    }

    public TypeParameterName(TypeName type) {
        this.type = type;
        this.bound = null;
    }

    public TypeParameterName(TypeName bound, BoundKind kind) {
        this.type = bound;
        this.bound = kind;
    }

    @Override
    public ConcreteType getType(Environment environment) {
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

    @Override
    public String toString() {
        if (bound == null) {
            return type.toString();
        }
        switch (bound) {
            case NONE:
                return "?";
            case LOWER:
                return "? super " + type.toString();
            case UPPER:
                return "? extends " + type.toString();
            default:
                throw new UnsupportedOperationException(bound.name());
        }
    }

    public enum BoundKind {
        NONE,
        LOWER,
        UPPER
    }
}
