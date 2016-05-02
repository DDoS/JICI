/*
 * This file is part of JICI, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015-2016 Aleksi Sapon <http://sapon.ca/jici/>
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
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.value.ObjectValue;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.lexer.Identifier;
import ca.sapon.jici.parser.expression.Expression;
import ca.sapon.jici.util.ReflectionUtil;
import ca.sapon.jici.util.StringUtil;
import ca.sapon.jici.util.TypeUtil;

public class AmbiguousReference implements Reference {
    private final List<Identifier> name;
    private int start;
    private int end;
    private Reference reference = null;

    public AmbiguousReference(List<Identifier> name) {
        this.name = name;
        start = name.get(0).getStart();
        end = name.get(name.size() - 1).getEnd();
    }

    private Reference getReference(Environment environment) {
        if (reference != null) {
            return reference;
        }
        final Expression resolved = disambiguate(environment, name);
        if (!(resolved instanceof Reference)) {
            throw new EvaluatorException("Not a reference", resolved);
        }
        return reference = (Reference) resolved;
    }

    @Override
    public Type getType(Environment environment) {
        return getReference(environment).getType(environment);
    }

    @Override
    public Type getTargetType(Environment environment) {
        return getReference(environment).getTargetType(environment);
    }

    @Override
    public Value getValue(Environment environment) {
        return getReference(environment).getValue(environment);
    }

    @Override
    public void setValue(Environment environment, Value value) {
        getReference(environment).setValue(environment, value);
    }

    @Override
    public int getStart() {
        return start;
    }

    @Override
    public int getEnd() {
        return end;
    }

    @Override
    public void setStart(int start) {
        this.start = start;
    }

    @Override
    public void setEnd(int end) {
        this.end = end;
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
                resolved = new StaticAccess(_class, identifier.getStart(), identifier.getEnd());
                accesses = name.subList(1, name.size());
            } else {
                accesses = new ArrayList<>(name);
                _class = ReflectionUtil.disambiguateClass(accesses);
                final int start = identifier.getStart();
                if (_class == null) {
                    throw new EvaluatorException("Unknown identifier " + StringUtil.toString(name, "."), start, name.get(name.size() - 1).getEnd());
                }
                resolved = new StaticAccess(_class, start, accesses.isEmpty() ? name.get(name.size() - 1).getEnd() : accesses.get(0).getStart());
            }
        }
        for (Identifier access : accesses) {
            resolved = new FieldAccess(resolved, access);
        }
        return resolved;
    }

    public static class StaticAccess implements Expression {
        private final Class<?> _class;
        private int start;
        private int end;
        private Type type = null;

        private StaticAccess(Class<?> _class, int start, int end) {
            this._class = _class;
            this.start = start;
            this.end = end;
        }

        @Override
        public Type getType(Environment environment) {
            if (type == null) {
                type = TypeUtil.wrap(_class);
            }
            return type;
        }

        @Override
        public Value getValue(Environment environment) {
            return ObjectValue.of(null);
        }

        @Override
        public int getStart() {
            return start;
        }

        @Override
        public int getEnd() {
            return end;
        }

        @Override
        public void setStart(int start) {
            this.start = start;
        }

        @Override
        public void setEnd(int end) {
            this.end = end;
        }

        @Override
        public String toString() {
            return _class.getCanonicalName();
        }
    }
}
