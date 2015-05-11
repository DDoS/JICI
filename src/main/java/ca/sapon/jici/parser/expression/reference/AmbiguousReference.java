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

import java.util.ArrayList;
import java.util.List;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.value.ObjectValue;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.evaluator.value.type.ObjectValueType;
import ca.sapon.jici.evaluator.value.type.PrimitiveValueType;
import ca.sapon.jici.evaluator.value.type.ValueType;
import ca.sapon.jici.lexer.Identifier;
import ca.sapon.jici.parser.expression.Expression;
import ca.sapon.jici.util.ReflectionUtil;
import ca.sapon.jici.util.StringUtil;

public class AmbiguousReference implements Reference {
    private final List<Identifier> name;
    private ValueType valueType = null;
    private Reference reference = null;

    public AmbiguousReference(List<Identifier> name) {
        this.name = name;
    }

    @Override
    public ValueType geValueType(Environment environment) {
        if (valueType == null) {
            reference = (Reference) disambiguate(environment, name);
            valueType = reference.geValueType(environment);
        }
        return valueType;
    }

    @Override
    public Value getValue(Environment environment) {
        return reference.getValue(environment);
    }

    @Override
    public void setValue(Environment environment, Value value) {
        reference.setValue(environment, value);
    }

    @Override
    public String toString() {
        return StringUtil.toString(name, ".");
    }

    public static Expression disambiguate(Environment environment, List<Identifier> name) {
        Expression resolved;
        final List<Identifier> accesses;
        final Identifier identifier = name.get(0);
        // variable followed by one or more field accesses
        // class followed by one or more field accesses
        if (environment.hasVariable(identifier)) {
            resolved = new VariableAccess(identifier);
            accesses = name.subList(1, name.size());
        } else {
            Class<?> _class = environment.findClass(identifier);
            if (_class != null) {
                resolved = new StaticAccess(_class);
                accesses = name.subList(1, name.size());
            } else {
                accesses = new ArrayList<>(name);
                _class = ReflectionUtil.disambiguateClass(accesses);
                if (_class == null) {
                    throw new IllegalArgumentException("Unknown identifier " + StringUtil.toString(name, "."));
                }
                resolved = new StaticAccess(_class);
            }
        }
        for (Identifier access : accesses) {
            resolved = new FieldAccess(resolved, access);
        }
        return resolved;
    }

    private static class StaticAccess implements Expression {
        private final Class<?> _class;
        private ValueType valueType = null;

        private StaticAccess(Class<?> _class) {
            this._class = _class;
        }

        @Override
        public ValueType geValueType(Environment environment) {
            if (valueType == null) {
                valueType = _class.isPrimitive() ? PrimitiveValueType.of(_class) : new ObjectValueType(_class);
            }
            return valueType;
        }

        @Override
        public Value getValue(Environment environment) {
            return ObjectValue.of(null);
        }

        @Override
        public String toString() {
            return _class.getCanonicalName();
        }
    }
}
