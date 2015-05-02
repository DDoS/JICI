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
package ca.sapon.jici.evaluator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.evaluator.value.ValueKind;
import ca.sapon.jici.lexer.Identifier;

public class Environment {
    private final Map<String, Class<?>> classes = new HashMap<>();
    private final Map<String, Value> variables = new HashMap<>();

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

    public Value findVariable(Identifier name) {
        return variables.get(name.getSource());
    }

    public void declareVariable(Identifier name, ValueKind kind) {
        final Value existing = findVariable(name);
        if (existing != null) {
            throw new IllegalArgumentException("Variable " + name.getSource() + " is already declared");
        }
        variables.put(name.getSource(), kind.defaultValue());
    }

    public Value getVariable(Identifier name) {
        final Value variable = findVariable(name);
        if (variable == null) {
            throw new IllegalArgumentException("Variable " + name.getSource() + " does not exist");
        }
        return variable;
    }

    public void setVariable(Identifier name, Value value) {
        if (findVariable(name) == null) {
            throw new IllegalArgumentException("Variable " + name.getSource() + " does not exist");
        }
        variables.put(name.getSource(), value);
    }
}
