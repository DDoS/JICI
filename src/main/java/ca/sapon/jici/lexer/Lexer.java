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
    private static final Map<String, Keyword> KEYWORDS = new HashMap<>();

    static {
        // punctuation
        addSymbol('.');
        addSymbol(',');
        addSymbol(':');
        addSymbol(';');
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
        // control flow
        addKeyword("assert");
        addKeyword("if");
        addKeyword("else");
        addKeyword("while");
        addKeyword("do");
        addKeyword("for");
        addKeyword("break");
        addKeyword("continue");
        addKeyword("switch");
        addKeyword("case");
        addKeyword("default");
        addKeyword("return");
        addKeyword("throw");
        addKeyword("try");
        addKeyword("catch");
        addKeyword("finally");
        // primitive types
        addKeyword("void");
        addKeyword("boolean");
        addKeyword("byte");
        addKeyword("short");
        addKeyword("char");
        addKeyword("int");
        addKeyword("long");
        addKeyword("float");
        addKeyword("double");
        // class
        addKeyword("class");
        addKeyword("interface");
        addKeyword("enum");
        addKeyword("abstract");
        addKeyword("extends");
        addKeyword("implements");
        addKeyword("super");
        addKeyword("this");
        // package
        addKeyword("package");
        addKeyword("import");
        // modifiers
        addKeyword("strictfp");
        addKeyword("transient");
        addKeyword("volatile");
        addKeyword("public");
        addKeyword("protected");
        addKeyword("private");
        addKeyword("final");
        addKeyword("static");
        addKeyword("synchronized");
        addKeyword("native");
        addKeyword("throws");
        // operator
        addKeyword("new");
        addKeyword("instanceof");
        // unused
        addKeyword("goto");
        addKeyword("const");
    }

    public static List<Token> lex(String source) throws LexerException {
        final List<Token> tokens = new ArrayList<>();

        for (int i = 0, j; i < source.length(); i = j) {
            final char c = source.charAt(i);

            final Token token;
            if (Character.isWhitespace(c)) {
                j = consumeWhitespace(source, i);
                token = null;
            } else if (Character.isJavaIdentifierStart(c)) {
                j = consumeIdentifier(source, i);
                final String identifier = source.substring(i, j);
                final Keyword keyword = KEYWORDS.get(identifier);
                if (keyword != null) {
                    token = keyword;
                } else {
                    token = new Identifier(identifier);
                }
            } else if (Character.isDigit(c)) {
                j = consumeNumberLiteral(source, i);
                token = new NumberLiteral(source.substring(i, j));
            } else {
                if (c == '.') {
                    j = consumeNumberLiteral(source, i);
                    if (i != j) {
                        token = new NumberLiteral(source.substring(i, j));
                    } else {
                        j = i + 1;
                        token = SYMBOLS.get('.');
                    }
                } else if (c == '\'') {
                    j = consumeCharacterLiteral(source, i);
                    token = new CharacterLiteral(source.substring(i, j));
                } else if (c == '"') {
                    j = consumeStringLiteral(source, i);
                    token = new StringLiteral(source.substring(i, j));
                } else {
                    final Symbol symbol = SYMBOLS.get(c);
                    if (symbol != null) {
                        j = i + 1;
                        token = symbol;
                    } else {
                        throw new LexerException("Unknown symbol", source, i);
                    }
                }
            }

            if (token != null) {
                tokens.add(token);
            }
        }

        return tokens;
    }

    private static int consumeWhitespace(String source, int i) {
        // just whitespace
        while (++i < source.length() && Character.isWhitespace(source.charAt(i)));
        return i;
    }

    private static int consumeIdentifier(String source, int i) {
        // just java identifier parts
        while (++i < source.length() && Character.isJavaIdentifierPart(source.charAt(i)));
        return i;
    }

    private static int consumeNumberLiteral(String source, int i) {
        // a string of characters without whitespace or symbols starting with a digit or a decimal,
        // with the following exceptions
        //   - one decimal separator allowed in the mantissa
        //     - if it begins the number it must be followed by a digit
        //   - a negative or positive sign after an exponent identifier for floating point
        //     - the exponent identifier is e or E for decimal
        //     - the exponent identifier is p or P for hexadecimal
        // notes
        //   - prefix signs are handled as operators
        //   - underscores are not symbols
        //   - no validation is done on the radixes
        char pc = '\0', c = '\0';
        boolean inMantissa = true;
        boolean decimalSeparator;
        boolean hexadecimal = false;
        if (source.charAt(i) == '.') {
            if (++i > source.length() || !Character.isDigit(source.charAt(i))) {
                return i - 1;
            }
            decimalSeparator = true;
        } else {
            decimalSeparator = false;
        }
        while (++i < source.length() && !Character.isWhitespace(c = source.charAt(i)) &&
                    (c == '.' && !decimalSeparator && inMantissa ||
                    (c == '-' || c == '+') && (hexadecimal ? pc == 'p' || pc == 'P' : pc == 'e' || pc == 'E') ||
                    !SYMBOLS.containsKey(c))) {
            if (!decimalSeparator) {
                decimalSeparator = c == '.';
            }
            if (pc == '\0') {
                hexadecimal = c == 'x' || c == 'X';
            }
            if (inMantissa && (hexadecimal ? c == 'p' || c == 'P' : c == 'e' || c == 'E')) {
                inMantissa = false;
            }
            pc = c;
        }
        return i;
    }

    private static int consumeCharacterLiteral(String source, int i) {
        // a string of characters enclosed in '
        return consumeEnclosedLiteral(source, i, '\'');
    }

    private static int consumeStringLiteral(String source, int i) {
        // a string of characters enclosed in "
        return consumeEnclosedLiteral(source, i, '"');
    }

    private static int consumeEnclosedLiteral(String source, int i, char enclosure) {
        char pc = '\0', c = '\0';
        while (++i < source.length() && (pc == '\\' || c != enclosure)) {
            pc = c;
            c = source.charAt(i);
        }
        return i;
    }

    private static void addSymbol(char source) {
        SYMBOLS.put(source, new Symbol(source));
    }

    private static void addKeyword(String source) {
        KEYWORDS.put(source, new Keyword(source));
    }

    public static class LexerException extends Exception {
        private static final long serialVersionUID = 1;

        public LexerException(String cause, String source, int index) {
            super(
                cause + " caused by '" + source.charAt(index) + "' at position " + index + "\n" +
                source.substring(Math.max(0, index - 5), Math.min(index + 5, source.length()))
            );
        }
    }
}
