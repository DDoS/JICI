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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.value.ObjectValue;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.evaluator.value.type.ValueType;
import ca.sapon.jici.parser.statement.Statement;
import ca.sapon.jici.parser.type.ClassType;
import ca.sapon.jici.util.ReflectionUtil;
import ca.sapon.jici.util.StringUtil;

public class ConstructorCall implements Statement, Expression {
    private final ClassType type;
    private final List<Expression> arguments;
    private ValueType valueType = null;
    private Constructor<?> constructor = null;

    public ConstructorCall(ClassType type, List<Expression> arguments) {
        this.arguments = arguments;
        this.type = type;
    }

    @Override
    public void execute(Environment environment) {
        geValueType(environment);
        getValue(environment);
    }

    @Override
    public ValueType geValueType(Environment environment) {
        if (valueType == null) {
            // get types
            valueType = type.getValueType(environment);
            final int size = arguments.size();
            final ValueType[] argumentTypes = new ValueType[size];
            for (int i = 0; i < size; i++) {
                argumentTypes[i] = arguments.get(i).geValueType(environment);
            }
            // try to find a matching constructor
            final Constructor<?>[] constructors = valueType.getClassType().getConstructors();
            // look for matches in length
            final Map<Constructor<?>, Class<?>[]> candidates = new HashMap<>();
            for (Constructor<?> candidate : constructors) {
                final Class<?>[] parameterTypes = candidate.getParameterTypes();
                if (parameterTypes.length == size) {
                    candidates.put(candidate, parameterTypes);
                }
            }
            // remove constructors with un-applicable parameters and look for perfect matches
            candidates:
            for (Iterator<Entry<Constructor<?>, Class<?>[]>> iterator = candidates.entrySet().iterator(); iterator.hasNext(); ) {
                final Entry<Constructor<?>, Class<?>[]> entry = iterator.next();
                final Class<?>[] parameters = entry.getValue();
                for (int i = 0; i < parameters.length; i++) {
                    final ValueType argument = argumentTypes[i];
                    final Class<?> parameter = parameters[i];
                    if (!argument.convertibleTo(parameter)) {
                        iterator.remove();
                        continue candidates;
                    }
                    if (argument.getClassType() != parameter) {
                        continue candidates;
                    }
                }
                constructor = entry.getKey();
                return valueType;
            }
            // remove constructors with the corresponding wider types
            int candidateCount = candidates.size();
            candidates:
            for (final Entry<Constructor<?>, Class<?>[]> entry : candidates.entrySet()) {
                final Class<?>[] parameters = entry.getValue();
                for (Class<?>[] challenges : candidates.values()) {
                    for (int i = 0; i < parameters.length; i++) {
                        // remove when the challenge is narrow than the parameter
                        if (ReflectionUtil.isNarrower(challenges[i], parameters[i])) {
                            candidateCount--;
                            continue candidates;
                        }
                    }
                }
                // cache the candidate because getting a single element from a set is awkward
                constructor = entry.getKey();
            }
            if (candidateCount != 1 || constructor == null) {
                constructor = null;
                throw new IllegalArgumentException("No constructor for signature: " + valueType.getName() + "(" + StringUtil.toString(Arrays.asList(argumentTypes), ", ") + ")");
            }
        }
        return valueType;
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
