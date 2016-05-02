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
package ca.sapon.jici.parser.statement;

import java.util.List;
import java.util.Set;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.lexer.Identifier;
import ca.sapon.jici.util.ReflectionUtil;
import ca.sapon.jici.util.StringUtil;

public class Import implements Statement {
    private final List<Identifier> name;
    private final boolean _package;
    private final int start;
    private final int end;

    public Import(List<Identifier> name, boolean _package, int start, int end) {
        this.name = name;
        this._package = _package;
        this.start = start;
        this.end = end;
    }

    @Override
    public void execute(Environment environment) {
        try {
            if (_package) {
                final Set<Class<?>> classes = ReflectionUtil.findClasses(name);
                if (classes.isEmpty()) {
                    throw new EvaluatorException("No classes in package, perhaps they were loaded by the bootstrap class loader?", this);
                }
                for (Class<?> _class : classes) {
                    environment.importClass(_class);
                }
            } else {
                final Class<?> _class = ReflectionUtil.findClass(name);
                if (_class == null) {
                    throw new EvaluatorException("Class not found: " + StringUtil.toString(name, "."), this);
                }
                environment.importClass(_class);
            }
        } catch (EvaluatorException exception) {
            throw exception;
        } catch (UnsupportedOperationException exception) {
            throw new EvaluatorException(exception.getMessage(), this);
        } catch (Exception exception) {
            throw new EvaluatorException(exception, this);
        }
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
    public String toString() {
        return "Import(" + StringUtil.toString(name, ".") + (_package ? ".*" : "") + ")";
    }
}
