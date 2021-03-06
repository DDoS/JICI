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
package ca.sapon.jici.parser.expression.reference;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.evaluator.member.ClassVariable;
import ca.sapon.jici.evaluator.type.ReferenceType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.lexer.Identifier;
import ca.sapon.jici.parser.expression.Expression;

public class FieldAccess implements Reference {
    private final Expression object;
    private final Identifier field;
    private int start;
    private int end;
    private ClassVariable classVariable = null;

    public FieldAccess(Expression object, Identifier field) {
        this.object = object;
        this.field = field;
        start = field.getStart();
        end = field.getEnd();
    }

    private ClassVariable getClassVariable(Environment environment) {
        if (classVariable != null) {
            return classVariable;
        }
        final Type objectType = object.getType(environment);
        final String name = field.getSource();
        if (!(objectType instanceof ReferenceType)) {
            throw new EvaluatorException("Not a class type " + objectType.getName(), object);
        }
        try {
            classVariable = ((ReferenceType) objectType).getField(name);
        } catch (UnsupportedOperationException exception) {
            throw new EvaluatorException(exception.getMessage(), this);
        }
        if (!classVariable.isStatic() && object instanceof AmbiguousReference.StaticAccess) {
            throw new EvaluatorException("Cannot access a non-static member directly from the type name", field);
        }
        return classVariable;
    }

    @Override
    public Type getType(Environment environment) {
        return getClassVariable(environment).getType();
    }

    @Override
    public Type getTargetType(Environment environment) {
        return getClassVariable(environment).getTargetType();
    }

    @Override
    public Value getValue(Environment environment) {
        final Value target = object.getValue(environment);
        try {
            return getClassVariable(environment).getValue(target);
        } catch (Exception exception) {
            throw new EvaluatorException("Could not access field", exception, field);
        }
    }

    @Override
    public void setValue(Environment environment, Value value) {
        final Value target = object.getValue(environment);
        try {
            getClassVariable(environment).setValue(target, value);
        } catch (Exception exception) {
            throw new EvaluatorException("Could not access field", exception, field);
        }
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
        return "FieldAccess(" + object + "." + field + ")";
    }
}
