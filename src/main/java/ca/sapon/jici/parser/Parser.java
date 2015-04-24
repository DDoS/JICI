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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ca.sapon.jici.lexer.Identifier;
import ca.sapon.jici.lexer.Keyword;
import ca.sapon.jici.lexer.Symbol;
import ca.sapon.jici.lexer.Token;
import ca.sapon.jici.lexer.TokenID;
import ca.sapon.jici.lexer.TokenType;
import ca.sapon.jici.lexer.literal.Literal;
import ca.sapon.jici.parser.expression.Cast;
import ca.sapon.jici.parser.expression.Expression;
import ca.sapon.jici.parser.expression.access.ClassAccess;
import ca.sapon.jici.parser.expression.access.FieldAccess;
import ca.sapon.jici.parser.expression.access.IndexAccess;
import ca.sapon.jici.parser.expression.access.SelfAccess;
import ca.sapon.jici.parser.expression.arithmetic.Add;
import ca.sapon.jici.parser.expression.arithmetic.Assignment;
import ca.sapon.jici.parser.expression.arithmetic.Increment;
import ca.sapon.jici.parser.expression.arithmetic.Multiply;
import ca.sapon.jici.parser.expression.arithmetic.Shift;
import ca.sapon.jici.parser.expression.arithmetic.Sign;
import ca.sapon.jici.parser.expression.call.ConstructorCall;
import ca.sapon.jici.parser.expression.call.MethodCall;
import ca.sapon.jici.parser.expression.logic.BitwiseLogic;
import ca.sapon.jici.parser.expression.logic.BooleanLogic;
import ca.sapon.jici.parser.expression.logic.Comparison;
import ca.sapon.jici.parser.expression.logic.Conditional;
import ca.sapon.jici.parser.expression.logic.TypeCheck;
import ca.sapon.jici.parser.statement.Declaration;
import ca.sapon.jici.parser.statement.Declaration.Variable;
import ca.sapon.jici.parser.statement.Empty;
import ca.sapon.jici.parser.statement.Import;
import ca.sapon.jici.parser.statement.Statement;
import ca.sapon.jici.parser.type.ClassType;
import ca.sapon.jici.parser.type.PrimitiveType;
import ca.sapon.jici.parser.type.Type;
import ca.sapon.jici.util.ListNavigator;

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
        final Token token = tokens.get();
        if (token.getID() == TokenID.SYMBOL_SEMICOLON) {
            tokens.advance();
            return new Empty();
        }
        // try to parse an import
        try {
            tokens.pushPosition();
            final Import _import = parseImport(tokens);
            tokens.discardPosition();
            return _import;
        } catch (IllegalArgumentException exception) {
            tokens.popPosition();
        }
        // try to parse a declaration
        try {
            tokens.pushPosition();
            final Declaration declaration = parseDeclaration(tokens);
            tokens.discardPosition();
            return declaration;
        } catch (IllegalArgumentException exception) {
            tokens.popPosition();
        }
        // try to parse an expression that is also a statement
        final Expression expression = parseExpression(tokens);
        if (expression instanceof Statement) {
            if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_SEMICOLON) {
                tokens.advance();
                return (Statement) expression;
            }
            throw new IllegalArgumentException("Expected ';'");
        }
        throw new IllegalArgumentException("Expected statement");
    }

    /*
        NAME:            IDENTIFIER.NAME _ IDENTIFIER

        TYPE:            NAME _ PRIMITIVE_TYPE

        VARIABLE:        IDENTIFIER _ IDENTIFIER = EXPRESSION
        VARIABLE_LSIT:   VARIABLE, VARIABLE_LSIT _ VARIABLE

        DECLARATION:     TYPE VARIABLE_LSIT;
    */

    private static List<Identifier> parseName(ListNavigator<Token> tokens) {
        return parseName(tokens, new ArrayList<>());
    }

    private static List<Identifier> parseName(ListNavigator<Token> tokens, List<Identifier> name) {
        if (tokens.has()) {
            final Token token = tokens.get();
            if (token instanceof Identifier) {
                tokens.advance();
                name.add((Identifier) token);
                if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_PERIOD) {
                    tokens.advance();
                    return parseName(tokens, name);
                }
                return name;
            } else if (token.getID() == TokenID.SYMBOL_MULTIPLY && !name.isEmpty()) {
                tokens.retreat();
                return name;
            }
        }
        throw new IllegalArgumentException("Expected identifier");
    }

    private static Type parseType(ListNavigator<Token> tokens) {
        if (tokens.has()) {
            final Token token = tokens.get();
            if (token.getType() == TokenType.PRIMITIVE_TYPE) {
                tokens.advance();
                return new PrimitiveType((Keyword) token);
            }
            return new ClassType(parseName(tokens));
        }
        throw new IllegalArgumentException("Expected primitive type or identifier");
    }

    private static Variable parseVariable(ListNavigator<Token> tokens) {
        if (tokens.has()) {
            final Token token = tokens.get();
            if (token instanceof Identifier) {
                tokens.advance();
                if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_ASSIGN) {
                    tokens.advance();
                    final Expression value = parseExpression(tokens);
                    return new Variable((Identifier) token, value);
                }
                return new Variable((Identifier) token);
            }
        }
        throw new IllegalArgumentException("Expected identifier");
    }

    private static List<Variable> parseVariableList(ListNavigator<Token> tokens) {
        return parseVariableList(tokens, new ArrayList<>());
    }

    private static List<Variable> parseVariableList(ListNavigator<Token> tokens, List<Variable> list) {
        list.add(parseVariable(tokens));
        if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_COMMA) {
            tokens.advance();
            return parseVariableList(tokens, list);
        }
        return list;
    }

    private static Declaration parseDeclaration(ListNavigator<Token> tokens) {
        if (tokens.has()) {
            final Declaration declaration = new Declaration(parseType(tokens), parseVariableList(tokens));
            if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_SEMICOLON) {
                tokens.advance();
                return declaration;
            }
            throw new IllegalArgumentException("Expected ';'");
        }
        throw new IllegalArgumentException("Expected identifier or primitive type");
    }

    /*
        IMPORT:          import NAME; _ import NAME.*;
    */

    private static Import parseImport(ListNavigator<Token> tokens) {
        if (tokens.has() && tokens.get().getID() == TokenID.KEYWORD_IMPORT) {
            tokens.advance();
            final List<Identifier> name = parseName(tokens);
            if (tokens.has()) {
                final Token token = tokens.get();
                if (token.getID() == TokenID.SYMBOL_PERIOD && tokens.has(2) && tokens.get(1).getID() == TokenID.SYMBOL_MULTIPLY) {
                    tokens.advance(2);
                    if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_SEMICOLON) {
                        tokens.advance();
                        return new Import(name, true);
                    }
                } else if (token.getID() == TokenID.SYMBOL_SEMICOLON) {
                    tokens.advance();
                    return new Import(name, false);
                }
                throw new IllegalArgumentException("Expected ';'");
            }
            throw new IllegalArgumentException("Expected '*' or ';'");
        }
        throw new IllegalArgumentException("Expected \"import\"");
    }

    /*
        EXPRESSION:      ASSIGNMENT
        EXPRESSION_LIST: EXPRESSION, EXPRESSION_LIST _ EXPRESSION

        ASSIGNMENT:      ACCESS = ASSIGNMENT _ CONDITIONAL
        CONDITIONAL:     BOOLEAN_OR ? EXPRESSION : CONDITIONAL _ BOOLEAN_OR
        BOOLEAN_OR:      BOOLEAN_OR || BOOLEAN_AND _ BOOLEAN_AND
        BOOLEAN_AND:     BOOLEAN_AND && BITWISE_OR _ BITWISE_OR
        BITWISE_OR:      BITWISE_OR | BITWISE_XOR _ BITWISE_XOR
        BITWISE_XOR:     BITWISE_XOR ^ BITWISE_AND _ BITWISE_AND
        BITWISE_AND:     BITWISE_AND & EQUAL _ EQUAL
        EQUAL:           EQUAL == COMPARISON _ COMPARISON
        COMPARISON:      COMPARISON >= SHIFT _ SHIFT instanceof TYPE _ SHIFT
        SHIFT:           SHIFT >> ADD _ ADD
        ADD:             ADD + MULTIPLY _ MULTIPLY
        MULTIPLY:        MULTIPLY * UNARY _ UNARY
        UNARY:           +UNARY _ ++UNARY _ UNARY++ _ (TYPE) UNARY _ ACCESS
        ACCESS:          ACCESS.IDENTIFIER _ ACCESS.class _ ACCESS(EXPRESSION_LIST) _ ACCESS[EXPRESSION] _ new NAME(EXPRESSION_LIST) _ ATOM

        ATOM:            LITREAL _ IDENTIFIER _ this _ super _ (EXPRESSION)
    */

    private static Expression parseExpression(ListNavigator<Token> tokens) {
        return parseAssignment(tokens);
    }

    private static List<Expression> parseExpressionList(ListNavigator<Token> tokens) {
        return parseExpressionList(tokens, new ArrayList<>());
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
        if (tokens.has()) {
            final Token token = tokens.get();
            if (token.getType() == TokenType.ASSIGNMENT) {
                if (assignee instanceof Identifier || assignee instanceof FieldAccess || assignee instanceof IndexAccess) {
                    tokens.advance();
                    final Expression value = parseAssignment(tokens);
                    return new Assignment(assignee, value, (Symbol) token);
                }
                throw new IllegalArgumentException("Expected identifier or index operation");
            }
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
            final Token token = tokens.get();
            if (token.getID() == TokenID.SYMBOL_BOOLEAN_OR) {
                tokens.advance();
                final Expression right = parseBooleanAND(tokens);
                final BooleanLogic add = new BooleanLogic(left, right, (Symbol) token);
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
            final Token token = tokens.get();
            if (token.getID() == TokenID.SYMBOL_BOOLEAN_AND) {
                tokens.advance();
                final Expression right = parseBitwiseOR(tokens);
                final BooleanLogic add = new BooleanLogic(left, right, (Symbol) token);
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
            final Token token = tokens.get();
            if (token.getID() == TokenID.SYMBOL_BITWISE_OR) {
                tokens.advance();
                final Expression right = parseBitwiseXOR(tokens);
                final BitwiseLogic add = new BitwiseLogic(left, right, (Symbol) token);
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
            final Token token = tokens.get();
            if (token.getID() == TokenID.SYMBOL_BITWISE_XOR) {
                tokens.advance();
                final Expression right = parseBitwiseAND(tokens);
                final BitwiseLogic add = new BitwiseLogic(left, right, (Symbol) token);
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
            final Token token = tokens.get();
            if (token.getID() == TokenID.SYMBOL_BITWISE_AND) {
                tokens.advance();
                final Expression right = parseEqual(tokens);
                final BitwiseLogic add = new BitwiseLogic(left, right, (Symbol) token);
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
            final Token token = tokens.get();
            if (token.getType() == TokenType.EQUAL_OPERATOR) {
                tokens.advance();
                final Expression right = parseComparison(tokens);
                final Comparison add = new Comparison(left, right, (Symbol) token);
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
            final Token token = tokens.get();
            if (token.getType() == TokenType.COMPARISON_OPERATOR) {
                tokens.advance();
                final Expression right = parseShift(tokens);
                final Comparison add = new Comparison(left, right, (Symbol) token);
                return parseComparison(tokens, add);
            } else if (token.getID() == TokenID.KEYWORD_INSTANCEOF) {
                tokens.advance();
                final Type type = parseType(tokens);
                final TypeCheck typeCheck = new TypeCheck(left, type);
                return parseComparison(tokens, typeCheck);
            }
        }
        return left;
    }

    private static Expression parseShift(ListNavigator<Token> tokens) {
        return parseShift(tokens, parseAdd(tokens));
    }

    private static Expression parseShift(ListNavigator<Token> tokens, Expression left) {
        if (tokens.has()) {
            final Token token = tokens.get();
            if (token.getType() == TokenType.SHIFT_OPERATOR) {
                tokens.advance();
                final Expression right = parseAdd(tokens);
                final Shift add = new Shift(left, right, (Symbol) token);
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
            final Token token = tokens.get();
            if (token.getType() == TokenType.ADD_OPERATOR) {
                tokens.advance();
                final Expression right = parseMultiply(tokens);
                final Add add = new Add(left, right, (Symbol) token);
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
            final Token token = tokens.get();
            if (token.getType() == TokenType.MULTIPLY_OPERATOR) {
                tokens.advance();
                final Expression right = parseUnary(tokens);
                final Multiply multiply = new Multiply(left, right, (Symbol) token);
                return parseMultiply(tokens, multiply);
            }
        }
        return left;
    }

    private static Expression parseUnary(ListNavigator<Token> tokens) {
        if (tokens.has()) {
            final Token token = tokens.get();
            switch (token.getID()) {
                case SYMBOL_INCREMENT:
                case SYMBOL_DECREMENT: {
                    tokens.advance();
                    final Expression inner = parseUnary(tokens);
                    return new Increment(inner, (Symbol) token, false);
                }
                case SYMBOL_PLUS:
                case SYMBOL_MINUS: {
                    tokens.advance();
                    final Expression inner = parseUnary(tokens);
                    return new Sign(inner, (Symbol) token);
                }
                case SYMBOL_BOOLEAN_NOT: {
                    tokens.advance();
                    final Expression inner = parseUnary(tokens);
                    return new BooleanLogic(inner, (Symbol) token);
                }
                case SYMBOL_BITWISE_NOT: {
                    tokens.advance();
                    final Expression inner = parseUnary(tokens);
                    return new BitwiseLogic(inner, (Symbol) token);
                }
                case SYMBOL_OPEN_PARENTHESIS: {
                    if (tokens.has(2)) {
                        tokens.pushPosition();
                        tokens.advance();
                        try {
                            final Type type = parseType(tokens);
                            if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_CLOSE_PARENTHESIS) {
                                tokens.advance();
                                final Expression inner = parseUnary(tokens);
                                tokens.discardPosition();
                                return new Cast(type, inner);
                            }
                        } catch (IllegalArgumentException exception) {
                            // this is not a cast, but an access
                        }
                        tokens.popPosition();
                    }
                }
            }
            final Expression inner = parseAccess(tokens);
            return parseUnary(tokens, inner);
        }
        throw new IllegalArgumentException("Expected a token");
    }

    private static Expression parseUnary(ListNavigator<Token> tokens, Expression inner) {
        if (tokens.has()) {
            final Token token = tokens.get();
            switch (token.getID()) {
                case SYMBOL_INCREMENT:
                case SYMBOL_DECREMENT: {
                    tokens.advance();
                    final Expression outer = new Increment(inner, (Symbol) token, true);
                    return parseUnary(tokens, outer);
                }
            }
        }
        return inner;
    }

    private static Expression parseAccess(ListNavigator<Token> tokens) {
        if (tokens.has() && tokens.get().getID() == TokenID.KEYWORD_NEW) {
            tokens.advance();
            final List<Identifier> name = parseName(tokens);
            if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_OPEN_PARENTHESIS) {
                tokens.advance();
                if (tokens.has()) {
                    final List<Expression> arguments;
                    if (tokens.get().getID() == TokenID.SYMBOL_CLOSE_PARENTHESIS) {
                        tokens.advance();
                        arguments = Collections.emptyList();
                    } else {
                        arguments = parseExpressionList(tokens);
                        if (!tokens.has() || tokens.get().getID() != TokenID.SYMBOL_CLOSE_PARENTHESIS) {
                            throw new IllegalArgumentException("Expected ')'");
                        }
                        tokens.advance();
                    }
                    final ConstructorCall call = new ConstructorCall(name, arguments);
                    return parseAccess(tokens, call);
                }
                throw new IllegalArgumentException("Expected expression list or ')'");
            }
            throw new IllegalArgumentException("Expected '('");
        }
        return parseAccess(tokens, parseAtom(tokens));
    }

    private static Expression parseAccess(ListNavigator<Token> tokens, Expression object) {
        if (tokens.has()) {
            switch (tokens.get().getID()) {
                case SYMBOL_PERIOD: {
                    tokens.advance();
                    if (tokens.has()) {
                        final Token token = tokens.get();
                        if (token instanceof Identifier) {
                            tokens.advance();
                            final FieldAccess access = new FieldAccess(object, (Identifier) token);
                            return parseAccess(tokens, access);
                        } else if (token.getID() == TokenID.KEYWORD_CLASS) {
                            tokens.advance();
                            final ClassAccess access = new ClassAccess(object);
                            return parseAccess(tokens, access);
                        } else if (token.getType() == TokenType.SELF_REFERENCE) {
                            tokens.advance();
                            final SelfAccess access = new SelfAccess(object, (Keyword) token);
                            return parseAccess(tokens, access);
                        }
                    }
                    throw new IllegalArgumentException("Expected identifier or \"class\"");
                }
                case SYMBOL_OPEN_BRACKET: {
                    tokens.advance();
                    final Expression index = parseExpression(tokens);
                    if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_CLOSE_BRACKET) {
                        tokens.advance();
                        final IndexAccess access = new IndexAccess(object, index);
                        return parseAccess(tokens, access);
                    }
                    throw new IllegalArgumentException("Expected ']'");
                }
                case SYMBOL_OPEN_PARENTHESIS: {
                    tokens.advance();
                    if (tokens.has()) {
                        final List<Expression> arguments;
                        if (tokens.get().getID() == TokenID.SYMBOL_CLOSE_PARENTHESIS) {
                            tokens.advance();
                            arguments = Collections.emptyList();
                        } else {
                            arguments = parseExpressionList(tokens);
                            if (!tokens.has() || tokens.get().getID() != TokenID.SYMBOL_CLOSE_PARENTHESIS) {
                                throw new IllegalArgumentException("Expected ')'");
                            }
                            tokens.advance();
                        }
                        final MethodCall call = new MethodCall(object, arguments);
                        return parseAccess(tokens, call);
                    }
                    throw new IllegalArgumentException("Expected expression list or ')'");
                }
            }
        }
        return object;
    }

    private static Expression parseAtom(ListNavigator<Token> tokens) {
        if (tokens.has()) {
            final Token token = tokens.get();
            switch (token.getType()) {
                case LITERAL: {
                    tokens.advance();
                    return (Literal) token;
                }
                case IDENTIFIER: {
                    tokens.advance();
                    return (Identifier) token;
                }
                case SELF_REFERENCE: {
                    tokens.advance();
                    return new SelfAccess((Keyword) token);
                }
                default: {
                    if (token.getID() == TokenID.SYMBOL_OPEN_PARENTHESIS) {
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
        throw new IllegalArgumentException("Expected literal, identifier or '('");
    }
}
