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
        // try to parse an expression that is also a statement
        final Expression expression = parseExpression(tokens);
        if (!(expression instanceof Statement)) {
            throw new IllegalArgumentException("Expected statement");
        }
        if (tokens.size() >= 1 && tokens.get(0).getID() == TokenID.SYMBOL_SEMICOLON) {
            tokens.incrementOffset(1);
            return  (Statement) expression;
        }
        throw new IllegalArgumentException("Illegal statement");
    }

    private static Statement parseDeclaration(OffsetStackList<Token> tokens) {
        return null;
    }

    private static Expression parseExpression(OffsetStackList<Token> tokens) {
        return parseAssignment(tokens);
    }

    private static Expression parseAssignment(OffsetStackList<Token> tokens) {
        final Expression assignee = parseAccess(tokens);
        if (tokens.size() >= 1 && tokens.get(0).getType() == TokenType.ASSIGNMENT) {
            tokens.incrementOffset(1);
            final Expression value = parseAssignment(tokens);
            return new Assignment(assignee, value);
        }
        return assignee;
    }

    private static Expression parseAccess(OffsetStackList<Token> tokens) {
        final Expression object = parseAtom(tokens);
        if (tokens.size() >= 1) {
            final Token token0 = tokens.get(0);
            if (token0.getID() == TokenID.SYMBOL_PERIOD) {
                tokens.incrementOffset(1);
                Expression member = parseAtom(tokens);
                final Access access = new Access(object, member);
                return parseAccess(tokens, access);
            }
        }
        return object;
    }

    private static Expression parseAccess(OffsetStackList<Token> tokens, Access object) {
        if (tokens.size() >= 1 && tokens.get(0).getID() == TokenID.SYMBOL_PERIOD) {
            tokens.incrementOffset(1);
            Expression member = parseAtom(tokens);
            final Access access = new Access(object, member);
            return parseAccess(tokens, access);
        }
        return object;
    }

    private static Expression parseAtom(OffsetStackList<Token> tokens) {
        if (tokens.size() >= 1) {
            final Token token0 = tokens.get(0);
            switch (token0.getType()) {
                case LITERAL: {
                    tokens.incrementOffset(1);
                    return (Literal) token0;
                }
                case IDENTIFIER: {
                    tokens.incrementOffset(1);
                    return new Variable((Identifier) token0);
                }
                default: {
                    if (token0.getID() == TokenID.SYMBOL_OPEN_PARENTHESIS) {
                        tokens.incrementOffset(1);
                        final Expression expression = parseExpression(tokens);
                        if (tokens.size() >= 1 && tokens.get(0).getID() == TokenID.SYMBOL_CLOSE_PARENTHESIS) {
                            tokens.incrementOffset(1);
                            return expression;
                        }
                        throw new IllegalArgumentException("Expected ')'");
                    }
                }
            }
        }
        throw new IllegalArgumentException("Expected literal, variable or '('");
    }

    /*
        ASSIGNMENT:    (CONDITIONAL = ASSIGNMENT) _ CONDITIONAL

        CONDITIONAL:   BOOLEAN_OR ? CONDITIONAL : CONDITIONAL _ BOOLEAN_OR

        BOOLEAN_OR:    BOOLEAN_AND || BOOLEAN_OR _ BOOLEAN_AND

        BOOLEAN_AND:   BITWISE_OR && BOOLEAN_AND _ BITWISE_OR

        BITWISE_OR:    BITWISE_XOR | BITWISE_OR _ BITWISE_XOR

        BITWISE_XOR:   BITWISE_AND ^ BITWISE_XOR _ BITWISE_AND

        BITWISE_AND:   EQUAL & BITWISE_AND _ EQUAL

        EQUAL:         COMPARISON == EQUAL _ COMPARISON

        COMPARISON:    SHIFT >= COMPARISON _ SHIFT

        SHIFT:         ADD >> SHIFT _ ADD

        ADD:           MULTIPLY + ADD _ MULTIPLY

        MULTIPLY:      MULTIPLY(UNARY * UNARY) _ UNARY
        MULTIPLY(M):   MULTIPLY(M * UNARY) _ M

        UNARY:         ++ACCESS _ ACCESS++ _ ACESS

        ACCESS:        ATOM.ACCESS _ ATOM() _ ATOM

        ATOM:          LITREAL _ VARIABLE _ (EXPRESSION)
    */
}
