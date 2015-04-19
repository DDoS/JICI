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
import ca.sapon.jici.util.ListNavigator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Parser {
    public static List<Statement> parse(List<Token> tokens) {
        final List<Statement> statements = new ArrayList<>();
        final ListNavigator<Token> navigableTokens = new ListNavigator<>(tokens);
        while (navigableTokens.has()) {
            statements.add(parseStatement(navigableTokens));
        }
        return statements;
    }

    private static Statement parseStatement(ListNavigator<Token> tokens) {
        final Token token0 = tokens.get();
        if (token0.getID() == TokenID.SYMBOL_SEMICOLON) {
            tokens.advance();
            return new Empty();
        }
        // try to parse an expression that is also a statement
        final Expression expression = parseExpression(tokens);
        if (!(expression instanceof Statement)) {
            throw new IllegalArgumentException("Expected statement");
        }
        if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_SEMICOLON) {
            tokens.advance();
            return  (Statement) expression;
        }
        throw new IllegalArgumentException("Expected ';'");
    }

    private static Statement parseDeclaration(ListNavigator<Token> tokens) {
        return null;
    }

    /*
        NAME:             IDENTIFIER.NAME _ IDENTIFIER

        EXPRESSION:       ASSIGNMENT
        EXPRESSION_LIST:  EXPRESSION, EXPRESSION_LIST _ EXPRESSION

        ASSIGNMENT:       ACCESS = ASSIGNMENT _ CONDITIONAL
        CONDITIONAL:      BOOLEAN_OR ? EXPRESSION : CONDITIONAL _ BOOLEAN_OR
        BOOLEAN_OR:       BOOLEAN_OR || BOOLEAN_AND _ BOOLEAN_AND
        BOOLEAN_AND:      BOOLEAN_AND && BITWISE_OR _ BITWISE_OR
        BITWISE_OR:       BITWISE_OR | BITWISE_XOR _ BITWISE_XOR
        BITWISE_XOR:      BITWISE_XOR ^ BITWISE_AND _ BITWISE_AND
        BITWISE_AND:      BITWISE_AND & EQUAL _ EQUAL
        EQUAL:            EQUAL == COMPARISON _ COMPARISON
        COMPARISON:       COMPARISON >= SHIFT _ SHIFT instanceof NAME _ SHIFT
        SHIFT:            SHIFT >> ADD _ ADD
        ADD:              ADD + MULTIPLY _ MULTIPLY
        MULTIPLY:         MULTIPLY * UNARY _ UNARY
        UNARY:            +UNARY _ ++UNARY _ UNARY++ _ (NAME) UNARY _ new NAME() _ new NAME(EXPRESSION_LIST) _ ACCESS
        ACCESS:           ACCESS.IDENTIFIER _ ACCESS() _ ACCESS(EXPRESSION_LIST) _ ACCESS[EXPRESSION] _ ATOM

        ATOM:             LITREAL _ IDENTIFIER _ (EXPRESSION)
    */

    private static Expression parseExpression(ListNavigator<Token> tokens) {
        return parseAssignment(tokens);
    }

    private static List<Expression> parseExpressionList(ListNavigator<Token> tokens) {
        return parseExpressionList(tokens, new ArrayList<Expression>());
    }

    private static List<Expression> parseExpressionList(ListNavigator<Token> tokens, List<Expression> list) {
        list.add(parseExpression(tokens));
        if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_COMMA) {
            tokens.advance();
            return parseExpressionList(tokens, list);
        }
        return list;
    }

    private static Expression parseAssignment(ListNavigator<Token> tokens) {
        final Expression assignee = parseConditional(tokens);
        if (tokens.has() && tokens.get().getType() == TokenType.ASSIGNMENT) {
            if (assignee instanceof Identifier || assignee instanceof Access
                    || assignee instanceof IndexOperation) {
                tokens.advance();
                final Expression value = parseAssignment(tokens);
                return new Assignment(assignee, value);
            }
            throw new IllegalArgumentException("Expected identifier or index operation");
        }
        return assignee;
    }

    private static Expression parseConditional(ListNavigator<Token> tokens) {
        final Expression test = parseBooleanOR(tokens);
        if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_QUESTION_MARK) {
            tokens.advance();
            final Expression left = parseExpression(tokens);
            if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_COLON) {
                tokens.advance();
                final Expression right = parseConditional(tokens);
                return new Conditional(test, left, right);
            }
            throw new IllegalArgumentException("Expected ':'");
        }
        return test;
    }

    private static Expression parseBooleanOR(ListNavigator<Token> tokens) {
        return parseBooleanOR(tokens, parseBooleanAND(tokens));
    }

    private static Expression parseBooleanOR(ListNavigator<Token> tokens, Expression left) {
        if (tokens.has()) {
            final Token token0 = tokens.get();
            if (token0.getID() == TokenID.SYMBOL_BOOLEAN_OR) {
                tokens.advance();
                final Expression right = parseBooleanAND(tokens);
                final BinaryArithmetic add = new BinaryArithmetic(left, right, (Symbol) token0);
                return parseBooleanOR(tokens, add);
            }
        }
        return left;
    }

    private static Expression parseBooleanAND(ListNavigator<Token> tokens) {
        return parseBooleanAND(tokens, parseBitwiseOR(tokens));
    }

    private static Expression parseBooleanAND(ListNavigator<Token> tokens, Expression left) {
        if (tokens.has()) {
            final Token token0 = tokens.get();
            if (token0.getID() == TokenID.SYMBOL_BOOLEAN_AND) {
                tokens.advance();
                final Expression right = parseBitwiseOR(tokens);
                final BinaryArithmetic add = new BinaryArithmetic(left, right, (Symbol) token0);
                return parseBooleanAND(tokens, add);
            }
        }
        return left;
    }

    private static Expression parseBitwiseOR(ListNavigator<Token> tokens) {
        return parseBitwiseOR(tokens, parseBitwiseXOR(tokens));
    }

    private static Expression parseBitwiseOR(ListNavigator<Token> tokens, Expression left) {
        if (tokens.has()) {
            final Token token0 = tokens.get();
            if (token0.getID() == TokenID.SYMBOL_BITWISE_OR) {
                tokens.advance();
                final Expression right = parseBitwiseXOR(tokens);
                final BinaryArithmetic add = new BinaryArithmetic(left, right, (Symbol) token0);
                return parseBitwiseOR(tokens, add);
            }
        }
        return left;
    }

    private static Expression parseBitwiseXOR(ListNavigator<Token> tokens) {
        return parseBitwiseXOR(tokens, parseBitwiseAND(tokens));
    }

    private static Expression parseBitwiseXOR(ListNavigator<Token> tokens, Expression left) {
        if (tokens.has()) {
            final Token token0 = tokens.get();
            if (token0.getID() == TokenID.SYMBOL_BITWISE_XOR) {
                tokens.advance();
                final Expression right = parseBitwiseAND(tokens);
                final BinaryArithmetic add = new BinaryArithmetic(left, right, (Symbol) token0);
                return parseBitwiseXOR(tokens, add);
            }
        }
        return left;
    }

    private static Expression parseBitwiseAND(ListNavigator<Token> tokens) {
        return parseBitwiseAND(tokens, parseEqual(tokens));
    }

    private static Expression parseBitwiseAND(ListNavigator<Token> tokens, Expression left) {
        if (tokens.has()) {
            final Token token0 = tokens.get();
            if (token0.getID() == TokenID.SYMBOL_BITWISE_AND) {
                tokens.advance();
                final Expression right = parseEqual(tokens);
                final BinaryArithmetic add = new BinaryArithmetic(left, right, (Symbol) token0);
                return parseBitwiseAND(tokens, add);
            }
        }
        return left;
    }

    private static Expression parseEqual(ListNavigator<Token> tokens) {
        return parseEqual(tokens, parseComparison(tokens));
    }

    private static Expression parseEqual(ListNavigator<Token> tokens, Expression left) {
        if (tokens.has()) {
            final Token token0 = tokens.get();
            if (token0.getType() == TokenType.EQUAL_OPERATOR) {
                tokens.advance();
                final Expression right = parseComparison(tokens);
                final BinaryArithmetic add = new BinaryArithmetic(left, right, (Symbol) token0);
                return parseEqual(tokens, add);
            }
        }
        return left;
    }

    private static Expression parseComparison(ListNavigator<Token> tokens) {
        return parseComparison(tokens, parseShift(tokens));
    }

    private static Expression parseComparison(ListNavigator<Token> tokens, Expression left) {
        if (tokens.has()) {
            final Token token0 = tokens.get();
            if (token0.getType() == TokenType.COMPARISON_OPERATOR) {
                tokens.advance();
                final Expression right = parseShift(tokens);
                final BinaryArithmetic add = new BinaryArithmetic(left, right, (Symbol) token0);
                return parseComparison(tokens, add);
            }
        }
        return left;
    }

    private static Expression parseShift(ListNavigator<Token> tokens) {
        return parseShift(tokens, parseAdd(tokens));
    }

    private static Expression parseShift(ListNavigator<Token> tokens, Expression left) {
        if (tokens.has()) {
            final Token token0 = tokens.get();
            if (token0.getType() == TokenType.SHIFT_OPERATOR) {
                tokens.advance();
                final Expression right = parseAdd(tokens);
                final BinaryArithmetic add = new BinaryArithmetic(left, right, (Symbol) token0);
                return parseShift(tokens, add);
            }
        }
        return left;
    }

    private static Expression parseAdd(ListNavigator<Token> tokens) {
        return parseAdd(tokens, parseMultiply(tokens));
    }

    private static Expression parseAdd(ListNavigator<Token> tokens, Expression left) {
        if (tokens.has()) {
            final Token token0 = tokens.get();
            if (token0.getType() == TokenType.ADD_OPERATOR) {
                tokens.advance();
                final Expression right = parseMultiply(tokens);
                final BinaryArithmetic add = new BinaryArithmetic(left, right, (Symbol) token0);
                return parseAdd(tokens, add);
            }
        }
        return left;
    }

    private static Expression parseMultiply(ListNavigator<Token> tokens) {
        return parseMultiply(tokens, parseUnary(tokens));
    }

    private static Expression parseMultiply(ListNavigator<Token> tokens, Expression left) {
        if (tokens.has()) {
            final Token token0 = tokens.get();
            if (token0.getType() == TokenType.MULTIPLY_OPERATOR) {
                tokens.advance();
                final Expression right = parseUnary(tokens);
                final BinaryArithmetic multiply = new BinaryArithmetic(left, right, (Symbol) token0);
                return parseMultiply(tokens, multiply);
            }
        }
        return left;
    }

    private static Expression parseUnary(ListNavigator<Token> tokens) {
        if (tokens.has()) {
            final Token token0 = tokens.get();
            final TokenID token0ID = token0.getID();
            if (token0.getType() == TokenType.UNARY_OPERATOR
                    || token0ID == TokenID.SYMBOL_PLUS
                    || token0ID == TokenID.SYMBOL_MINUS) {
                tokens.advance();
                final Expression inner = parseUnary(tokens);
                switch (token0ID) {
                    case SYMBOL_INCREMENT: {
                        return new Increment(inner, false, true);
                    }
                    case SYMBOL_DECREMENT: {
                        return new Increment(inner, false, false);
                    }
                    default: {
                        return new UnaryArithmetic(inner, false, (Symbol) token0);
                    }
                }
            }
            final Expression inner = parseAccess(tokens);
            return parseUnary(tokens, inner);
        }
        throw new IllegalArgumentException("Expected at least one token");
    }

    private static Expression parseUnary(ListNavigator<Token> tokens, Expression inner) {
        if (tokens.has()) {
            final Expression outer;
            switch (tokens.get().getID()) {
                case SYMBOL_INCREMENT: {
                    outer = new Increment(inner, true, true);
                    break;
                }
                case SYMBOL_DECREMENT: {
                    outer = new Increment(inner, true, false);
                    break;
                }
                default: {
                    outer = null;
                    break;
                }
            }
            if (outer != null) {
                tokens.advance();
                return parseUnary(tokens, outer);
            }
        }
        return inner;
    }

    private static Expression parseAccess(ListNavigator<Token> tokens) {
        return parseAccess(tokens, parseAtom(tokens));
    }

    private static Expression parseAccess(ListNavigator<Token> tokens, Expression object) {
        if (tokens.has()) {
            switch (tokens.get().getID()) {
                case SYMBOL_PERIOD: {
                    tokens.advance();
                    if (tokens.has()) {
                        final Token token0 = tokens.get();
                        if (token0 instanceof Identifier) {
                            tokens.advance();
                            final Access access = new Access(object, (Identifier) token0);
                            return parseAccess(tokens, access);
                        }
                    }
                    throw new IllegalArgumentException("Expected identifier");
                }
                case SYMBOL_OPEN_BRACKET: {
                    tokens.advance();
                    final Expression index = parseExpression(tokens);
                    if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_CLOSE_BRACKET) {
                        tokens.advance();
                        final IndexOperation indexOperation = new IndexOperation(object, index);
                        return parseAccess(tokens, indexOperation);
                    }
                    throw new IllegalArgumentException("Expected ']'");
                }
                case SYMBOL_OPEN_PARENTHESIS: {
                    tokens.advance();
                    final List<Expression> arguments;
                    if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_CLOSE_PARENTHESIS) {
                        tokens.advance();
                        arguments = Collections.emptyList();
                    } else {
                        arguments = parseExpressionList(tokens);
                        if (!tokens.has() || tokens.get().getID() != TokenID.SYMBOL_CLOSE_PARENTHESIS) {
                            throw new IllegalArgumentException("Expected ')'");
                        }
                        tokens.advance();
                    }
                    final CallOperation callOperation = new CallOperation(object, arguments);
                    return parseAccess(tokens, callOperation);
                }
            }
        }
        return object;
    }

    private static Expression parseAtom(ListNavigator<Token> tokens) {
        if (tokens.has()) {
            final Token token0 = tokens.get();
            switch (token0.getType()) {
                case LITERAL: {
                    tokens.advance();
                    return (Literal) token0;
                }
                case IDENTIFIER: {
                    tokens.advance();
                    return (Identifier) token0;
                }
                default: {
                    if (token0.getID() == TokenID.SYMBOL_OPEN_PARENTHESIS) {
                        tokens.advance();
                        final Expression expression = parseExpression(tokens);
                        if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_CLOSE_PARENTHESIS) {
                            tokens.advance();
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
