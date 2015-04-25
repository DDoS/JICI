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
package ca.sapon.jici.test;

import java.util.List;

import ca.sapon.jici.lexer.Keyword;
import ca.sapon.jici.lexer.Lexer;
import ca.sapon.jici.lexer.LexerException;
import ca.sapon.jici.lexer.Symbol;
import ca.sapon.jici.lexer.Token;
import ca.sapon.jici.lexer.TokenID;
import ca.sapon.jici.lexer.TokenType;
import org.junit.Assert;
import org.junit.Test;

public class LexerTest {
    @Test
    public void testLexEmpty() throws LexerException {
        Assert.assertEquals(0, Lexer.lex("").size());
    }

    @Test
    public void testLexSpaces() throws LexerException {
        Assert.assertEquals(0, Lexer.lex(" \t\f").size());
    }

    @Test
    public void testLexLineTerminators() throws LexerException {
        Assert.assertEquals(0, Lexer.lex("\r\n").size());
    }

    @Test
    public void testLexIdentifier() throws LexerException {
        testLex(TokenID.IDENTIFIER, "t");
        testLex(TokenID.IDENTIFIER, "test");

        testLex(TokenID.IDENTIFIER, "_");
        testLex(TokenID.IDENTIFIER, "_t");
        testLex(TokenID.IDENTIFIER, "_test");
        testLex(TokenID.IDENTIFIER, "te_st");
        testLex(TokenID.IDENTIFIER, "test_");
        testLex(TokenID.IDENTIFIER, "t_");

        testLex(TokenID.IDENTIFIER, "$");
        testLex(TokenID.IDENTIFIER, "$t");
        testLex(TokenID.IDENTIFIER, "$test");
        testLex(TokenID.IDENTIFIER, "te$st");
        testLex(TokenID.IDENTIFIER, "test$");
        testLex(TokenID.IDENTIFIER, "t$");

        testLex(TokenID.IDENTIFIER, "t1");
        testLex(TokenID.IDENTIFIER, "_1");
        testLex(TokenID.IDENTIFIER, "$1");

        testLex(TokenID.IDENTIFIER, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_$");
    }

    @Test
    public void testLexKeyword() throws LexerException {
        for (Keyword keyword : Keyword.getAll()) {
            testLex(keyword.getID(), keyword.getSource());
        }
    }

    @Test
    public void testLexSymbol() throws LexerException {
        for (Symbol symbol : Symbol.getAll()) {
            if (symbol.getType() != TokenType.COMMENT_DELIMITER) {
                testLex(symbol.getID(), symbol.getSource());
            }
        }
    }

    @Test
    public void testLexBooleanLiteral() throws LexerException {
        testLex(TokenID.LITERAL_TRUE, "true");
        testLex(TokenID.LITERAL_FALSE, "false");
    }

    @Test
    public void testLexCharacterLiteral() throws LexerException {
        testLex(TokenID.LITERAL_CHARACTER, "'a'");
        testLex(TokenID.LITERAL_CHARACTER, "'\\''");
        testLex(TokenID.LITERAL_CHARACTER, "'\\\\'");
        testLex(TokenID.LITERAL_CHARACTER, "'\\n'");
        testLex(TokenID.LITERAL_CHARACTER, "'\\u0061'");
    }

    @Test
    public void testLexStringLiteral() throws LexerException {
        testLex(TokenID.LITERAL_STRING, "\"t\"");
        testLex(TokenID.LITERAL_STRING, "\"test\"");
        testLex(TokenID.LITERAL_STRING, "\"this is a test\"");
        testLex(TokenID.LITERAL_STRING, "\"\\\"\"");
        testLex(TokenID.LITERAL_STRING, "\"\\\\\"");
        testLex(TokenID.LITERAL_STRING, "\"\\n\"");
    }

    @Test
    public void testLexNullLiteral() throws LexerException {
        testLex(TokenID.LITERAL_NULL, "null");
    }

    @Test
    public void testLexDoubleLiteral() throws LexerException {
        testLex(TokenID.LITERAL_DOUBLE, "1.");
        testLex(TokenID.LITERAL_DOUBLE, "1.0");
        testLex(TokenID.LITERAL_DOUBLE, ".1");

        testLex(TokenID.LITERAL_DOUBLE, "1d");
        testLex(TokenID.LITERAL_DOUBLE, "1.d");
        testLex(TokenID.LITERAL_DOUBLE, "1.0d");
        testLex(TokenID.LITERAL_DOUBLE, ".1d");

        testLex(TokenID.LITERAL_DOUBLE, "1e2");
        testLex(TokenID.LITERAL_DOUBLE, "1.e2");
        testLex(TokenID.LITERAL_DOUBLE, "1.0e2");
        testLex(TokenID.LITERAL_DOUBLE, ".1e2");

        testLex(TokenID.LITERAL_DOUBLE, "1e2d");
        testLex(TokenID.LITERAL_DOUBLE, "1.e2d");
        testLex(TokenID.LITERAL_DOUBLE, "1.0e2d");
        testLex(TokenID.LITERAL_DOUBLE, ".1e2d");

        testLex(TokenID.LITERAL_DOUBLE, "1e-2");
        testLex(TokenID.LITERAL_DOUBLE, "1.e-2");
        testLex(TokenID.LITERAL_DOUBLE, "1.0e-2");
        testLex(TokenID.LITERAL_DOUBLE, ".1e-2");

        testLex(TokenID.LITERAL_DOUBLE, "1e-2d");
        testLex(TokenID.LITERAL_DOUBLE, "1.e-2d");
        testLex(TokenID.LITERAL_DOUBLE, "1.0e-2d");
        testLex(TokenID.LITERAL_DOUBLE, ".1e-2d");

        testLex(TokenID.LITERAL_DOUBLE, "0x1p2");
        testLex(TokenID.LITERAL_DOUBLE, "0x1.p2");
        testLex(TokenID.LITERAL_DOUBLE, "0x.fp2");
        testLex(TokenID.LITERAL_DOUBLE, "0x1.fp2");

        testLex(TokenID.LITERAL_DOUBLE, "0x1p2d");
        testLex(TokenID.LITERAL_DOUBLE, "0x1.p2d");
        testLex(TokenID.LITERAL_DOUBLE, "0x.fp2d");
        testLex(TokenID.LITERAL_DOUBLE, "0x1.fp2d");

        testLex(TokenID.LITERAL_DOUBLE, "1D");
        testLex(TokenID.LITERAL_DOUBLE, "1E2");
        testLex(TokenID.LITERAL_DOUBLE, "1E2D");
        testLex(TokenID.LITERAL_DOUBLE, "0X1P2");
        testLex(TokenID.LITERAL_DOUBLE, "0X1P2D");

        testLex(TokenID.LITERAL_DOUBLE, "1234567890d");
    }

    @Test
    public void testLexFloatLiteral() throws LexerException {
        testLex(TokenID.LITERAL_FLOAT, "1f");
        testLex(TokenID.LITERAL_FLOAT, "1.f");
        testLex(TokenID.LITERAL_FLOAT, "1.0f");
        testLex(TokenID.LITERAL_FLOAT, ".1f");

        testLex(TokenID.LITERAL_FLOAT, "1e2f");
        testLex(TokenID.LITERAL_FLOAT, "1.e2f");
        testLex(TokenID.LITERAL_FLOAT, "1.0e2f");
        testLex(TokenID.LITERAL_FLOAT, ".1e2f");

        testLex(TokenID.LITERAL_FLOAT, "1e-2f");
        testLex(TokenID.LITERAL_FLOAT, "1.e-2f");
        testLex(TokenID.LITERAL_FLOAT, "1.0e-2f");
        testLex(TokenID.LITERAL_FLOAT, ".1e-2f");

        testLex(TokenID.LITERAL_FLOAT, "0x1p2f");
        testLex(TokenID.LITERAL_FLOAT, "0x1.p2f");
        testLex(TokenID.LITERAL_FLOAT, "0x.fp2f");
        testLex(TokenID.LITERAL_FLOAT, "0x1.fp2f");

        testLex(TokenID.LITERAL_FLOAT, "1F");
        testLex(TokenID.LITERAL_FLOAT, "1E2F");
        testLex(TokenID.LITERAL_FLOAT, "0X1P2F");

        testLex(TokenID.LITERAL_FLOAT, "1234567890f");
    }

    @Test
    public void testLexIntLiteral() throws LexerException {
        testLex(TokenID.LITERAL_INT, "1");

        testLex(TokenID.LITERAL_INT, "0x1");
        testLex(TokenID.LITERAL_INT, "0X1F");

        testLex(TokenID.LITERAL_INT, "0b1");
        testLex(TokenID.LITERAL_INT, "0B11");

        testLex(TokenID.LITERAL_INT, "01");
        testLex(TokenID.LITERAL_INT, "017");

        testLex(TokenID.LITERAL_INT, "1234567890");
    }

    @Test
    public void testLexLongLiteral() throws LexerException {
        testLex(TokenID.LITERAL_LONG, "1l");

        testLex(TokenID.LITERAL_LONG, "0x1l");
        testLex(TokenID.LITERAL_LONG, "0X1Fl");

        testLex(TokenID.LITERAL_LONG, "0b1l");
        testLex(TokenID.LITERAL_LONG, "0B11l");

        testLex(TokenID.LITERAL_LONG, "01l");
        testLex(TokenID.LITERAL_LONG, "017l");

        testLex(TokenID.LITERAL_LONG, "1L");
        testLex(TokenID.LITERAL_LONG, "0X1L");
        testLex(TokenID.LITERAL_LONG, "0B1L");
        testLex(TokenID.LITERAL_LONG, "017L");

        testLex(TokenID.LITERAL_LONG, "1234567890l");
    }

    @Test
    public void testLexComments() throws LexerException {
        Assert.assertEquals(0, Lexer.lex("//abcd").size());
        Assert.assertEquals(0, Lexer.lex("//abcd\n").size());
        Assert.assertEquals(0, Lexer.lex("//abcd\r").size());
        Assert.assertEquals(0, Lexer.lex("//abcd\r\n").size());

        Assert.assertEquals(0, Lexer.lex("/*abcd").size());
        Assert.assertEquals(0, Lexer.lex("/*abcd*/").size());

        assertEquals(TokenID.IDENTIFIER, "a", Lexer.lex("//0\na"));
        assertEquals(TokenID.IDENTIFIER, "a", Lexer.lex("//0\ra"));
        assertEquals(TokenID.IDENTIFIER, "a", Lexer.lex("//0\r\na"));
        assertEquals(TokenID.IDENTIFIER, "a", Lexer.lex("/*0*/a"));

        assertEquals(TokenID.IDENTIFIER, "a", Lexer.lex("///*\na"));
        assertEquals(TokenID.IDENTIFIER, "a", Lexer.lex("/*//*/a"));
    }

    @Test
    public void testLexUnknownCharacter() {
        try {
            testLex(null, "#");
            Assert.fail();
        } catch (LexerException ignored) {
        }
        try {
            testLex(null, "te#st");
            Assert.fail();
        } catch (LexerException ignored) {
        }
    }

    private void testLex(TokenID expectedID, String source) throws LexerException {
        assertEquals(expectedID, source, Lexer.lex(source));
    }

    private void assertEquals(TokenID expectedID, String expectedSource, List<Token> actual) {
        Assert.assertEquals("Expected one token, got many", 1, actual.size());
        assertEquals(expectedID, expectedSource, actual.get(0));
    }

    private void assertEquals(TokenID expectedID, String expectedSource, Token actual) {
        Assert.assertEquals("Expected ID didn't match actual ID", expectedID, actual.getID());
        Assert.assertEquals("Expected source didn't match actual source", expectedSource, actual.getSource());
    }
}
