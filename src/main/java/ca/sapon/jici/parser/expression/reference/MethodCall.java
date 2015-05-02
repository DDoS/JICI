/**
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
package ca.sapon.jici.parser.expression.reference;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.value.ObjectValue;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.lexer.Identifier;
import ca.sapon.jici.parser.expression.Expression;
import ca.sapon.jici.parser.statement.Statement;
import ca.sapon.jici.util.ReflectionUtil;
import ca.sapon.jici.util.StringUtil;

public class MethodCall extends Dereference implements Statement {
    private final Identifier method;
    private final List<Expression> arguments;
    private Method callable = null;

    public MethodCall(Reference reference, Identifier method, List<Expression> arguments) {
        super(reference);
        this.method = method;
        this.arguments = arguments;
    }

    @Override
    public void execute(Environment environment) {
        getValue(environment);
    }

    @Override
    public Value getValue(Environment environment) {
        final Value value = reference.getValue(environment);
        final int size = arguments.size();
        final Object[] values = new Object[size];
        for (int i = 0; i < size; i++) {
            values[i] = arguments.get(i).getValue(environment).asObject();
        }
        try {
            return ObjectValue.of(getMethod(value.getTypeClass(), values).invoke(value.asObject(), values));
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalArgumentException("Could not call method", exception);
        }
    }

    private Method getMethod(Class<?> _class, Object[] values) {
        if (callable == null) {
            final Method[] methods = _class.getMethods();
            for (Method candidate : methods) {
                if (ReflectionUtil.validateArgumentTypes(candidate.getParameterTypes(), values)) {
                    callable = candidate;
                    return candidate;
                }
            }
            final List<String> typeNames = new ArrayList<>(values.length);
            for (Object value : values) {
                typeNames.add(value.getClass().getCanonicalName());
            }
            throw new IllegalArgumentException("No constructor for types: " + StringUtil.toString(typeNames, ", "));
        }
        return callable;
    }

    @Override
    public String toString() {
        return "MethodCall(" + (reference instanceof ContextReference ? "" : reference + ".") + method + "(" + StringUtil.toString(arguments, ", ") + "))";
    }
}
