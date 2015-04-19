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

import ca.sapon.jici.lexer.literal.BooleanLiteral;
import ca.sapon.jici.lexer.literal.CharacterLiteral;
import ca.sapon.jici.lexer.literal.NullLiteral;
import ca.sapon.jici.lexer.literal.StringLiteral;
import ca.sapon.jici.lexer.literal.number.NumberLiteral;

import java.util.ArrayList;
import java.util.List;

/**
 * A Java lexer. Transforms a source string to a list of {@link ca.sapon.jici.lexer.Token}s.
 */
public class Lexer {

    /**
     * Returns a list of {@link ca.sapon.jici.lexer.Token} lexed from the given source string.
     * This list is composed of identifier, literals (null, integer, long, float, double, char and String), keywords and symbols.
     *
     * @param source The source to lex
     * @return The list of lexed tokens
     * @throws LexerException If the source is malformed. The message identifies the offending character
     */
    public static List<Token> lex(String source) throws LexerException {
        // this builds a list of tokens, which are identifiers, literals and symbols
        final List<Token> tokens = new ArrayList<>();
        // traverse the string, attempting to consume tokens
        final int length = source.length();
        for (int i = 0, j; i < length; i = j) {
            final char c = source.charAt(i);
            // tries to generate a token, generates null on failure
            final Token token;
            if (isSpace(c)) {
                // ignore all spaces
                j = consumeSpaces(source, i);
                token = null;
            } else if (isLineTerminator(c)) {
                // consume a line terminator
                j = consumeLineTerminator(source, i);
                token = null;
            } else if (Character.isJavaIdentifierStart(c)) {
                // try to consume an identifier (starts by a Java identifier)
                j = consumeIdentifier(source, i);
                final String identifier = source.substring(i, j);
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
                j = consumeNumberLiteral(source, i);
                token = NumberLiteral.get(source.substring(i, j));
            } else {
                // try to consume a number literal (floating point can start by a decimal separator)
                if (c == '.' && i != (j = consumeNumberLiteral(source, i))) {
                    token = NumberLiteral.get(source.substring(i, j));
                } else if (c == '\'') {
                    // consume a character literal (starts with ')
                    j = consumeCharacterLiteral(source, i);
                    token = new CharacterLiteral(source.substring(i, j));
                } else if (c == '"') {
                    // consume a string literal (starts with ")
                    j = consumeStringLiteral(source, i);
                    token = new StringLiteral(source.substring(i, j));
                } else {
                    // try to consume a char or compound symbol (starts with a symbol)
                    j = consumeSymbol(source, i);
                    if (i != j) {
                        token = Symbol.get(source.substring(i, j));
                    } else {
                        // if no symbol is consumed, the char is unknown
                        throw new LexerException("Unknown symbol", source, i);
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

    /*
     * These are the consumers, they take the source and the index of the first char to consume and return
     * the index + 1 of the last consumed char. If the returned index is the same nothing was consumed.
     */

    private static int consumeWhitespace(String source, int i) {
        // just whitespace
        while (++i < source.length() && Character.isWhitespace(source.charAt(i)));
    private static int consumeSpaces(String source, int i) {
        // consume spaces but not line terminators
        while (++i < source.length() && isSpace(source.charAt(i)));
        return i;
    }

    private static int consumeLineTerminator(String source, int i) {
        // line terminators, but not spaces
        char c = source.charAt(i);
        // LF
        if (c == '\n') {
            return i + 1;
        }
        // CR
        if (c == '\r') {
            // CR + LF
            if (++i < source.length() && source.charAt(i) == '\n') {
                return i + 1;
            }
        }
        return i;
    }

    private static int consumeIdentifier(String source, int i) {
        // just java identifier parts
        while (++i < source.length() && Character.isJavaIdentifierPart(source.charAt(i)));
        return i;
    }

    private static int consumeNumberLiteral(String source, int i) {
        // a string of alphanimeric characters starting with a digit or a decimal point,
        // with the following exceptions
        //   - underscores are allowed between non identifier alphanumerics or underscores
        //   - one decimal separator allowed in the mantissa
        //     - if it begins the number it must be followed by a digit
        //   - a negative or positive sign after an exponent identifier
        //     - the exponent identifier is e or E for decimal
        //     - the exponent identifier is p or P for hexadecimal
        // notes
        //   - prefix signs are handled as operators
        //   - no validation is done on the radixes
        char pc = '\0', c = '\0';
        boolean inMantissa = true;
        boolean decimalSeparatorFound;
        boolean hexadecimal = false;
        // if the first char is a decimal separator, we need a following digit and we found a decimal separator
        if (isDecimalSeparator(source.charAt(i))) {
            if (++i >= source.length() || !Character.isDigit(pc = source.charAt(i))) {
                return i - 1;
            }
            decimalSeparatorFound = true;
        } else {
            decimalSeparatorFound = false;
        }
        // the main consumer loop, implements the description above
        while (++i < source.length()
                && (Character.isLetterOrDigit(c = source.charAt(i))
                    || isDigitSeparator(c)
                        && canPrecedeDigitSeparator(pc, hexadecimal, inMantissa)
                        && canFollowDigitSeparator(source, i + 1, hexadecimal, inMantissa)
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
        return isSignIdentifier(pc) ? i - 1 : i;
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

    private static boolean canFollowDigitSeparator(String source, int i, boolean hexadecimal, boolean inMantissa) {
        return i < source.length() && canPrecedeDigitSeparator(source.charAt(i), hexadecimal, inMantissa);
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
        char c;
        // if the count of consecutive escapes is odd, escaping is active
        int escapeCount = 0;
        // consume until we find the matching enclosure, ignoring escaped ones
        i++;
        while (i < source.length()) {
            c = source.charAt(i++);
            if (c == enclosure && (escapeCount & 1) == 0) {
                break;
            }
            if (c == '\\') {
                escapeCount++;
            } else {
                escapeCount = 0;
            }
        }
        return i;
    }

    private static int consumeSymbol(String source, int i) {
        // attempt to consume compound symbols
        int j = i;
        // stop when we no longer have a symbol, and return the previous index
        while (Symbol.is(source.substring(i, j + 1))) {
            if (++j >= source.length()) {
                break;
            }
        }
        return j;
    }

    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\f';
    }

    private static boolean isLineTerminator(char c) {
        return c == '\n' || c == '\r';
    }
}
