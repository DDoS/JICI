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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class Lexer {
    private static final Map<Character, Symbol> SYMBOLS = new HashMap<>();

    static {
        // punctuation
        addSymbol('.');
        addSymbol(',');
        addSymbol(':');
        addSymbol(';');
        addSymbol('"');
        addSymbol('\'');
        // math
        addSymbol('=');
        addSymbol('+');
        addSymbol('-');
        addSymbol('*');
        addSymbol('/');
        addSymbol('%');
        // logical
        addSymbol('!');
        addSymbol('&');
        addSymbol('|');
        addSymbol('~');
        addSymbol('^');
        addSymbol('<');
        addSymbol('>');
        addSymbol('?');
        // enclosing
        addSymbol('(');
        addSymbol(')');
        addSymbol('[');
        addSymbol(']');
        addSymbol('{');
        addSymbol('}');
        // other
        addSymbol('\\');
    }

    public static List<Token> lex(String source) {
        final List<Token> tokens = new ArrayList<>();

        for (int i = 0, j; i < source.length(); i = j) {
            final char c = source.charAt(i);

            if (Character.isWhitespace(c)) {
                j = consumeWhitespace(source, i);
            } else if (Character.isLetter(c)) {
                j = consumeLetters(source, i);
                tokens.add(new Text(source.substring(i, j)));
            } else if (Character.isDigit(c)) {
                j = consumeDigits(source, i);
                tokens.add(new Number(source.substring(i, j)));
            } else {
                Symbol symbol = SYMBOLS.get(c);
                if (symbol != null) {
                    tokens.add(symbol);
                }
                j = i + 1;
            }
        }

        return tokens;
    }

    private static int consumeWhitespace(String source, int i) {
        while (++i < source.length() && Character.isWhitespace(source.charAt(i)));
        return i;
    }

    private static int consumeLetters(String source, int i) {
        while (++i < source.length() && Character.isLetterOrDigit(source.charAt(i)));
        return i;
    }

    private static int consumeDigits(String source, int i) {
        while (++i < source.length() && Character.isDigit(source.charAt(i)));
        return i;
    }

    private static void addSymbol(char source) {
        SYMBOLS.put(source, new Symbol(source));
    }
}
