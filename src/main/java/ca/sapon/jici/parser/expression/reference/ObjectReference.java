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
package ca.sapon.jici.parser.expression.reference;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.evaluator.value.type.ValueType;
import ca.sapon.jici.lexer.Identifier;
import ca.sapon.jici.parser.expression.Expression;
import ca.sapon.jici.util.ReflectionUtil;
import ca.sapon.jici.util.StringUtil;

public class ObjectReference implements Reference {
    private final Expression object;
    private ValueType valueType = null;

    public ObjectReference(Expression object) {
        this.object = object;
    }

    @Override
    public ValueType geValueType(Environment environment) {
        if (valueType == null) {
            valueType = object.geValueType(environment);
        }
        return valueType;
    }

    @Override
    public Value getValue(Environment environment) {
        return object.getValue(environment);
    }

    @Override
    public void setValue(Environment environment, Value value) {
        throw new IllegalArgumentException("Can't set the value of an object reference");
    }

    @Override
    public Field getField(Environment environment, Identifier name) {
        return null;
    }

    @Override
    public Method getMethod(Environment environment, Identifier name, ValueType[] arguments) {
        final String nameString = name.getSource();
        // try to find a matching method
        final Method[] methods = geValueType(environment).getTypeClass().getMethods();
        // look for matches in length and name
        final Map<Method, Class<?>[]> candidates = new HashMap<>();
        for (Method candidate : methods) {
            if (candidate.getName().equals(nameString)) {
                final Class<?>[] parameterTypes = candidate.getParameterTypes();
                if (parameterTypes.length == arguments.length) {
                    candidates.put(candidate, parameterTypes);
                }
            }
        }
        // try to resolve the overloads
        final Method method = ReflectionUtil.resolveOverloads(candidates, arguments);
        if (method == null) {
            throw new IllegalArgumentException("No method for signature: " + nameString + "(" + StringUtil.toString(Arrays.asList(arguments), ", ") + ")");
        }
        return method;
    }

    @Override
    public String toString() {
        return object.toString();
    }
}
