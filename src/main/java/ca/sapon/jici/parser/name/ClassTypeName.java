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
package ca.sapon.jici.parser.name;

import java.util.List;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.evaluator.type.ClassType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.lexer.Identifier;
import ca.sapon.jici.util.ReflectionUtil;
import ca.sapon.jici.util.StringUtil;

public class ClassTypeName implements TypeName, ImportedTypeName {
    private final List<Identifier> name;
    private ClassType hint = null;
    private Type type = null;

    public ClassTypeName(List<Identifier> name) {
        this.name = name;
    }

    @Override
    public Type getType(Environment environment) {
        if (type == null) {
            // look for an imported class with possible inner classes
            Class<?> _class = environment.findClass(name.get(0));
            if (_class != null) {
                final int size = name.size();
                for (int i = 1; i < size; i++) {
                    _class = ReflectionUtil.lookupNestedClass(_class, name.get(i).getSource());
                    if (_class == null) {
                        break;
                    }
                }
            }
            // else try for a full class name
            if (_class == null) {
                _class = ReflectionUtil.findClass(name);
            }
            // else try the hint
            if (_class == null && hintNameMatches()) {
                _class = hint.getTypeClass();
                try {
                    environment.importClass(_class);
                } catch (UnsupportedOperationException ignored) {
                }
            }
            // we tried everything, fail
            if (_class == null) {
                throw new EvaluatorException("Class not found: " + toString(), getStart(), getEnd());
            }
            type = ClassType.of(_class);
        }
        return type;
    }

    private boolean hintNameMatches() {
        if (hint == null) {
            return false;
        }
        Class<?> hintClass = hint.getTypeClass();
        for (int i = name.size() - 1; i >= 0; i--) {
            if (hintClass == null || !name.get(i).getSource().equals(hintClass.getSimpleName())) {
                return false;
            }
            hintClass = hintClass.getEnclosingClass();
        }
        return true;
    }

    @Override
    public void setTypeHint(Type hint) {
        if (hint instanceof ClassType) {
            this.hint = (ClassType) hint;
        }
    }

    @Override
    public int getStart() {
        return name.get(0).getIndex();
    }

    @Override
    public int getEnd() {
        return name.get(name.size() - 1).getEnd();
    }

    @Override
    public String toString() {
        return StringUtil.toString(name, ".");
    }
}
