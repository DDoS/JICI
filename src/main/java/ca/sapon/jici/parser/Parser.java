/*
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

import ca.sapon.jici.SourceIndexed;
import ca.sapon.jici.lexer.Identifier;
import ca.sapon.jici.lexer.Keyword;
import ca.sapon.jici.lexer.Symbol;
import ca.sapon.jici.lexer.Token;
import ca.sapon.jici.lexer.TokenID;
import ca.sapon.jici.lexer.TokenGroup;
import ca.sapon.jici.lexer.literal.Literal;
import ca.sapon.jici.lexer.literal.number.NumberLiteral;
import ca.sapon.jici.parser.expression.Cast;
import ca.sapon.jici.parser.expression.ClassAccess;
import ca.sapon.jici.parser.expression.Conditional;
import ca.sapon.jici.parser.expression.Expression;
import ca.sapon.jici.parser.expression.Shift;
import ca.sapon.jici.parser.expression.arithmetic.Arithmetic;
import ca.sapon.jici.parser.expression.arithmetic.Sign;
import ca.sapon.jici.parser.expression.assignment.Assignment;
import ca.sapon.jici.parser.expression.assignment.PostIncrement;
import ca.sapon.jici.parser.expression.assignment.PreIncrement;
import ca.sapon.jici.parser.expression.call.AmbiguousCall;
import ca.sapon.jici.parser.expression.call.ConstructorCall;
import ca.sapon.jici.parser.expression.call.MethodCall;
import ca.sapon.jici.parser.expression.comparison.Comparison;
import ca.sapon.jici.parser.expression.comparison.Equal;
import ca.sapon.jici.parser.expression.comparison.TypeCheck;
import ca.sapon.jici.parser.expression.logic.BitwiseLogic;
import ca.sapon.jici.parser.expression.logic.BitwiseNot;
import ca.sapon.jici.parser.expression.logic.BooleanLogic;
import ca.sapon.jici.parser.expression.logic.BooleanNot;
import ca.sapon.jici.parser.expression.reference.AmbiguousReference;
import ca.sapon.jici.parser.expression.reference.FieldAccess;
import ca.sapon.jici.parser.expression.reference.IndexAccess;
import ca.sapon.jici.parser.expression.reference.Reference;
import ca.sapon.jici.parser.expression.reference.VariableAccess;
import ca.sapon.jici.parser.statement.Declaration;
import ca.sapon.jici.parser.statement.Declaration.Variable;
import ca.sapon.jici.parser.statement.Empty;
import ca.sapon.jici.parser.statement.Import;
import ca.sapon.jici.parser.statement.Statement;
import ca.sapon.jici.parser.type.ClassTypeName;
import ca.sapon.jici.parser.type.PrimitiveTypeName;
import ca.sapon.jici.parser.type.TypeName;
import ca.sapon.jici.util.ListNavigator;

public final class Parser {
    private Parser() {
    }

    public static List<Statement> parse(List<Token> tokens) {
        final List<Statement> statements = new ArrayList<>();
        final ListNavigator<Token> navigableTokens = new ListNavigator<>(tokens);
        while (navigableTokens.has()) {
            statements.add(parseStatement(navigableTokens));
        }
        return statements;
    }

    public static Statement parseStatement(List<Token> tokens) {
        return parseStatement(new ListNavigator<>(tokens));
    }

    public static Expression parseExpression(List<Token> tokens) {
        return parseExpression(new ListNavigator<>(tokens));
    }

    private static Statement parseStatement(ListNavigator<Token> tokens) {
        final Token token = tokens.get();
        if (token.getID() == TokenID.SYMBOL_SEMICOLON) {
            tokens.advance();
            return new Empty(token.getIndex());
        }
        // try to parse an import
        try {
            tokens.pushPosition();
            final Import _import = parseImport(tokens);
            tokens.discardPosition();
            return _import;
        } catch (ParseFailure exception) {
            tokens.popPosition();
        }
        // try to parse a declaration
        try {
            tokens.pushPosition();
            final Declaration declaration = parseDeclaration(tokens);
            tokens.discardPosition();
            return declaration;
        } catch (ParseFailure exception) {
            tokens.popPosition();
        }
        // try to parse an expression that is also a statement
        final Expression expression = parseExpression(tokens);
        if (expression instanceof Statement) {
            if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_SEMICOLON) {
                tokens.advance();
                return (Statement) expression;
            }
            throw new ParseError("Expected ';'", tokens);
        }
        throw new ParseError("Expected a statement", token.getStart(), tokens.get(tokens.remaining() - 1).getEnd());
    }

    /*
        CLASS_NAME:      IDENTIFIER.CLASS_NAME _ IDENTIFIER

        TYPE_NAME:       CLASS_NAME _ PRIMITIVE_TYPE_NAME

        VARIABLE:        IDENTIFIER _ IDENTIFIER = EXPRESSION
        VARIABLE_LIST:   VARIABLE, VARIABLE_LIST _ VARIABLE

        DECLARATION:     TYPE VARIABLE_LIST;
    */

    private static List<Identifier> parseClassName(ListNavigator<Token> tokens) {
        return parseClassName(tokens, new ArrayList<Identifier>());
    }

    private static List<Identifier> parseClassName(ListNavigator<Token> tokens, List<Identifier> name) {
        if (tokens.has()) {
            final Token token = tokens.get();
            if (token instanceof Identifier) {
                tokens.advance();
                name.add((Identifier) token);
                if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_PERIOD) {
                    tokens.advance();
                    return parseClassName(tokens, name);
                }
                return name;
            } else if (!name.isEmpty()) {
                tokens.retreat();
                return name;
            }
        }
        throw new ParseError("Expected an identifier", tokens);
    }

    private static TypeName parseTypeName(ListNavigator<Token> tokens) {
        if (tokens.has()) {
            final Token token = tokens.get();
            if (token.getGroup() == TokenGroup.PRIMITIVE_TYPE) {
                tokens.advance();
                return new PrimitiveTypeName((Keyword) token);
            }
            return new ClassTypeName(parseClassName(tokens));
        }
        throw new ParseError("Expected an identifier or a primitive type", tokens);
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
        throw new ParseFailure();
    }

    private static List<Variable> parseVariableList(ListNavigator<Token> tokens) {
        return parseVariableList(tokens, new ArrayList<Variable>());
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
            final TypeName type;
            try {
                type = parseTypeName(tokens);
            } catch (ParserException exception) {
                throw new ParseFailure();
            }
            final Declaration declaration = new Declaration(type, parseVariableList(tokens));
            if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_SEMICOLON) {
                tokens.advance();
                return declaration;
            }
            throw new ParseError("Expected ';'", tokens);
        }
        throw new ParseFailure();
    }

    /*
        IMPORT:          import CLASS_NAME; _ import CLASS_NAME.*;
    */

    private static Import parseImport(ListNavigator<Token> tokens) {
        if (tokens.has() && tokens.get().getID() == TokenID.KEYWORD_IMPORT) {
            tokens.advance();
            final List<Identifier> name = parseClassName(tokens);
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
                throw new ParseError("Expected ';'", tokens);
            }
            throw new ParseError("Expected '*' or ';'", tokens);
        }
        throw new ParseFailure();
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
        ACCESS:          ACCESS.IDENTIFIER _ ACCESS.IDENTIFIER(EXPRESSION_LIST) _ ACCESS[EXPRESSION] _ ATOM

        ATOM:            LITERAL _ CLASS_NAME _ CLASS_NAME(EXPRESSION_LIST) _ new CLASS_NAME(EXPRESSION_LIST) _ TYPE.class _ (EXPRESSION)
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
        if (tokens.has()) {
            final Token token = tokens.get();
            if (token.getGroup() == TokenGroup.ASSIGNMENT) {
                if (assignee instanceof Reference) {
                    tokens.advance();
                    final Expression value = parseAssignment(tokens);
                    return new Assignment((Reference) assignee, value, (Symbol) token);
                }
                throw new ParseError("Expected a reference", assignee);
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
            throw new ParseError("Expected ':'", tokens);
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
            if (token.getGroup() == TokenGroup.EQUAL_OPERATOR) {
                tokens.advance();
                final Expression right = parseComparison(tokens);
                final Equal add = new Equal(left, right, (Symbol) token);
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
            if (token.getGroup() == TokenGroup.COMPARISON_OPERATOR) {
                tokens.advance();
                final Expression right = parseShift(tokens);
                final Comparison add = new Comparison(left, right, (Symbol) token);
                return parseComparison(tokens, add);
            } else if (token.getID() == TokenID.KEYWORD_INSTANCEOF) {
                tokens.advance();
                final ClassTypeName type = new ClassTypeName(parseClassName(tokens));
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
            if (token.getGroup() == TokenGroup.SHIFT_OPERATOR) {
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
            if (token.getGroup() == TokenGroup.ADD_OPERATOR) {
                tokens.advance();
                final Expression right = parseMultiply(tokens);
                final Arithmetic arithmetic = new Arithmetic(left, right, (Symbol) token);
                return parseAdd(tokens, arithmetic);
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
            if (token.getGroup() == TokenGroup.MULTIPLY_OPERATOR) {
                tokens.advance();
                final Expression right = parseUnary(tokens);
                final Arithmetic arithmetic = new Arithmetic(left, right, (Symbol) token);
                return parseMultiply(tokens, arithmetic);
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
                    if (inner instanceof Reference) {
                        return new PreIncrement((Reference) inner, (Symbol) token);
                    }
                    throw new ParseError("Expected a reference", inner);
                }
                case SYMBOL_PLUS:
                case SYMBOL_MINUS: {
                    tokens.advance();
                    final Expression inner = parseUnary(tokens);
                    if (inner instanceof NumberLiteral) {
                        ((NumberLiteral) inner).applySign(token.getID() == TokenID.SYMBOL_MINUS);
                        return inner;
                    } else {
                        return new Sign(inner, (Symbol) token);
                    }
                }
                case SYMBOL_BOOLEAN_NOT: {
                    tokens.advance();
                    final Expression inner = parseUnary(tokens);
                    return new BooleanNot(inner);
                }
                case SYMBOL_BITWISE_NOT: {
                    tokens.advance();
                    final Expression inner = parseUnary(tokens);
                    return new BitwiseNot(inner);
                }
                case SYMBOL_OPEN_PARENTHESIS: {
                    if (tokens.has(2)) {
                        tokens.pushPosition();
                        tokens.advance();
                        try {
                            final TypeName type = parseTypeName(tokens);
                            if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_CLOSE_PARENTHESIS) {
                                tokens.advance();
                                final Expression inner = parseUnary(tokens);
                                tokens.discardPosition();
                                return new Cast(type, inner);
                            }
                        } catch (ParserException exception) {
                            // this is not a cast, but an access
                        }
                        tokens.popPosition();
                    }
                }
            }
            final Expression inner = parseAccess(tokens);
            return parseUnary(tokens, inner);
        }
        throw new ParseError("Expected a token", tokens);
    }

    private static Expression parseUnary(ListNavigator<Token> tokens, Expression inner) {
        if (tokens.has()) {
            final Token token = tokens.get();
            switch (token.getID()) {
                case SYMBOL_INCREMENT:
                case SYMBOL_DECREMENT: {
                    tokens.advance();
                    if (inner instanceof Reference) {
                        final PostIncrement outer = new PostIncrement((Reference) inner, (Symbol) token);
                        return parseUnary(tokens, outer);
                    }
                    throw new ParseError("Expected a reference", inner);
                }
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
                        final Token token = tokens.get();
                        if (token.getGroup() == TokenGroup.IDENTIFIER) {
                            tokens.advance();
                            final Expression access;
                            if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_OPEN_PARENTHESIS) {
                                tokens.advance();
                                final List<Expression> arguments = parseArguments(tokens);
                                access = new MethodCall(object, (Identifier) token, arguments);
                            } else {
                                access = new FieldAccess(object, (Identifier) token);
                            }
                            return parseAccess(tokens, access);
                        }
                    }
                    throw new ParseError("Expected an identifier", tokens);
                }
                case SYMBOL_OPEN_BRACKET: {
                    tokens.advance();
                    final Expression index = parseExpression(tokens);
                    if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_CLOSE_BRACKET) {
                        tokens.advance();
                        final IndexAccess access = new IndexAccess(object, index);
                        return parseAccess(tokens, access);
                    }
                    throw new ParseError("Expected ']'", tokens);
                }
            }
        }
        return object;
    }

    private static Expression parseAtom(ListNavigator<Token> tokens) {
        if (tokens.has()) {
            final Token token = tokens.get();
            switch (token.getGroup()) {
                case LITERAL: {
                    tokens.advance();
                    return (Literal) token;
                }
                case PRIMITIVE_TYPE: {
                    tokens.advance();
                    if (tokens.has(2) && tokens.get(0).getID() == TokenID.SYMBOL_PERIOD && tokens.get(1).getID() == TokenID.KEYWORD_CLASS) {
                        tokens.advance(2);
                        return new ClassAccess(new PrimitiveTypeName((Keyword) token));
                    }
                    tokens.retreat();
                    break;
                }
                case IDENTIFIER: {
                    final List<Identifier> name = parseClassName(tokens);
                    if (tokens.has()) {
                        switch (tokens.get().getID()) {
                            case SYMBOL_PERIOD: {
                                tokens.advance();
                                if (tokens.has() && tokens.get().getID() == TokenID.KEYWORD_CLASS) {
                                    tokens.advance();
                                    return new ClassAccess(new ClassTypeName(name));
                                }
                                tokens.retreat();
                                break;
                            }
                            case SYMBOL_OPEN_PARENTHESIS: {
                                tokens.advance();
                                if (name.size() == 1) {
                                    throw new ParseError("Local methods are not supported", name.get(0));
                                } else {
                                    final List<Expression> arguments = parseArguments(tokens);
                                    return new AmbiguousCall(name, arguments);
                                }
                            }
                        }
                    }
                    if (name.size() == 1) {
                        return new VariableAccess(name.get(0));
                    } else {
                        return new AmbiguousReference(name);
                    }
                }
                default: {
                    switch (token.getID()) {
                        case SYMBOL_OPEN_PARENTHESIS: {
                            tokens.advance();
                            final Expression expression = parseExpression(tokens);
                            if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_CLOSE_PARENTHESIS) {
                                tokens.advance();
                                return expression;
                            }
                            throw new ParseError("Expected ')'", tokens);
                        }
                        case KEYWORD_NEW: {
                            tokens.advance();
                            final ClassTypeName name = new ClassTypeName(parseClassName(tokens));
                            if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_OPEN_PARENTHESIS) {
                                tokens.advance();
                                final List<Expression> arguments = parseArguments(tokens);
                                return new ConstructorCall(name, arguments);
                            }
                            throw new ParseError("Expected '('", tokens);
                        }
                    }
                }
            }
        }
        throw new ParseError("Expected a literal, an identifier, \"new\" or '('", tokens);
    }

    private static List<Expression> parseArguments(ListNavigator<Token> tokens) {
        final List<Expression> arguments;
        if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_CLOSE_PARENTHESIS) {
            tokens.advance();
            arguments = Collections.emptyList();
        } else {
            arguments = parseExpressionList(tokens);
            if (!tokens.has() || tokens.get().getID() != TokenID.SYMBOL_CLOSE_PARENTHESIS) {
                throw new ParseError("Expected ')'", tokens);
            }
            tokens.advance();
        }
        return arguments;
    }

    private static class ParseFailure extends ParserException {
        private static final long serialVersionUID = 1;

        private ParseFailure() {
            super("parse failure", 0, 0);
        }
    }

    private static class ParseError extends ParserException {
        private static final long serialVersionUID = 1;

        private ParseError(String error, ListNavigator<Token> tokens) {
            super(error, getErrorStart(tokens), getErrorEnd(tokens));
        }

        private ParseError(String error, SourceIndexed indexed) {
            this(error, indexed.getStart(), indexed.getEnd());
        }

        private ParseError(String error, int start, int end) {
            super(error, start, end);
        }
    }

    private static int getErrorStart(ListNavigator<Token> tokens) {
        if (tokens.has()) {
            return tokens.get().getIndex();
        }
        if (tokens.position() > 0) {
            final Token token = tokens.get(-1);
            return token.getIndex() + token.getSource().length();
        }
        return 0;
    }

    private static int getErrorEnd(ListNavigator<Token> tokens) {
        if (tokens.has()) {
            final Token token = tokens.get();
            return token.getIndex() + token.getSource().length() - 1;
        }
        return getErrorStart(tokens);
    }
}
