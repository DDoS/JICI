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
package ca.sapon.jici.parser;

import ca.sapon.jici.lexer.Identifier;
import ca.sapon.jici.lexer.literal.Literal;
import ca.sapon.jici.lexer.Token;
import ca.sapon.jici.lexer.TokenID;
import ca.sapon.jici.lexer.TokenType;
import ca.sapon.jici.util.OffsetStackList;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    public static List<Statement> parse(List<Token> tokens) {
        final List<Statement> statements = new ArrayList<>();
        final OffsetStackList<Token> parseList = new OffsetStackList<>(tokens);
        while (parseList.size() > 0) {
            statements.add(parseStatement(parseList));
        }
        return statements;
    }

    private static Statement parseStatement(OffsetStackList<Token> tokens) {
        final Token token0 = tokens.get(0);
        if (token0.getID() == TokenID.SYMBOL_SEMICOLON) {
            tokens.incrementOffset(1);
            return new Empty();
        }
        switch (token0.getType()) {
            case IDENTIFIER: {
                Statement statement;
                // try to parse a declaration
                tokens.pushOffset();
                statement = parseDeclaration(tokens);
                if (statement == null) {
                    // try to parse an assignment
                    tokens.peekOffset();
                    statement = parseAssignment(tokens);
                    if (statement == null) {
                        // TODO: use parse exception
                        throw new IllegalArgumentException();
                    }
                }
                if (tokens.size() >= 1 && tokens.get(0).getID() == TokenID.SYMBOL_SEMICOLON) {
                    tokens.incrementOffset(1);
                    return statement;
                }
                // TODO: use parse exception
                throw new IllegalArgumentException();
            }
            default: {
                // TODO: use parse exception
                throw new IllegalArgumentException();
            }
        }
    }

    private static Statement parseDeclaration(OffsetStackList<Token> tokens) {
        if (tokens.size() >= 2) {
            final Token token0 = tokens.get(0);
            final Token token1 = tokens.get(1);
            // try to parse a type and nam
            if (token0.getType() == TokenType.IDENTIFIER && token1.getType() == TokenType.IDENTIFIER) {
                tokens.incrementOffset(2);
                Expression value = null;
                if (tokens.size() >= 1) {
                    final Token token2 = tokens.get(0);
                    // try to parse an assignment and expression
                    if (token2.getID() == TokenID.SYMBOL_ASSIGN) {
                        tokens.incrementOffset(1);
                        value = parseExpression(tokens);
                        if (value == null) {
                            // TODO: use parse exception
                            throw new IllegalArgumentException();
                        }
                    }
                }
                return new Declaration((Identifier) token0, (Identifier) token1, value);
            }
        }
        return null;
    }

    private static Assignment parseAssignment(OffsetStackList<Token> tokens) {
        if (tokens.size() >= 2) {
            final Token token0 = tokens.get(0);
            final Token token1 = tokens.get(1);
            // try to parse a name, assignment and expression
            if (token0.getType() == TokenType.IDENTIFIER && token1.getType() == TokenType.ASSIGNMENT) {
                tokens.incrementOffset(2);
                final Expression value = parseExpression(tokens);
                if (value == null) {
                    // TODO: use parse exception
                    throw new IllegalArgumentException();
                }
                return new Assignment((Identifier) token0, value);
            }
        }
        return null;
    }

    private static Expression parseExpression(OffsetStackList<Token> tokens) {
        if (tokens.size() >= 1) {
            final Token token0 = tokens.get(0);
            switch (token0.getType()) {
                case IDENTIFIER: {
                    tokens.incrementOffset(1);
                    return new Variable((Identifier) token0);
                }
                case LITERAL: {
                    tokens.incrementOffset(1);
                    return (Literal) token0;
                }
            }
        }
        return null;
    }
}
