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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.evaluator.type.LiteralReferenceType;
import ca.sapon.jici.evaluator.type.LiteralType;
import ca.sapon.jici.evaluator.type.ParametrizedType;
import ca.sapon.jici.evaluator.type.ReferenceType;
import ca.sapon.jici.evaluator.type.TypeArgument;
import ca.sapon.jici.lexer.Identifier;
import ca.sapon.jici.util.ReflectionUtil;
import ca.sapon.jici.util.StringUtil;
import ca.sapon.jici.util.TypeUtil;

public class ClassTypeName implements TypeName {
    private final List<Identifier> name;
    private final List<TypeArgumentName> arguments;
    private ReferenceType hint = null;
    private LiteralReferenceType type = null;

    public ClassTypeName(List<Identifier> name) {
        this(name, Collections.<TypeArgumentName>emptyList());
    }

    public ClassTypeName(List<Identifier> name, List<TypeArgumentName> arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    @Override
    public LiteralType getType(Environment environment) {
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
            if (_class == null && hint != null) {
                final Class<?> match = TypeUtil.findNameMatch(hint, name);
                if (match == null) {
                    // failed, discard hint
                    hint = null;
                } else {
                    _class = match;
                    try {
                        environment.importClass(_class);
                    } catch (UnsupportedOperationException ignored) {
                    }
                }
            }
            // we tried everything, fail
            if (_class == null) {
                throw new EvaluatorException("Class not found: " + toString(), getStart(), getEnd());
            }
            if (!arguments.isEmpty()) {
                final List<TypeArgument> classes = new ArrayList<>(arguments.size());
                for (TypeArgumentName parameter : arguments) {
                    classes.add(parameter.getType(environment));
                }
                type = ParametrizedType.of(_class, classes);
            } else {
                type = LiteralReferenceType.of(_class);
            }
        }
        return type;
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
        return StringUtil.toString(name, ".") + (!arguments.isEmpty() ? "<" + StringUtil.toString(arguments, ", ") + ">" : "");
    }
}
