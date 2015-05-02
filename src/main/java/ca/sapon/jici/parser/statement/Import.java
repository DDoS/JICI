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
package ca.sapon.jici.parser.statement;

import java.util.List;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.lexer.Identifier;
import ca.sapon.jici.util.ReflectionUtil;
import ca.sapon.jici.util.StringUtil;

public class Import implements Statement {
    private final List<Identifier> name;
    private final boolean _package;

    public Import(List<Identifier> name, boolean _package) {
        this.name = name;
        this._package = _package;
    }

    @Override
    public void execute(Environment environment) {
        if (_package) {
            throw new  IllegalArgumentException("Package imports are not supported");
        }
        final String nameString = StringUtil.toString(name, ".");
        final Class<?> _class = ReflectionUtil.lookupClass(nameString);
        if (_class == null) {
            throw new IllegalArgumentException("Class not found: " + nameString);
        }
        environment.importClass(_class);
    }

    @Override
    public String toString() {
        return "Import(" + StringUtil.toString(name, ".") + (_package ? ".*" : "") + ")";
    }
}
