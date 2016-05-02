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

import ca.sapon.jici.SourceIndexed;
import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.type.LiteralReferenceType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.value.ObjectValue;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.lexer.Token;
import ca.sapon.jici.lexer.TokenID;
import ca.sapon.jici.parser.name.TypeName;

public class ClassAccess implements Expression {
    private static final LiteralReferenceType TYPE = LiteralReferenceType.of(Class.class);
    private final TypeName typeName;
    private final SourceIndexed source;
    private int start;
    private int end;
    private Type type = null;
    private Class<?> typeClass = null;

    public ClassAccess(Token voidToken, int end) {
        if (voidToken.getID() != TokenID.KEYWORD_VOID) {
            throw new IllegalArgumentException("Not a void token " + voidToken);
        }
        typeName = null;
        source = voidToken;
        start = source.getStart();
        this.end = end;
    }

    public ClassAccess(TypeName typeName, int end) {
        this.typeName = typeName;
        source = typeName;
        this.end = end;
    }

    @Override
    public Type getType(Environment environment) {
        if (type == null) {
            if (typeName == null) {
                typeClass = void.class;
            } else {
                typeClass = typeName.getType(environment).getTypeClass();
            }
            type = TYPE;
        }
        return type;
    }

    @Override
    public Value getValue(Environment environment) {
        return ObjectValue.of(typeClass);
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
        return "ClassAccess(" + source + ".class" + ")";
    }
}
