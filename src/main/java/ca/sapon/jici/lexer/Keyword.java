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
package ca.sapon.jici.lexer;

import java.util.HashMap;
import java.util.Map;

public class Keyword extends Token {
    private static final Map<String, Keyword> KEYWORDS = new HashMap<>();

    static {
        // control flow
        add("assert");
        add("if");
        add("else");
        add("while");
        add("do");
        add("for");
        add("break");
        add("continue");
        add("switch");
        add("case");
        add("default");
        add("return");
        add("throw");
        add("try");
        add("catch");
        add("finally");
        // primitive types
        add("void");
        add("boolean");
        add("byte");
        add("short");
        add("char");
        add("int");
        add("long");
        add("float");
        add("double");
        // class
        add("class");
        add("interface");
        add("enum");
        add("abstract");
        add("extends");
        add("implements");
        add("super");
        add("this");
        // package
        add("package");
        add("import");
        // modifiers
        add("strictfp");
        add("transient");
        add("volatile");
        add("public");
        add("protected");
        add("private");
        add("final");
        add("static");
        add("synchronized");
        add("native");
        add("throws");
        // operator
        add("new");
        add("instanceof");
        // unused
        add("goto");
        add("const");
    }

    private Keyword(String source) {
        super(source);
    }

    public static boolean is(String source) {
        return KEYWORDS.containsKey(source);
    }

    public static Keyword get(String source) {
        return KEYWORDS.get(source);
    }

    private static void add(String source) {
        KEYWORDS.put(source, new Keyword(source));
    }
}
