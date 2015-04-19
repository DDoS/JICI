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
import java.util.Collections;
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

    private static Expression parseExpression(OffsetStackList<Token> tokens) {
        return parseAssignment(tokens);
    }

    private static List<Expression> parseExpressionList(OffsetStackList<Token> tokens) {
        return parseExpressionList(tokens, new ArrayList<Expression>());
    }

    private static List<Expression> parseExpressionList(OffsetStackList<Token> tokens, List<Expression> list) {
        list.add(parseExpression(tokens));
        if (tokens.size() >= 1 && tokens.get(0).getID() == TokenID.SYMBOL_COMMA) {
            tokens.incrementOffset(1);
            return parseExpressionList(tokens, list);
        }
        return list;
    }

    private static Expression parseAssignment(OffsetStackList<Token> tokens) {
        final Expression assignee = parseConditional(tokens);
        if (tokens.size() >= 1 && tokens.get(0).getType() == TokenType.ASSIGNMENT) {
            if (assignee instanceof Identifier || assignee instanceof Access
                    || assignee instanceof IndexOperation) {
                tokens.incrementOffset(1);
                final Expression value = parseAssignment(tokens);
                return new Assignment(assignee, value);
            }
            throw new IllegalArgumentException("Expected identifier or index operation");
        }
        return assignee;
    }

    private static Expression parseConditional(OffsetStackList<Token> tokens) {
        final Expression test = parseBooleanOR(tokens);
        if (tokens.size() >= 1 && tokens.get(0).getID() == TokenID.SYMBOL_QUESTION_MARK) {
            tokens.incrementOffset(1);
            final Expression left = parseExpression(tokens);
            if (tokens.size() >= 1 && tokens.get(0).getID() == TokenID.SYMBOL_COLON) {
                tokens.incrementOffset(1);
                final Expression right = parseConditional(tokens);
                return new Conditional(test, left, right);
            }
            throw new IllegalArgumentException("Expected ':'");
        }
        return test;
    }

    private static Expression parseBooleanOR(OffsetStackList<Token> tokens) {
        return parseBooleanOR(tokens, parseBooleanAND(tokens));
    }

    private static Expression parseBooleanOR(OffsetStackList<Token> tokens, Expression left) {
        if (tokens.size() >= 1) {
            final Token token0 = tokens.get(0);
            if (token0.getID() == TokenID.SYMBOL_BOOLEAN_OR) {
                tokens.incrementOffset(1);
                final Expression right = parseBooleanAND(tokens);
                final BinaryArithmetic add = new BinaryArithmetic(left, right, (Symbol) token0);
                return parseBooleanOR(tokens, add);
            }
        }
        return left;
    }

    private static Expression parseBooleanAND(OffsetStackList<Token> tokens) {
        return parseBooleanAND(tokens, parseBitwiseOR(tokens));
    }

    private static Expression parseBooleanAND(OffsetStackList<Token> tokens, Expression left) {
        if (tokens.size() >= 1) {
            final Token token0 = tokens.get(0);
            if (token0.getID() == TokenID.SYMBOL_BOOLEAN_AND) {
                tokens.incrementOffset(1);
                final Expression right = parseBitwiseOR(tokens);
                final BinaryArithmetic add = new BinaryArithmetic(left, right, (Symbol) token0);
                return parseBooleanAND(tokens, add);
            }
        }
        return left;
    }

    private static Expression parseBitwiseOR(OffsetStackList<Token> tokens) {
        return parseBitwiseOR(tokens, parseBitwiseXOR(tokens));
    }

    private static Expression parseBitwiseOR(OffsetStackList<Token> tokens, Expression left) {
        if (tokens.size() >= 1) {
            final Token token0 = tokens.get(0);
            if (token0.getID() == TokenID.SYMBOL_BITWISE_OR) {
                tokens.incrementOffset(1);
                final Expression right = parseBitwiseXOR(tokens);
                final BinaryArithmetic add = new BinaryArithmetic(left, right, (Symbol) token0);
                return parseBitwiseOR(tokens, add);
            }
        }
        return left;
    }

    private static Expression parseBitwiseXOR(OffsetStackList<Token> tokens) {
        return parseBitwiseXOR(tokens, parseBitwiseAND(tokens));
    }

    private static Expression parseBitwiseXOR(OffsetStackList<Token> tokens, Expression left) {
        if (tokens.size() >= 1) {
            final Token token0 = tokens.get(0);
            if (token0.getID() == TokenID.SYMBOL_BITWISE_XOR) {
                tokens.incrementOffset(1);
                final Expression right = parseBitwiseAND(tokens);
                final BinaryArithmetic add = new BinaryArithmetic(left, right, (Symbol) token0);
                return parseBitwiseXOR(tokens, add);
            }
        }
        return left;
    }

    private static Expression parseBitwiseAND(OffsetStackList<Token> tokens) {
        return parseBitwiseAND(tokens, parseEqual(tokens));
    }

    private static Expression parseBitwiseAND(OffsetStackList<Token> tokens, Expression left) {
        if (tokens.size() >= 1) {
            final Token token0 = tokens.get(0);
            if (token0.getID() == TokenID.SYMBOL_BITWISE_AND) {
                tokens.incrementOffset(1);
                final Expression right = parseEqual(tokens);
                final BinaryArithmetic add = new BinaryArithmetic(left, right, (Symbol) token0);
                return parseBitwiseAND(tokens, add);
            }
        }
        return left;
    }

    private static Expression parseEqual(OffsetStackList<Token> tokens) {
        return parseEqual(tokens, parseComparison(tokens));
    }

    private static Expression parseEqual(OffsetStackList<Token> tokens, Expression left) {
        if (tokens.size() >= 1) {
            final Token token0 = tokens.get(0);
            if (token0.getType() == TokenType.EQUAL_OPERATOR) {
                tokens.incrementOffset(1);
                final Expression right = parseComparison(tokens);
                final BinaryArithmetic add = new BinaryArithmetic(left, right, (Symbol) token0);
                return parseEqual(tokens, add);
            }
        }
        return left;
    }

    private static Expression parseComparison(OffsetStackList<Token> tokens) {
        return parseComparison(tokens, parseShift(tokens));
    }

    private static Expression parseComparison(OffsetStackList<Token> tokens, Expression left) {
        if (tokens.size() >= 1) {
            final Token token0 = tokens.get(0);
            if (token0.getType() == TokenType.COMPARISON_OPERATOR) {
                tokens.incrementOffset(1);
                final Expression right = parseShift(tokens);
                final BinaryArithmetic add = new BinaryArithmetic(left, right, (Symbol) token0);
                return parseComparison(tokens, add);
            }
        }
        return left;
    }

    private static Expression parseShift(OffsetStackList<Token> tokens) {
        return parseShift(tokens, parseAdd(tokens));
    }

    private static Expression parseShift(OffsetStackList<Token> tokens, Expression left) {
        if (tokens.size() >= 1) {
            final Token token0 = tokens.get(0);
            if (token0.getType() == TokenType.SHIFT_OPERATOR) {
                tokens.incrementOffset(1);
                final Expression right = parseAdd(tokens);
                final BinaryArithmetic add = new BinaryArithmetic(left, right, (Symbol) token0);
                return parseShift(tokens, add);
            }
        }
        return left;
    }

    private static Expression parseAdd(OffsetStackList<Token> tokens) {
        return parseAdd(tokens, parseMultiply(tokens));
    }

    private static Expression parseAdd(OffsetStackList<Token> tokens, Expression left) {
        if (tokens.size() >= 1) {
            final Token token0 = tokens.get(0);
            if (token0.getType() == TokenType.ADD_OPERATOR) {
                tokens.incrementOffset(1);
                final Expression right = parseMultiply(tokens);
                final BinaryArithmetic add = new BinaryArithmetic(left, right, (Symbol) token0);
                return parseAdd(tokens, add);
            }
        }
        return left;
    }

    private static Expression parseMultiply(OffsetStackList<Token> tokens) {
        return parseMultiply(tokens, parseUnary(tokens));
    }

    private static Expression parseMultiply(OffsetStackList<Token> tokens, Expression left) {
        if (tokens.size() >= 1) {
            final Token token0 = tokens.get(0);
            if (token0.getType() == TokenType.MULTIPLY_OPERATOR) {
                tokens.incrementOffset(1);
                final Expression right = parseUnary(tokens);
                final BinaryArithmetic multiply = new BinaryArithmetic(left, right, (Symbol) token0);
                return parseMultiply(tokens, multiply);
            }
        }
        return left;
    }

    private static Expression parseUnary(OffsetStackList<Token> tokens) {
        if (tokens.size() >= 1) {
            final Token token0 = tokens.get(0);
            final TokenID token0ID = token0.getID();
            if (token0.getType() == TokenType.UNARY_OPERATOR
                    || token0ID == TokenID.SYMBOL_PLUS
                    || token0ID == TokenID.SYMBOL_MINUS) {
                tokens.incrementOffset(1);
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

    private static Expression parseUnary(OffsetStackList<Token> tokens, Expression inner) {
        if (tokens.size() >= 1) {
            final Expression outer;
            switch (tokens.get(0).getID()) {
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
                tokens.incrementOffset(1);
                return parseUnary(tokens, outer);
            }
        }
        return inner;
    }

    private static Expression parseAccess(OffsetStackList<Token> tokens) {
        return parseAccess(tokens, parseAtom(tokens));
    }

    private static Expression parseAccess(OffsetStackList<Token> tokens, Expression object) {
        if (tokens.size() >= 1) {
            switch (tokens.get(0).getID()) {
                case SYMBOL_PERIOD: {
                    tokens.incrementOffset(1);
                    final Token token0 = tokens.get(0);
                    if (token0 instanceof Identifier) {
                        tokens.incrementOffset(1);
                        final Access access = new Access(object, (Identifier) token0);
                        return parseAccess(tokens, access);
                    }
                    throw new IllegalArgumentException("Expected identifier");
                }
                case SYMBOL_OPEN_BRACKET: {
                    tokens.incrementOffset(1);
                    final Expression index = parseExpression(tokens);
                    if (tokens.size() >= 1 && tokens.get(0).getID() == TokenID.SYMBOL_CLOSE_BRACKET) {
                        tokens.incrementOffset(1);
                        final IndexOperation indexOperation = new IndexOperation(object, index);
                        return parseAccess(tokens, indexOperation);
                    }
                    throw new IllegalArgumentException("Expected ']'");
                }
                case SYMBOL_OPEN_PARENTHESIS: {
                    tokens.incrementOffset(1);
                    final List<Expression> arguments;
                    if (tokens.size() >= 1 && tokens.get(0).getID() == TokenID.SYMBOL_CLOSE_PARENTHESIS) {
                        tokens.incrementOffset(1);
                        arguments = Collections.emptyList();
                    } else {
                        arguments = parseExpressionList(tokens);
                        if (tokens.size() < 1 || tokens.get(0).getID() != TokenID.SYMBOL_CLOSE_PARENTHESIS) {
                            throw new IllegalArgumentException("Expected ')'");
                        }
                        tokens.incrementOffset(1);
                    }
                    final CallOperation callOperation = new CallOperation(object, arguments);
                    return parseAccess(tokens, callOperation);
                }
            }
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
                    return (Identifier) token0;
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
