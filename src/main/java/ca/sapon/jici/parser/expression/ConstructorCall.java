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
package ca.sapon.jici.parser.expression;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.value.ObjectValue;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.parser.statement.Statement;
import ca.sapon.jici.parser.type.ClassType;
import ca.sapon.jici.util.ReflectionUtil;
import ca.sapon.jici.util.StringUtil;

public class ConstructorCall implements Statement, Expression {
    private final ClassType type;
    private final List<Expression> arguments;
    private Constructor<?> constructor = null;

    public ConstructorCall(ClassType type, List<Expression> arguments) {
        this.arguments = arguments;
        this.type = type;
    }

    @Override
    public void execute(Environment environemnt) {
    }

    @Override
    public Value getValue(Environment environment) {
        final Class<?> _class = type.getTypeClass(environment);
        final int size = arguments.size();
        final Object[] values = new Object[size];
        for (int i = 0; i < size; i++) {
            values[i] = arguments.get(i).getValue(environment).asObject();
        }
        try {
            return ObjectValue.of(getConstructor(_class, values).newInstance(values));
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("Could not call constructor", e);
        }
    }

    private Constructor<?> getConstructor(Class<?> _class, Object[] values) {
        if (constructor == null) {
            final Constructor<?>[] constructors = _class.getConstructors();
            for (Constructor<?> candidate : constructors) {
                if (ReflectionUtil.validateArgumentTypes(candidate.getParameterTypes(), values)) {
                    constructor = candidate;
                    return candidate;
                }
            }
            final List<String> typeNames = new ArrayList<>(values.length);
            for (Object value : values) {
                typeNames.add(value.getClass().getCanonicalName());
            }
            throw new IllegalArgumentException("No constructor for types: " + StringUtil.toString(typeNames, ", "));
        }
        return constructor;
    }

    @Override
    public String toString() {
        return "ConstructorCall(new " + type + "(" + StringUtil.toString(arguments, ", ") + "))";
    }
}
