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
package ca.sapon.jici.parser.expression.comparison;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.evaluator.type.PrimitiveType;
import ca.sapon.jici.evaluator.type.SingleClassType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.value.BooleanValue;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.parser.expression.Expression;
import ca.sapon.jici.parser.name.TypeName;

public class TypeCheck implements Expression {
    private final Expression object;
    private final TypeName typeName;
    private Type type = null;
    private SingleClassType checkType = null;

    public TypeCheck(Expression object, TypeName typeName) {
        this.object = object;
        this.typeName = typeName;
    }

    @Override
    public Type getType(Environment environment) {
        if (type == null) {
            final Type objectType = object.getType(environment);
            final Type checkType = typeName.getType(environment);
            if (!(checkType instanceof SingleClassType)) {
                throw new EvaluatorException("Cannot check type " + checkType.getName(), typeName);
            }
            if (objectType.isPrimitive()) {
                throw new EvaluatorException("Cannot type check " + objectType.getName(), object);
            }
            if (objectType.isVoid()) {
                throw new EvaluatorException("Cannot type check void", object);
            }
            type = PrimitiveType.THE_BOOLEAN;
            this.checkType = (SingleClassType) checkType;
        }
        return type;
    }

    @Override
    public Value getValue(Environment environment) {
        final Value value = object.getValue(environment);
        return BooleanValue.of(checkType.getTypeClass().isInstance(value.asObject()));
    }

    @Override
    public int getStart() {
        return object.getStart();
    }

    @Override
    public int getEnd() {
        return typeName.getEnd();
    }

    @Override
    public String toString() {
        return "TypeCheck(" + object + " instanceof " + typeName + ")";
    }
}
