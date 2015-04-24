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
import java.util.List;

import ca.sapon.jici.lexer.literal.BooleanLiteral;
import ca.sapon.jici.lexer.literal.CharacterLiteral;
import ca.sapon.jici.lexer.literal.NullLiteral;
import ca.sapon.jici.lexer.literal.StringLiteral;
import ca.sapon.jici.lexer.literal.number.NumberLiteral;
import ca.sapon.jici.util.StringConsumer;

/**
 * A Java lexer. Transforms a source string to a list of {@link ca.sapon.jici.lexer.Token}s.
 */
public class Lexer {
    /**
     * Returns a list of {@link ca.sapon.jici.lexer.Token} lexed from the given source string. This list is composed of identifier, literals (null, integer, long, float, double, char and String),
     * keywords and symbols.
     *
     * @param source The source to lex
     * @return The list of lexed tokens
     * @throws LexerException If the source is malformed. The message identifies the offending character
     */
    public static List<Token> lex(String source) throws LexerException {
        // this builds a list of tokens, which are identifiers, literals and symbols
        final List<Token> tokens = new ArrayList<>();
        final StringConsumer consumer = new StringConsumer(source);
        // traverse the string, attempting to consume tokens
        while (consumer.has()) {
            final int last = consumer.position();
            final char c = consumer.get();
            // tries to generate a token, generates null on failure
            final Token token;
            if (isWhitespace(c)) {
                // ignore all whitespace
                consumeWhitespace(consumer);
                token = null;
            } else if (Character.isJavaIdentifierStart(c)) {
                // try to consume an identifier (starts by a Java identifier)
                consumeIdentifier(consumer);
                final String identifier = consumer.consumed(last);
                // check if it is a null literal
                if (NullLiteral.is(identifier)) {
                    token = NullLiteral.get();
                } else if (BooleanLiteral.is(identifier)) {
                    token = BooleanLiteral.get(identifier);
                } else {
                    // check if it is a keyword
                    final Keyword keyword = Keyword.get(identifier);
                    if (keyword != null) {
                        token = keyword;
                    } else {
                        token = new Identifier(identifier);
                    }
                }
            } else if (Character.isDigit(c)) {
                // consume a number literal (starts with a digit)
                consumeNumberLiteral(consumer);
                token = NumberLiteral.get(consumer.consumed(last));
            } else {
                // try to consume a number literal (floating point can start by a decimal separator)
                if (c == '.' && consumeNumberLiteral(consumer) != last) {
                    token = NumberLiteral.get(consumer.consumed(last));
                } else if (c == '\'') {
                    // consume a character literal (starts with ')
                    consumeCharacterLiteral(consumer);
                    token = new CharacterLiteral(consumer.consumed(last));
                } else if (c == '"') {
                    // consume a string literal (starts with ")
                    consumeStringLiteral(consumer);
                    token = new StringLiteral(consumer.consumed(last));
                } else {
                    // try to consume a char or compound symbol (starts with a symbol)
                    consumeSymbol(consumer);
                    if (consumer.position() != last) {
                        final Symbol symbol = Symbol.get(consumer.consumed(last));
                        // consume comments and drop their starting symbols
                        switch (symbol.getID()) {
                            case SYMBOL_DOUBLE_SLASH:
                                consumeLineComment(consumer);
                                token = null;
                                break;
                            case SYMBOL_SLASH_STAR:
                                consumeBlockComment(consumer);
                                token = null;
                                break;
                            default:
                                token = symbol;
                                break;
                        }
                    } else {
                        // if no symbol is consumed, the char is unknown
                        throw new LexerException("Unknown symbol", source, consumer.position());
                    }
                }
            }
            // only add tokens on success
            if (token != null) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private static void consumeLineComment(StringConsumer consumer) {
        // consume everything until a line terminator is reached
        while (consumer.consume() && !isLineTerminator(consumer.get())) {
        }
    }

    private static void consumeBlockComment(StringConsumer consumer) {
        // consume everything until we hit at "*/", including it
        char c, pc = '\0', ppc = '\0';
        while (consumer.consume()) {
            c = consumer.get();
            if (ppc == '*' && pc == '/') {
                break;
            }
            ppc = pc;
            pc = c;
        }
    }

    private static void consumeWhitespace(StringConsumer consumer) {
        // consume whitespace
        while (consumer.consume() && isWhitespace(consumer.get())) {
        }
    }

    private static void consumeLineTerminator(StringConsumer consumer) {
        // single line terminator no spaces
        final char c = consumer.get();
        if (c == '\n') {
            // LF
            consumer.consume();
        } else if (c == '\r') {
            // CR
            if (consumer.consume() && consumer.get() == '\n') {
                // CR + LF
                consumer.consume();
            }
        }
    }

    private static void consumeIdentifier(StringConsumer consumer) {
        // just java identifier parts
        while (consumer.consume() && Character.isJavaIdentifierPart(consumer.get())) {
        }
    }

    private static int consumeNumberLiteral(StringConsumer consumer) {
        // a string of alphanumeric characters starting with a digit or a decimal point,
        // with the following exceptions
        //   - underscores are allowed between non identifier alphanumerics or underscores
        //   - one decimal separator allowed in the mantissa
        //     - if it begins the number it must be followed by a digit
        //   - a negative or positive sign after an exponent identifier
        //     - the exponent identifier is e or E for decimal
        //     - the exponent identifier is p or P for hexadecimal
        // notes
        //   - prefix signs are handled as operators
        //   - no validation is done on the radix
        char pc = '\0', c;
        boolean inMantissa = true;
        boolean decimalSeparatorFound;
        boolean hexadecimal = false;
        // if the first char is a decimal separator, we need a following digit and we found a decimal separator
        if (isDecimalSeparator(consumer.get())) {
            if (!consumer.consume() || !Character.isDigit(pc = consumer.get())) {
                consumer.expel();
                return consumer.position();
            }
            decimalSeparatorFound = true;
        } else {
            decimalSeparatorFound = false;
        }
        // the main consumer loop, implements the description above
        while (consumer.consume()
                && (Character.isLetterOrDigit(c = consumer.get())
                || isDigitSeparator(c) && canPrecedeDigitSeparator(pc, hexadecimal, inMantissa) && canFollowDigitSeparator(consumer, hexadecimal, inMantissa)
                || isDecimalSeparator(c) && !decimalSeparatorFound && inMantissa
                || isSignIdentifier(c) && isExponentSeparator(pc, hexadecimal))) {
            // check if we found a decimal separator
            if (!decimalSeparatorFound) {
                decimalSeparatorFound = isDecimalSeparator(c);
            }
            // the second char consumed will always be where the hexadecimal identifier is
            if (pc == '\0') {
                hexadecimal = isHexadecimalIdentifier(c);
            }
            // check if we have found an exponent separator and move out of the mantissa
            if (inMantissa && isExponentSeparator(c, hexadecimal)) {
                inMantissa = false;
            }
            pc = c;
        }
        // ignore trailing signs
        if (isSignIdentifier(pc)) {
            consumer.expel();
        }
        return consumer.position();
    }

    private static boolean isSpecialIdentifier(char c, boolean hexadecimal, boolean inMantissa) {
        return isSignIdentifier(c) || isDecimalSeparator(c) || isExponentSeparator(c, hexadecimal)
                || isRadixIdentifier(c) || isTypeIdentifier(c, inMantissa);
    }

    private static boolean isSignIdentifier(char c) {
        return c == '-' || c == '+';
    }

    private static boolean isDecimalSeparator(char c) {
        return c == '.';
    }

    private static boolean isExponentSeparator(char c, boolean hexadecimal) {
        return hexadecimal ? c == 'p' || c == 'P' : c == 'e' || c == 'E';
    }

    private static boolean isDigitSeparator(char c) {
        return c == '_';
    }

    private static boolean isRadixIdentifier(char c) {
        return isHexadecimalIdentifier(c) || isBinaryIdentifier(c);
    }

    private static boolean isHexadecimalIdentifier(char c) {
        return c == 'x' || c == 'X';
    }

    private static boolean isBinaryIdentifier(char c) {
        return c == 'b' || c == 'B';
    }

    private static boolean isTypeIdentifier(char c, boolean inMantissa) {
        return c == 'l' || c == 'L' || !inMantissa && (c == 'f' || c == 'F' || c == 'd' || c == 'D');
    }

    private static boolean canPrecedeDigitSeparator(char c, boolean hexadecimal, boolean inMantissa) {
        return isDigitSeparator(c) || !isSpecialIdentifier(c, hexadecimal, inMantissa);
    }

    private static boolean canFollowDigitSeparator(StringConsumer consumer, boolean hexadecimal, boolean inMantissa) {
        return consumer.has(1) && canPrecedeDigitSeparator(consumer.get(1), hexadecimal, inMantissa);
    }

    private static void consumeCharacterLiteral(StringConsumer consumer) throws LexerException {
        // a string of characters enclosed in '
        consumeEnclosedLiteral(consumer, '\'');
    }

    private static void consumeStringLiteral(StringConsumer consumer) throws LexerException {
        // a string of characters enclosed in "
        consumeEnclosedLiteral(consumer, '"');
    }

    private static void consumeEnclosedLiteral(StringConsumer consumer, char enclosure) throws LexerException {
        char c;
        // if the count of consecutive escapes is odd, escaping is active
        int escapeCount = 0;
        // consume until we find the matching enclosure, ignoring escaped ones
        consumer.consume();
        while (consumer.has()) {
            c = consumer.get();
            if (isLineTerminator(c)) {
                throw new LexerException("Expected '" + enclosure + "'", consumer.string(), consumer.position());
            }
            consumer.consume();
            if (c == enclosure && (escapeCount & 1) == 0) {
                break;
            }
            if (c == '\\') {
                escapeCount++;
            } else {
                escapeCount = 0;
            }
        }
    }

    private static void consumeSymbol(StringConsumer consumer) {
        // attempt to consume compound symbols
        final int start = consumer.position();
        // consume as long as we have a symbol
        do {
            if (!consumer.has()) {
                return;
            }
            consumer.consume();
        } while (Symbol.is(consumer.consumed(start)));
        consumer.expel();
    }

    private static boolean isWhitespace(char c) {
        return isSpace(c) || isLineTerminator(c);
    }

    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\f';
    }

    private static boolean isLineTerminator(char c) {
        return c == '\n' || c == '\r';
    }
}
