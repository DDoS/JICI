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
    private Class<?> typeClass = null;
    private Class<?>[] argumentTypes = null;
    private Constructor<?> constructor = null;

    public ConstructorCall(ClassType type, List<Expression> arguments) {
        this.arguments = arguments;
        this.type = type;
    }

    @Override
    public void execute(Environment environment) {
        getTypeClass(environment, null);
        getValue(environment);
    }

    @Override
    public Class<?> getTypeClass(Environment environment, Class<?> upperObjectBound) {
        if (typeClass == null) {
            typeClass = type.getTypeClass(environment);
            // try to find a matching constructor
            argumentTypes = new Class<?>[arguments.size()];
            final int size = arguments.size();
            final Constructor<?>[] constructors = typeClass.getConstructors();
            // look for matches in length
            final List<Constructor<?>> lengthMatches = new ArrayList<>();
            for (Constructor<?> candidate : constructors) {
                if (candidate.getParameterTypes().length == size) {
                    lengthMatches.add(candidate);
                }
            }
            // try to find the lowest object upper bound acceptable
            candidates:
            for (Constructor<?> candidate : lengthMatches) {
                final Class<?>[] upperObjectBounds = candidate.getParameterTypes();
                // validate the upper object bounds
                for (int i = 0; i < size; i++) {
                    try {
                        argumentTypes[i] = arguments.get(i).getTypeClass(environment, upperObjectBounds[i]);
                    } catch (IllegalArgumentException exception) {
                        continue candidates;
                    }
                }
                // validate the primitive parameters
                for (int i = 0; i < size; i++) {
                    if (argumentTypes[i].isPrimitive() && !ReflectionUtil.convertibleTo(upperObjectBounds[i], argumentTypes[i])) {
                        continue candidates;
                    }
                }
                // TODO: only change if the match is closer
                constructor = candidate;
            }
        }
        return typeClass;
    }

    @Override
    public Value getValue(Environment environment) {
        final int size = arguments.size();
        final Object[] values = new Object[size];
        for (int i = 0; i < size; i++) {
            values[i] = arguments.get(i).getValue(environment).asObject();
        }
        try {
            return ObjectValue.of(constructor.newInstance(values));
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalArgumentException("Could not call constructor", exception);
        }
    }

    @Override
    public String toString() {
        return "ConstructorCall(new " + type + "(" + StringUtil.toString(arguments, ", ") + "))";
    }
}
