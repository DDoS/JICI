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
package ca.sapon.jici.parser.expression;

import java.util.List;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.evaluator.value.type.ValueType;
import ca.sapon.jici.lexer.Identifier;
import ca.sapon.jici.parser.expression.reference.AmbiguousReference;
import ca.sapon.jici.parser.expression.reference.MethodCall;
import ca.sapon.jici.parser.statement.Statement;
import ca.sapon.jici.util.StringUtil;

public class AmbiguousCall implements Expression, Statement {
    private final List<Identifier> name;
    private final List<Expression> arguments;
    private ValueType valueType = null;
    private MethodCall call = null;

    public AmbiguousCall(List<Identifier> name, List<Expression> arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    @Override
    public void execute(Environment environment) {
        getValueType(environment);
        getValue(environment);
    }

    @Override
    public ValueType getValueType(Environment environment) {
        if (valueType == null) {
            final int lastIndex = name.size() - 1;
            final Expression resolved = AmbiguousReference.disambiguate(environment, name.subList(0, lastIndex));
            call = new MethodCall(resolved, name.get(lastIndex), arguments);
            valueType = call.getValueType(environment);
        }
        return valueType;
    }

    @Override
    public Value getValue(Environment environment) {
        return call.getValue(environment);
    }

    @Override
    public String toString() {
        return "MethodCall(" + StringUtil.toString(name, ".") + "(" + StringUtil.toString(arguments, ", ") + "))";
    }
}