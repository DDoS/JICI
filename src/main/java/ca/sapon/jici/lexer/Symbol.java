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

public class Symbol extends Token {
    private static final Map<String, Symbol> SYMBOLS = new HashMap<>();
    private static final Symbol[] CHAR_SYMBOLS = new Symbol[256];

    static {
        // punctuation
        add(".");
        add(",");
        add(":");
        add(";");
        // math
        add("+");
        add("-");
        add("*");
        add("/");
        add("%");
        add("++");
        add("--");
        // bitwise
        add("&");
        add("|");
        add("~");
        add("^");
        // shift
        add("<<");
        add(">>");
        add(">>>");
        // boolean
        add("!");
        add("&&");
        add("||");
        // comparison
        add("<");
        add(">");
        add(">=");
        add("<=");
        add("==");
        add("!=");
        // assignment
        add("=");
        add("+=");
        add("-=");
        add("*=");
        add("/=");
        add("%=");
        add("&=");
        add("|=");
        add("~=");
        add("^=");
        // enclosing
        add("(");
        add(")");
        add("[");
        add("]");
        add("{");
        add("}");
        // other
        add("?");
        add("..");
        add("...");
    }

    private Symbol(String source) {
        super(source);
    }

    public static boolean is(char source) {
        return CHAR_SYMBOLS[source] != null;
    }

    public static boolean is(String source) {
        if (source.length() == 1) {
            return is(source.charAt(0));
        }
        return SYMBOLS.containsKey(source);
    }

    public static Symbol get(char source) {
        return CHAR_SYMBOLS[source];
    }

    public static Symbol get(String source) {
        if (source.length() == 1) {
            return get(source.charAt(0));
        }
        return SYMBOLS.get(source);
    }

    private static void add(String source) {
        final Symbol symbol = new Symbol(source);
        SYMBOLS.put(source, symbol);
        if (source.length() == 1) {
            CHAR_SYMBOLS[source.charAt(0)] = symbol;
        }
    }
}
