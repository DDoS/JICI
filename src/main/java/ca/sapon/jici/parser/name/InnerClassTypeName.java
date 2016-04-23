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
import java.util.List;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.type.LiteralReferenceType;
import ca.sapon.jici.evaluator.type.ParametrizedType;
import ca.sapon.jici.lexer.Identifier;

/**
 *
 */
public class InnerClassTypeName extends ClassTypeName {
    private final ClassTypeName outerName;
    private ParametrizedType outerType;
    private boolean typeCached = false;

    public InnerClassTypeName(ClassTypeName outerName, List<Identifier> name) {
        this(outerName, name, Collections.<TypeArgumentName>emptyList());
    }

    public InnerClassTypeName(ClassTypeName outerName, List<Identifier> name, List<TypeArgumentName> arguments) {
        super(name, arguments);
        this.outerName = outerName;
    }

    @Override
    protected ParametrizedType getOwner(Environment environment) {
        if (!typeCached) {
            final LiteralReferenceType outerType = outerName.getType(environment);
            this.outerType = outerType instanceof ParametrizedType ? (ParametrizedType) outerType : null;
            typeCached = true;
        }
        return outerType;
    }

    @Override
    public int getStart() {
        return outerName.getStart();
    }

    @Override
    public String toString() {
        return outerName.toString() + '.' + super.toString();
    }
}
