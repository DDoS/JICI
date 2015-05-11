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
package ca.sapon.jici.evaluator;

import java.util.HashMap;
import java.util.Map;

import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.evaluator.value.type.ValueType;
import ca.sapon.jici.lexer.Identifier;

public class Environment {
    private final Map<String, Class<?>> classes = new HashMap<>();
    private final Map<String, Variable> variables = new HashMap<>();

    public void importClass(Class<?> _class) {
        final String name = _class.getCanonicalName();
        // Validate the type of class
        if (_class.isPrimitive()) {
            throw new IllegalArgumentException("Can't import a primitive type: " + name);
        }
        if (_class.isArray()) {
            throw new IllegalArgumentException("Can't import an array class: " + name);
        }
        if (_class.isAnonymousClass()) {
            throw new IllegalArgumentException("Can't import an anonymous class: " + name);
        }
        // Check for existing import under the same name
        final String simpleName = _class.getSimpleName();
        final Class<?> existing = classes.get(simpleName);
        if (existing != null) {
            throw new IllegalArgumentException("Class " + name + " clashes with existing import " + existing.getCanonicalName());
        }
        // Add the class to the imports
        classes.put(simpleName, _class);
    }

    public Class<?> findClass(Identifier name) {
        return classes.get(name.getSource());
    }

    public Class<?> getClass(Identifier name) {
        final Class<?> _class = findClass(name);
        if (_class == null) {
            throw new IllegalArgumentException("Class " + name.getSource() + " does not exist");
        }
        return _class;
    }

    public boolean hasClass(Identifier name) {
        return classes.containsKey(name.getSource());
    }

    public void declareVariable(Identifier name, ValueType type) {
        declareVariable(name, type, type.getKind().defaultValue());
    }

    public void declareVariable(Identifier name, ValueType type, Value value) {
        final String nameString = name.getSource();
        if (variables.containsKey(nameString)) {
            throw new IllegalArgumentException("Variable " + nameString + " is already declared");
        }
        variables.put(name.getSource(), new Variable(nameString, type, value));
    }

    public ValueType getVariableType(Identifier name) {
        return findVariable(name).getType();
    }

    public Value getVariable(Identifier name) {
        return findVariable(name).getValue();
    }

    public void setVariable(Identifier name, Value value) {
        findVariable(name).setValue(value);
    }

    public boolean hasVariable(Identifier name) {
        return variables.containsKey(name.getSource());
    }

    private Variable findVariable(Identifier name) {
        final String nameString = name.getSource();
        final Variable variable = variables.get(nameString);
        if (variable == null) {
            throw new IllegalArgumentException("Variable " + nameString + " does not exist");
        }
        return variable;
    }

    private static class Variable {
        private final String name;
        private final ValueType type;
        private Value value;

        private Variable(String name, ValueType type, Value value) {
            this.name = name;
            this.type = type;
            this.value = value;
        }

        private String getName() {
            return name;
        }

        private ValueType getType() {
            return type;
        }

        private Value getValue() {
            return value;
        }

        private void setValue(Value value) {
            this.value = value;
        }
    }
}
