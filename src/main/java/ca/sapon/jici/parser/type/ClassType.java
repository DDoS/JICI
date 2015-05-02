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
package ca.sapon.jici.parser.type;

import java.util.List;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.lexer.Identifier;
import ca.sapon.jici.util.StringUtil;

public class ClassType implements Type {
    private final List<Identifier> type;
    private Class<?> _class = null;

    public ClassType(List<Identifier> type) {
        this.type = type;
    }

    @Override
    public Class<?> getTypeClass(Environment environment) {
        if (_class == null) {
            _class = environment.findClass(type.get(0));
            if (_class == null) {
                final StringBuilder name = new StringBuilder();
                for (Identifier identifier : type) {
                    name.append(identifier.getSource());
                    if ((_class = lookupName(name.toString())) != null) {
                        return _class;
                    }
                    name.append('.');
                }
                throw new IllegalArgumentException("Class not found: " + toString());
            }
        }
        return _class;
    }

    @Override
    public String toString() {
        return StringUtil.toString(type, ".");
    }

    private static Class<?> lookupName(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException exception) {
            return null;
        }
    }
}
