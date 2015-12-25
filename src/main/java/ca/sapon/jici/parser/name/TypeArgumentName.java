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

import java.util.Collections;

import ca.sapon.jici.SourceIndexed;
import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.evaluator.type.LiteralType;
import ca.sapon.jici.evaluator.type.SingleReferenceType;
import ca.sapon.jici.evaluator.type.TypeArgument;
import ca.sapon.jici.evaluator.type.WildcardType;

/**
 *
 */
public class TypeArgumentName implements SourceIndexed {
    private final TypeName bound;
    private final BoundKind kind;
    private TypeArgument type = null;

    public TypeArgumentName(TypeName bound, BoundKind kind) {
        this.bound = bound;
        this.kind = kind;
    }

    public TypeArgument getType(Environment environment) {
        if (type == null) {
            if (kind == BoundKind.NONE) {
                type = WildcardType.THE_UNBOUNDED;
            } else {
                final LiteralType bound = this.bound.getType(environment);
                if (!(bound instanceof TypeArgument)) {
                    throw new EvaluatorException("Name must be a type argument", this);
                }
                switch (kind) {
                    case EXACT:
                        type = (TypeArgument) bound;
                        break;
                    case LOWER:
                        type = WildcardType.of(Collections.singleton((SingleReferenceType) bound), Collections.<SingleReferenceType>emptySet());
                        break;
                    case UPPER:
                        type = WildcardType.of(Collections.<SingleReferenceType>emptySet(), Collections.singleton((SingleReferenceType) bound));
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
