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
import ca.sapon.jici.lexer.Symbol;
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
        throw new IllegalArgumentException("Expected ';'");
    }

    private static Statement parseDeclaration(OffsetStackList<Token> tokens) {
        return null;
    }

    /*
        EXPRESSION:       ASSIGNMENT
        EXPRESSION_LIST:  EXPRESSION, EXPRESSION_LIST _ EXPRESSION

        ASSIGNMENT:       CONDITIONAL = ASSIGNMENT _ CONDITIONAL
        CONDITIONAL:      BOOLEAN_OR ? CONDITIONAL : CONDITIONAL _ BOOLEAN_OR
        BOOLEAN_OR:       BOOLEAN_OR || BOOLEAN_AND _ BOOLEAN_AND
        BOOLEAN_AND:      BOOLEAN_AND && BITWISE_OR _ BITWISE_OR
        BITWISE_OR:       BITWISE_OR | BITWISE_XOR _ BITWISE_XOR
        BITWISE_XOR:      BITWISE_XOR ^ BITWISE_AND _ BITWISE_AND
        BITWISE_AND:      BITWISE_AND & EQUAL _ EQUAL
        EQUAL:            EQUAL == COMPARISON _ COMPARISON
        COMPARISON:       COMPARISON >= SHIFT _ SHIFT
        SHIFT:            SHIFT >> ADD _ ADD
        ADD:              ADD + MULTIPLY _ MULTIPLY
        MULTIPLY:         MULTIPLY * UNARY _ UNARY
        UNARY:            +UNARY _ ++UNARY _ UNARY++ _ ACESS
        ACCESS:           ACCESS.ATOM _ ACCESS(EXPRESSION_LIST) _ ACCESS[EXPRESSION] _ ATOM

        ATOM:             LITREAL _ VARIABLE _ (EXPRESSION)
    */

    private static Expression parseExpression(OffsetStackList<Token> tokens) {
        return parseAssignment(tokens);
    }

    private static Expression parseAssignment(OffsetStackList<Token> tokens) {
        final Expression assignee = parseMultiply(tokens);
        if (tokens.size() >= 1 && tokens.get(0).getType() == TokenType.ASSIGNMENT) {
            tokens.incrementOffset(1);
            final Expression value = parseAssignment(tokens);
            return new Assignment(assignee, value);
        }
        return assignee;
    }

    private static Expression parseMultiply(OffsetStackList<Token> tokens) {
        final Expression value0 = parseUnary(tokens);
        if (tokens.size() >= 1) {
            final Token token0 = tokens.get(0);
            final TokenID token0ID = token0.getID();
            if (token0ID == TokenID.SYMBOL_MULTIPLY
                    || token0ID == TokenID.SYMBOL_DIVIDE
                    || token0ID == TokenID.SYMBOL_MODULO) {
                tokens.incrementOffset(1);
                final Expression value1 = parseUnary(tokens);
                final BinaryOperation multiply = new BinaryOperation(value0, value1, (Symbol) token0);
                return parseMultiply(tokens, multiply);
            }
        }
        return value0;
    }

    private static Expression parseMultiply(OffsetStackList<Token> tokens, BinaryOperation leftMultiply) {
        if (tokens.size() >= 1) {
            final Token token0 = tokens.get(0);
            final TokenID token0ID = token0.getID();
            if (token0ID == TokenID.SYMBOL_MULTIPLY
                    || token0ID == TokenID.SYMBOL_DIVIDE
                    || token0ID == TokenID.SYMBOL_MODULO) {
                tokens.incrementOffset(1);
                final Expression value1 = parseUnary(tokens);
                final BinaryOperation multiply = new BinaryOperation(leftMultiply, value1, (Symbol) token0);
                return parseMultiply(tokens, multiply);
            }
        }
        return leftMultiply;
    }

    private static Expression parseUnary(OffsetStackList<Token> tokens) {
        if (tokens.size() >= 1) {
            Token token0 = tokens.get(0);
            TokenID token0ID = token0.getID();
            if (token0.getType() == TokenType.UNARY_OPERATOR
                    || token0ID == TokenID.SYMBOL_PLUS
                    || token0ID == TokenID.SYMBOL_MINUS) {
                tokens.incrementOffset(1);
                final Expression value = parseUnary(tokens);
                if (token0ID == TokenID.SYMBOL_INCREMENT || token0ID == TokenID.SYMBOL_DECREMENT) {
                    return new Increment(value, (Symbol) token0, false);
                }
                return new UnaryOperation(value, (Symbol) token0, false);
            }
            final Expression value = parseAccess(tokens);
            return parseUnary(tokens, value);
        }
        throw new IllegalArgumentException("Expected at least one token");
    }

    private static Expression parseUnary(OffsetStackList<Token> tokens, Expression inner) {
        if (tokens.size() >= 1) {
            final Token token0 = tokens.get(0);
            final TokenID token0ID = token0.getID();
            if (token0ID == TokenID.SYMBOL_INCREMENT || token0ID == TokenID.SYMBOL_DECREMENT) {
                tokens.incrementOffset(1);
                final Expression value = new Increment(inner, (Symbol) token0, true);
                return parseUnary(tokens, value);
            }
        }
        return inner;
    }

    private static Expression parseAccess(OffsetStackList<Token> tokens) {
        final Expression object = parseAtom(tokens);
        if (tokens.size() >= 1) {
            final Token token0 = tokens.get(0);
            if (token0.getID() == TokenID.SYMBOL_PERIOD) {
                tokens.incrementOffset(1);
                final Expression member = parseAtom(tokens);
                final Access access = new Access(object, member);
                return parseAccess(tokens, access);
            }
        }
        return object;
    }

    private static Expression parseAccess(OffsetStackList<Token> tokens, Access object) {
        if (tokens.size() >= 1 && tokens.get(0).getID() == TokenID.SYMBOL_PERIOD) {
            tokens.incrementOffset(1);
            final Expression member = parseAtom(tokens);
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
}
