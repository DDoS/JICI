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

import ca.sapon.jici.SourceIndexed;
import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.evaluator.type.ConcreteType;
import ca.sapon.jici.evaluator.type.SingleClassType;
import ca.sapon.jici.evaluator.type.TypeParameter;
import ca.sapon.jici.evaluator.type.WildcardType;

/**
 *
 */
public class TypeParameterName implements SourceIndexed {
    private final TypeName bound;
    private final BoundKind kind;
    private TypeParameter type = null;

    public TypeParameterName(TypeName bound, BoundKind kind) {
        this.bound = bound;
        this.kind = kind;
    }

    public TypeParameter getType(Environment environment) {
        if (type == null) {
            if (kind == BoundKind.NONE) {
                type = new WildcardType(null, kind);
            } else {
                final ConcreteType bound = this.bound.getType(environment);
                if (!(bound instanceof SingleClassType)) {
                    throw new EvaluatorException("Type parameter must be a class type", this);
                }
                switch (kind) {
                    case EXACT:
                        type = (SingleClassType) bound;
                        break;
                    case LOWER:
                    case UPPER:
                        type = new WildcardType((SingleClassType) bound, kind);
                        break;
                }
            }
        }
        return type;
    }

    @Override
    public int getStart() {
        return bound.getStart();
    }

    @Override
    public int getEnd() {
        return bound.getEnd();
    }

    @Override
    public String toString() {
        switch (kind) {
            case EXACT:
                return bound.toString();
            case NONE:
                return "?";
            case LOWER:
                return "? super " + bound.toString();
            case UPPER:
                return "? extends " + bound.toString();
            default:
                throw new UnsupportedOperationException(kind.name());
        }
    }

    public enum BoundKind {
        EXACT,
        NONE,
        LOWER,
        UPPER
    }
}
