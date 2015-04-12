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

import ca.sapon.jici.lexer.TokenID;

public class Symbol extends Token {
    private static final Map<String, Symbol> SYMBOLS = new HashMap<>();
    private static final Symbol[] CHAR_SYMBOLS = new Symbol[256];

    static {
        // punctuation
        add(TokenID.SYMBOL_PERIOD, ".");
        add(TokenID.SYMBOL_COMMA, ",");
        add(TokenID.SYMBOL_COLON, ":");
        add(TokenID.SYMBOL_SEMICOLON, ";");
        // math
        add(TokenID.SYMBOL_PLUS, "+");
        add(TokenID.SYMBOL_MINUS, "-");
        add(TokenID.SYMBOL_TIMES, "*");
        add(TokenID.SYMBOL_DIVIDE, "/");
        add(TokenID.SYMBOL_MODULO, "%");
        add(TokenID.SYMBOL_INCREMENT, "++");
        add(TokenID.SYMBOL_DECREMENT, "--");
        // bitwise
        add(TokenID.SYMBOL_BITWISE_AND, "&");
        add(TokenID.SYMBOL_BITWISE_OR, "|");
        add(TokenID.SYMBOL_BITWISE_NOT, "~");
        add(TokenID.SYMBOL_BITWISE_XOR, "^");
        // shift
        add(TokenID.SYMBOL_LOGICAL_LEFT_SHIFT, "<<");
        add(TokenID.SYMBOL_ARITHMETIC_RIGHT_SHIFT, ">>");
        add(TokenID.SYMBOL_LOGICAL_RIGHT_SHIFT, ">>>");
        // boolean
        add(TokenID.SYMBOL_BOOLEAN_NOT, "!");
        add(TokenID.SYMBOL_BOOLEAN_AND, "&&");
        add(TokenID.SYMBOL_BOOLEAN_OR, "||");
        // comparison
        add(TokenID.SYMBOL_LESSER, "<");
        add(TokenID.SYMBOL_GREATER, ">");
        add(TokenID.SYMBOL_GREATER_OR_EQUAL, ">=");
        add(TokenID.SYMBOL_LESSER_OR_EQUAL, "<=");
        add(TokenID.SYMBOL_EQUAL, "==");
        add(TokenID.SYMBOL_NOT_EQUAL, "!=");
        // assignment
        add(TokenID.SYMBOL_ASSIGN, "=");
        add(TokenID.SYMBOL_ADD_ASSIGN, "+=");
        add(TokenID.SYMBOL_SUBTRACT_ASSIGN, "-=");
        add(TokenID.SYMBOL_MULTIPLY_ASSIGN, "*=");
        add(TokenID.SYMBOL_DIVIDE_ASSIGN, "/=");
        add(TokenID.SYMBOL_REMAINDER_ASSIGN, "%=");
        add(TokenID.SYMBOL_BITWISE_AND_ASSIGN, "&=");
        add(TokenID.SYMBOL_BITWISE_OR_ASSIGN, "|=");
        add(TokenID.SYMBOL_BITWISE_NOT_ASSIGN, "~=");
        add(TokenID.SYMBOL_BITWISE_XOR_ASSIGN, "^=");
        // enclosing
        add(TokenID.SYMBOL_OPEN_PARENTHESIS, "(");
        add(TokenID.SYMBOL_CLOSE_PARENTHESIS, ")");
        add(TokenID.SYMBOL_OPEN_BRACKET, "[");
        add(TokenID.SYMBOL_CLOSE_BRACKET, "]");
        add(TokenID.SYMBOL_OPEN_BRACE, "{");
        add(TokenID.SYMBOL_CLOSE_BRACE, "}");
        // other
        add(TokenID.SYMBOL_QUESTION_MARK, "?");
        add(TokenID.SYMBOL_DOUBLE_PERIOD, "..");
        add(TokenID.SYMBOL_TRIPLE_PERIOD, "...");
        add(TokenID.SYMBOL_ARROW, "->");
        add(TokenID.SYMBOL_AT, "@");
    }

    private Symbol(TokenID id, String source) {
        super(id, source);
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

    private static void add(TokenID id, String source) {
        final Symbol symbol = new Symbol(id, source);
        SYMBOLS.put(source, symbol);
        if (source.length() == 1) {
            CHAR_SYMBOLS[source.charAt(0)] = symbol;
        }
    }
}
