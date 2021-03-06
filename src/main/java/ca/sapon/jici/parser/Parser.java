/*
 * This file is part of JICI, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015-2016 Aleksi Sapon <http://sapon.ca/jici/>
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
import ca.sapon.jici.lexer.TokenGroup;
import ca.sapon.jici.lexer.TokenID;
import ca.sapon.jici.lexer.literal.Literal;
import ca.sapon.jici.lexer.literal.number.IntLiteral;
import ca.sapon.jici.lexer.literal.number.LongLiteral;
import ca.sapon.jici.parser.expression.ArrayConstructor;
import ca.sapon.jici.parser.expression.ArrayConstructor.ArrayInitializer;
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
import ca.sapon.jici.parser.expression.call.QualifiedConstructorCall;
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
import ca.sapon.jici.parser.name.ArrayTypeName;
import ca.sapon.jici.parser.name.ClassTypeName;
import ca.sapon.jici.parser.name.InnerClassTypeName;
import ca.sapon.jici.parser.name.PrimitiveTypeName;
import ca.sapon.jici.parser.name.TypeArgumentName;
import ca.sapon.jici.parser.name.TypeArgumentName.BoundKind;
import ca.sapon.jici.parser.name.TypeName;
import ca.sapon.jici.parser.statement.Declaration;
import ca.sapon.jici.parser.statement.Declaration.Variable;
import ca.sapon.jici.parser.statement.Empty;
import ca.sapon.jici.parser.statement.Import;
import ca.sapon.jici.parser.statement.Statement;
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
        final ListNavigator<Token> navigableTokens = new ListNavigator<>(tokens);
        final Expression expression = parseExpression(navigableTokens);
        if (navigableTokens.has()) {
            throw new ParseError("Expected end of expression", navigableTokens);
        }
        return expression;
    }

    public static TypeName parseTypeName(List<Token> tokens) {
        return parseTypeName(new ListNavigator<>(tokens));
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
        NAME:            IDENTIFIER.NAME _ IDENTIFIER

        CLASS_NAME:      NAME _ NAME TYPE_ARG_LIST _ NAME.CLASS_NAME _ NAME TYPE_ARG_LIST.CLASS_NAME
        ARRAY_NAME:      CLASS_NAME[] _ PRIMITIVE_TYPE_NAME[] _ ARRAY_NAME[]
        TYPE_NAME:       CLASS_NAME _ PRIMITIVE_TYPE_NAME _ ARRAY_NAME

        TYPE_ARG:        TYPE_NAME _ ? _ ? extends TYPE_NAME _ ? super TYPE_NAME
        TYPE_ARG_LIST:   <TYPE_ARG, TYPE_ARG_LIST> _ <TYPE_ARG>
    */

    private static List<Identifier> parseName(ListNavigator<Token> tokens) {
        return parseName(tokens, new ArrayList<Identifier>());
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
            }
            if (!name.isEmpty()) {
                tokens.retreat();
                return name;
            }
        }
        throw new ParseError("Expected an identifier", tokens);
    }

    private static ClassTypeName parseClassName(ListNavigator<Token> tokens) {
        final List<Identifier> name = parseName(tokens);
        final List<TypeArgumentName> typeArguments = parseTypeArgumentNameList(tokens);
        final int end = tokens.get(-1).getEnd();
        return parseClassName(tokens, new ClassTypeName(name, typeArguments, end));
    }

    private static ClassTypeName parseClassName(ListNavigator<Token> tokens, ClassTypeName outer) {
        if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_PERIOD) {
            tokens.advance();
            final List<Identifier> name = parseName(tokens);
            final List<TypeArgumentName> typeArguments = parseTypeArgumentNameList(tokens);
            final int end = tokens.get(-1).getEnd();
            return parseClassName(tokens, new InnerClassTypeName(outer, name, typeArguments, end));
        }
        return outer;
    }

    private static TypeName parseArrayName(ListNavigator<Token> tokens, TypeName componentType) {
        final int dimensions = parseArrayDimensions(tokens);
        if (dimensions > 0) {
            final int end = tokens.get(-1).getEnd();
            return new ArrayTypeName(componentType, dimensions, end);
        }
        return componentType;
    }

    private static int parseArrayDimensions(ListNavigator<Token> tokens) {
        return parseArrayDimensions(tokens, 0);
    }

    private static int parseArrayDimensions(ListNavigator<Token> tokens, int dimensions) {
        if (tokens.has(2) && tokens.get(0).getID() == TokenID.SYMBOL_OPEN_BRACKET && tokens.get(1).getID() == TokenID.SYMBOL_CLOSE_BRACKET) {
            tokens.advance(2);
            return parseArrayDimensions(tokens, dimensions + 1);
        }
        return dimensions;
    }

    private static TypeName parseTypeName(ListNavigator<Token> tokens) {
        if (tokens.has()) {
            final TypeName type;
            final Token token = tokens.get();
            if (token.getGroup() == TokenGroup.PRIMITIVE_TYPE) {
                tokens.advance();
                type = new PrimitiveTypeName((Keyword) token);
            } else {
                type = parseClassName(tokens);
            }
            return parseArrayName(tokens, type);
        }
        throw new ParseError("Expected an identifier or a primitive type", tokens);
    }

    private static TypeArgumentName parseTypeArgumentName(ListNavigator<Token> tokens) {
        if (tokens.has()) {
            final Token token = tokens.get();
            if (token.getID() == TokenID.SYMBOL_QUESTION_MARK) {
                final int start = tokens.get().getStart();
                tokens.advance();
                if (tokens.has()) {
                    switch (tokens.get().getID()) {
                        case KEYWORD_EXTENDS: {
                            tokens.advance();
                            final TypeName type = parseTypeName(tokens);
                            return new TypeArgumentName(type, BoundKind.UPPER, start);
                        }
                        case KEYWORD_SUPER: {
                            tokens.advance();
                            final TypeName type = parseTypeName(tokens);
                            return new TypeArgumentName(type, BoundKind.LOWER, start);
                        }
                    }
                }
                return new TypeArgumentName(start);
            } else {
                final TypeName type = parseTypeName(tokens);
                return new TypeArgumentName(type, BoundKind.EXACT, type.getStart());
            }
        }
        throw new ParseError("Expected a type name or '?'", tokens);
    }

    private static List<TypeArgumentName> parseTypeArgumentNameList(ListNavigator<Token> tokens) {
        final List<TypeArgumentName> typeArgumentNames = new ArrayList<>();
        if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_LESSER) {
            // Ignore cases of the diamond operator (don't treat it as as type argument name list)
            if (tokens.has(2) && tokens.get(1).getID() == TokenID.SYMBOL_GREATER) {
                return typeArgumentNames;
            }
            tokens.advance();
            parseTypeArgumentNameList(tokens, typeArgumentNames);
            if (tokens.has()) {
                switch (tokens.get().getID()) {
                    case SYMBOL_GREATER:
                        tokens.advance();
                        return typeArgumentNames;
                    case SYMBOL_ARITHMETIC_RIGHT_SHIFT:
                        tokens.advanceFractional();
                        if (tokens.fractional() == 2) {
                            tokens.closeFractional();
                        }
                        return typeArgumentNames;
                    case SYMBOL_LOGICAL_RIGHT_SHIFT:
                        tokens.advanceFractional();
                        if (tokens.fractional() == 3) {
                            tokens.closeFractional();
                        }
                        return typeArgumentNames;
                }
            }
            throw new ParseError("Expected '>'", tokens);
        }
        return typeArgumentNames;
    }

    private static List<TypeArgumentName> parseTypeArgumentNameList(ListNavigator<Token> tokens, List<TypeArgumentName> typeArgumentNames) {
        typeArgumentNames.add(parseTypeArgumentName(tokens));
        if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_COMMA) {
            tokens.advance();
            return parseTypeArgumentNameList(tokens, typeArgumentNames);
        }
        return typeArgumentNames;
    }

    /*
        VARIABLE:        IDENTIFIER _ IDENTIFIER = EXPRESSION _ IDENTIFIER = ARRAY_INIT
        VARIABLE_LIST:   VARIABLE, VARIABLE_LIST _ VARIABLE

        DECLARATION:     TYPE VARIABLE_LIST;
    */

    private static Variable parseVariable(ListNavigator<Token> tokens) {
        if (tokens.has()) {
            final Token token = tokens.get();
            if (token instanceof Identifier) {
                tokens.advance();
                final int dimensions = parseArrayDimensions(tokens);
                if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_ASSIGN) {
                    tokens.advance();
                    Expression value;
                    try {
                        value = parseArrayInitializer(tokens);
                    } catch (ParseFailure ignored) {
                        value = parseExpression(tokens);
                    }
                    return new Variable((Identifier) token, dimensions, value);
                }
                return new Variable((Identifier) token, dimensions);
            }
        }
        throw new ParseFailure();
    }

    private static List<Variable> parseVariableList(ListNavigator<Token> tokens) {
        return parseVariableList(tokens, new ArrayList<Variable>());
    }

    private static List<Variable> parseVariableList(ListNavigator<Token> tokens, List<Variable> variables) {
        variables.add(parseVariable(tokens));
        if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_COMMA) {
            tokens.advance();
            return parseVariableList(tokens, variables);
        }
        return variables;
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
        ARRAY_INIT:      { ARRAY_CONTENTS } _ { ARRAY_CONTENTS, } _ { } _ {, }
        ARRAY_CONTENTS:   EXPRESSION, ARRAY_CONTENTS _ ARRAY_INIT, ARRAY_CONTENTS _ EXPRESSION _ ARRAY_INIT
    */

    private static ArrayInitializer parseArrayInitializer(ListNavigator<Token> tokens) {
        if (tokens.has()) {
            final Token startToken = tokens.get();
            if (startToken.getID() == TokenID.SYMBOL_OPEN_BRACE) {
                tokens.advance();
                final List<Expression> contents = parseArrayContents(tokens);
                if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_COMMA) {
                    tokens.advance();
                }
                if (tokens.has()) {
                    final Token endToken = tokens.get();
                    if (endToken.getID() == TokenID.SYMBOL_CLOSE_BRACE) {
                        tokens.advance();
                        return new ArrayInitializer(contents, startToken.getStart(), endToken.getEnd());
                    }
                }
                throw new ParseError("Expected '}", tokens);
            }
        }
        throw new ParseFailure();
    }

    private static List<Expression> parseArrayContents(ListNavigator<Token> tokens) {
        return parseArrayContents(tokens, new ArrayList<Expression>());
    }

    private static List<Expression> parseArrayContents(ListNavigator<Token> tokens, List<Expression> contents) {
        try {
            contents.add(parseArrayInitializer(tokens));
        } catch (ParseFailure failure) {
            try {
                contents.add(parseExpression(tokens));
            } catch (ParseError ignored) {
                return contents;
            }
        }
        if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_COMMA) {
            tokens.advance();
            return parseArrayContents(tokens, contents);
        }
        return contents;
    }

    /*
        IMPORT:          import CLASS_NAME; _ import CLASS_NAME.*;
    */

    private static Import parseImport(ListNavigator<Token> tokens) {
        if (tokens.has() && tokens.get().getID() == TokenID.KEYWORD_IMPORT) {
            final int start = tokens.get().getStart();
            tokens.advance();
            final List<Identifier> name = parseName(tokens);
            if (tokens.has()) {
                final Token token = tokens.get();
                if (token.getID() == TokenID.SYMBOL_PERIOD && tokens.has(2) && tokens.get(1).getID() == TokenID.SYMBOL_MULTIPLY) {
                    tokens.advance(2);
                    if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_SEMICOLON) {
                        final int end = tokens.get().getEnd();
                        tokens.advance();
                        return new Import(name, true, start, end);
                    }
                } else if (token.getID() == TokenID.SYMBOL_SEMICOLON) {
                    final int end = tokens.get().getEnd();
                    tokens.advance();
                    return new Import(name, false, start, end);
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
        ACCESS:          ACCESS.IDENTIFIER _ ACCESS.IDENTIFIER ARGUMENTS _ ACCESS[EXPRESSION]
                         ACCESS.CTOR_CALL _ ATOM

        ATOM:            LITERAL _ NAME _ NAME.IDENTIFIER ARGUMENTS _ NAME.<TYPE_ARG_LIST>IDENTIFIER ARGUMENTS
                         CTOR_CALL _ ARRAY_CTOR _ TYPE_NAME.class _ void.class _ (EXPRESSION)

        CTOR_CALL:       new CLASS_NAME ARGUMENTS _ new <TYPE_ARG_LIST>CLASS_NAME ARGUMENTS
                         new CLASS_NAME<>ARGUMENTS _ new <TYPE_ARG_LIST>CLASS_NAME<>ARGUMENTS
        ARRAY_CTOR:      new ARRAY_NAME ARRAY_INIT _ new CLASS_NAME ARRAY_SIZES

        ARGUMENTS:       (EXPRESSION_LIST)

        ARRAY_SIZES:     SIZED_ARRAYS _ SIZED_ARRAYS UNSIZED_ARRAYS
        SIZED_ARRAYS:    [EXPRESSION] _ SIZED_ARRAYS[EXPRESSION]
        UNSIZED_ARRAYS:  [] _ UNSIZED_ARRAYS[]
    */

    private static Expression parseExpression(ListNavigator<Token> tokens) {
        return parseAssignment(tokens);
    }

    private static List<Expression> parseExpressionList(ListNavigator<Token> tokens) {
        return parseExpressionList(tokens, new ArrayList<Expression>());
    }

    private static List<Expression> parseExpressionList(ListNavigator<Token> tokens, List<Expression> expressions) {
        expressions.add(parseExpression(tokens));
        if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_COMMA) {
            tokens.advance();
            return parseExpressionList(tokens, expressions);
        }
        return expressions;
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
                final TypeName type = parseTypeName(tokens);
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
                    if (inner instanceof IntLiteral) {
                        return ((IntLiteral) inner).withSign((Symbol) token);
                    }
                    if (inner instanceof LongLiteral) {
                        return ((LongLiteral) inner).withSign((Symbol) token);
                    }
                    return new Sign(inner, (Symbol) token);
                }
                case SYMBOL_BOOLEAN_NOT: {
                    final int start = tokens.get().getStart();
                    tokens.advance();
                    final Expression inner = parseUnary(tokens);
                    return new BooleanNot(inner, start);
                }
                case SYMBOL_BITWISE_NOT: {
                    final int start = tokens.get().getStart();
                    tokens.advance();
                    final Expression inner = parseUnary(tokens);
                    return new BitwiseNot(inner, start);
                }
                case SYMBOL_OPEN_PARENTHESIS: {
                    if (tokens.has(2)) {
                        tokens.pushPosition();
                        final int start = tokens.get().getStart();
                        tokens.advance();
                        try {
                            final TypeName type = parseTypeName(tokens);
                            if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_CLOSE_PARENTHESIS) {
                                tokens.advance();
                                final Expression inner = parseUnary(tokens);
                                tokens.discardPosition();
                                return new Cast(type, inner, start);
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
        if (!tokens.has()) {
            return object;
        }
        switch (tokens.get().getID()) {
            case SYMBOL_PERIOD: {
                tokens.advance();
                if (!tokens.has()) {
                    throw new ParseError("Expected a token", tokens);
                }
                final Token token = tokens.get();
                // Look for a qualified constructor call
                if (token.getID() == TokenID.KEYWORD_NEW) {
                    final Expression constructorCall = parseQualifiedConstructorCall(tokens, object);
                    return parseAccess(tokens, constructorCall);
                }
                // Else check for a method call or field access
                final int start = token.getStart();
                // Start with optional type arguments
                final List<TypeArgumentName> typeArguments = parseTypeArgumentNameList(tokens);
                // Then check for an identifier
                if (!tokens.has() || tokens.get().getGroup() != TokenGroup.IDENTIFIER) {
                    throw new ParseError("Expected an identifier", tokens);
                }
                final Token identifier = tokens.get();
                tokens.advance();
                // Now go looking for arguments for a method call. If none found then we have a field access
                final Expression access;
                if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_OPEN_PARENTHESIS) {
                    final List<Expression> arguments = parseArgumentList(tokens);
                    final int end = tokens.get(-1).getEnd();
                    access = new MethodCall(object, (Identifier) identifier, typeArguments, arguments, start, end);
                } else {
                    if (!typeArguments.isEmpty()) {
                        throw new ParseError("Type arguments not accepted here", typeArguments.get(0));
                    }
                    access = new FieldAccess(object, (Identifier) identifier);
                }
                return parseAccess(tokens, access);
            }
            case SYMBOL_OPEN_BRACKET: {
                tokens.advance();
                final Expression index = parseExpression(tokens);
                if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_CLOSE_BRACKET) {
                    final int end = tokens.get().getEnd();
                    tokens.advance();
                    final IndexAccess access = new IndexAccess(object, index, end);
                    return parseAccess(tokens, access);
                }
                throw new ParseError("Expected ']'", tokens);
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
                    tokens.pushPosition();
                    tokens.advance();
                    final int dimensions = parseArrayDimensions(tokens);
                    if (tokens.has(2) && tokens.get(0).getID() == TokenID.SYMBOL_PERIOD && tokens.get(1).getID() == TokenID.KEYWORD_CLASS) {
                        final int end = tokens.get().getEnd();
                        tokens.advance(2);
                        TypeName type = new PrimitiveTypeName((Keyword) token);
                        if (dimensions > 0) {
                            final int nameEnd = tokens.get(-1).getEnd();
                            type = new ArrayTypeName(type, dimensions, nameEnd);
                        }
                        tokens.discardPosition();
                        return new ClassAccess(type, end);
                    }
                    tokens.popPosition();
                    break;
                }
                case IDENTIFIER: {
                    final List<Identifier> name = parseName(tokens);
                    // try for a class access
                    tokens.pushPosition();
                    final int dimensions = parseArrayDimensions(tokens);
                    if (tokens.has(2) && tokens.get(0).getID() == TokenID.SYMBOL_PERIOD && tokens.get(1).getID() == TokenID.KEYWORD_CLASS) {
                        final int end = tokens.get(1).getEnd();
                        tokens.advance(2);
                        TypeName type = new ClassTypeName(name);
                        if (dimensions > 0) {
                            final int nameEnd = tokens.get(-1).getEnd();
                            type = new ArrayTypeName(type, dimensions, nameEnd);
                        }
                        tokens.discardPosition();
                        return new ClassAccess(type, end);
                    }
                    tokens.popPosition();
                    // look for type arguments
                    final int start;
                    final List<TypeArgumentName> typeArguments;
                    if (tokens.has(2) && tokens.get(0).getID() == TokenID.SYMBOL_PERIOD && tokens.get(1).getID() == TokenID.SYMBOL_LESSER) {
                        start = tokens.get(1).getStart();
                        tokens.advance(1);
                        typeArguments = parseTypeArgumentNameList(tokens);
                    } else {
                        start = name.get(name.size() - 1).getStart();
                        typeArguments = Collections.emptyList();
                    }
                    if (!typeArguments.isEmpty()) {
                        if (tokens.has() && tokens.get().getGroup() == TokenGroup.IDENTIFIER) {
                            // add the method identifier after the type arguments to full name
                            name.add((Identifier) tokens.get());
                            tokens.advance();
                        } else {
                            throw new ParseError("Expected identifier", tokens);
                        }
                    }
                    // check for a method call by looking for arguments
                    if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_OPEN_PARENTHESIS) {
                        final List<Expression> arguments = parseArgumentList(tokens);
                        final int end = tokens.get(-1).getEnd();
                        return new AmbiguousCall(name, typeArguments, arguments, start, end);
                    }
                    // this is a name access, either a variable of a field
                    if (!typeArguments.isEmpty()) {
                        throw new ParseError("Type arguments not accepted here", typeArguments.get(0));
                    }
                    if (name.size() == 1) {
                        return new VariableAccess(name.get(0));
                    }
                    return new AmbiguousReference(name);
                }
                default: {
                    switch (token.getID()) {
                        case SYMBOL_OPEN_PARENTHESIS: {
                            // Parenthesized expression
                            int start = token.getStart();
                            tokens.advance();
                            final Expression expression = parseExpression(tokens);
                            if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_CLOSE_PARENTHESIS) {
                                int end = tokens.get().getEnd();
                                tokens.advance();
                                expression.setStart(start);
                                expression.setEnd(end);
                                return expression;
                            }
                            throw new ParseError("Expected ')'", tokens);
                        }
                        case KEYWORD_NEW: {
                            return parseConstructor(tokens);
                        }
                        case KEYWORD_VOID: {
                            // Void class access
                            tokens.advance();
                            if (tokens.has(2) && tokens.get(0).getID() == TokenID.SYMBOL_PERIOD && tokens.get(1).getID() == TokenID.KEYWORD_CLASS) {
                                final int end = tokens.get(1).getEnd();
                                tokens.advance(2);
                                return new ClassAccess(token, end);
                            }
                            tokens.retreat();
                            break;
                        }
                    }
                }
            }
        }
        throw new ParseError("Expected either a literal, a type class access, an identifier, \"new\" or '('", tokens);
    }

    private static Expression parseConstructor(ListNavigator<Token> tokens) {
        // Try for an array constructor first
        tokens.pushPosition();
        try {
            final Expression arrayConstructor = parseArrayConstructor(tokens);
            tokens.discardPosition();
            return arrayConstructor;
        } catch (ParseFailure exception) {
            // Not an array constructor, try for a constructor call
        }
        tokens.popPosition();
        return parseConstructorCall(tokens);
    }

    private static Expression parseConstructorCall(ListNavigator<Token> tokens) {
        if (!tokens.has() || tokens.get().getID() != TokenID.KEYWORD_NEW) {
            throw new ParseError("Expected \"new\"", tokens);
        }
        final int start = tokens.get().getStart();
        tokens.advance();
        // Try for constructor type arguments
        final List<TypeArgumentName> typeArguments;
        if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_LESSER) {
            typeArguments = parseTypeArgumentNameList(tokens);
        } else {
            typeArguments = Collections.emptyList();
        }
        // Now parse the class name
        final ClassTypeName className = parseClassName(tokens);
        // If the class name doesn't have type arguments we can have a diamond operator
        final boolean diamondOperator;
        if (!className.hasArguments() && tokens.has(2)
                && tokens.get(0).getID() == TokenID.SYMBOL_LESSER
                && tokens.get(1).getID() == TokenID.SYMBOL_GREATER) {
            tokens.advance(2);
            diamondOperator = true;
        } else {
            diamondOperator = false;
        }
        // Now we need a list of arguments
        final List<Expression> arguments = parseArgumentList(tokens);
        final int end = tokens.get(-1).getEnd();
        return new ConstructorCall(className, typeArguments, diamondOperator, arguments, start, end);
    }

    private static Expression parseArrayConstructor(ListNavigator<Token> tokens) {
        if (!tokens.has() || tokens.get().getID() != TokenID.KEYWORD_NEW) {
            throw new ParseError("Expected \"new\"", tokens);
        }
        final int start = tokens.get().getStart();
        tokens.advance();
        // Now attempt to parse the type name, can be a class, primitive or array name
        final TypeName typeName;
        try {
            typeName = parseTypeName(tokens);
        } catch (ParseError error) {
            throw new ParseFailure();
        }
        // If it's an array name then we expect an initializer afterwards
        if (typeName instanceof ArrayTypeName) {
            try {
                final ArrayInitializer initializer = parseArrayInitializer(tokens);
                final int end = tokens.get(-1).getEnd();
                return new ArrayConstructor((ArrayTypeName) typeName, initializer, start, end);
            } catch (ParseFailure failure) {
                throw new ParseError("Expected '{'", tokens);
            }
        }
        // Otherwise we go looking for sized array dimensions followed by unsized ones
        if (!tokens.has() || tokens.get().getID() != TokenID.SYMBOL_OPEN_BRACKET) {
            throw new ParseFailure();
        }
        final List<Expression> sizes = parseArraySizes(tokens);
        final int end = tokens.get(-1).getEnd();
        return new ArrayConstructor(typeName, sizes, start, end);
    }

    private static Expression parseQualifiedConstructorCall(ListNavigator<Token> tokens, Expression target) {
        if (!tokens.has() || tokens.get().getID() != TokenID.KEYWORD_NEW) {
            throw new ParseError("Expected \"new\"", tokens);
        }
        final int start = tokens.get().getStart();
        tokens.advance();
        // Try for constructor type arguments
        final List<TypeArgumentName> typeArguments;
        if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_LESSER) {
            typeArguments = parseTypeArgumentNameList(tokens);
        } else {
            typeArguments = Collections.emptyList();
        }
        // Now parse the class name, which unlike the unqualified constructor call is just a single identifier
        if (!tokens.has() || tokens.get().getGroup() != TokenGroup.IDENTIFIER) {
            throw new ParseError("Expected an identifier", tokens);
        }
        final Identifier identifier = (Identifier) tokens.get();
        tokens.advance();
        // Next we can have either type arguments for the class type or a diamond operator
        final List<TypeArgumentName> classTypeArguments;
        final boolean diamondOperator;
        final int classTypeEnd;
        if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_LESSER) {
            if (tokens.has(2) && tokens.get(1).getID() == TokenID.SYMBOL_GREATER) {
                tokens.advance(2);
                classTypeArguments = Collections.emptyList();
                diamondOperator = true;
                classTypeEnd = identifier.getEnd();
            } else {
                classTypeArguments = parseTypeArgumentNameList(tokens);
                diamondOperator = false;
                classTypeEnd = tokens.get(-1).getEnd();
            }
        } else {
            classTypeArguments = Collections.emptyList();
            diamondOperator = false;
            classTypeEnd = identifier.getEnd();
        }
        // Finally we have an argument list
        final List<Expression> arguments = parseArgumentList(tokens);
        final int end = tokens.get(-1).getEnd();
        return new QualifiedConstructorCall(target, identifier, typeArguments, classTypeArguments, diamondOperator, arguments, classTypeEnd, start, end);
    }

    private static List<Expression> parseArgumentList(ListNavigator<Token> tokens) {
        if (!tokens.has() || tokens.get().getID() != TokenID.SYMBOL_OPEN_PARENTHESIS) {
            throw new ParseError("Expected '('", tokens);
        }
        tokens.advance();
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

    private static List<Expression> parseArraySizes(ListNavigator<Token> tokens) {
        if (!tokens.has() || tokens.get().getID() != TokenID.SYMBOL_OPEN_BRACKET) {
            throw new ParseError("Expected '['", tokens);
        }
        tokens.advance();
        final List<Expression> sizes = new ArrayList<>();
        parseSizedArrays(tokens, sizes);
        if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_OPEN_BRACKET) {
            tokens.advance();
            parseUnsizedArrays(tokens, sizes);
        }
        return sizes;
    }

    private static List<Expression> parseSizedArrays(ListNavigator<Token> tokens, List<Expression> sizes) {
        if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_CLOSE_BRACKET) {
            if (sizes.isEmpty()) {
                throw new ParseError("Expected array size expression", tokens);
            }
            tokens.retreat();
            return sizes;
        }
        final Expression expression = parseExpression(tokens);
        if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_CLOSE_BRACKET) {
            sizes.add(expression);
            tokens.advance();
            if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_OPEN_BRACKET) {
                tokens.advance();
                return parseSizedArrays(tokens, sizes);
            }
            return sizes;
        }
        throw new ParseError("Expected ']'", tokens);
    }

    private static List<Expression> parseUnsizedArrays(ListNavigator<Token> tokens, List<Expression> sizes) {
        if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_CLOSE_BRACKET) {
            tokens.advance();
            sizes.add(null);
            if (tokens.has() && tokens.get().getID() == TokenID.SYMBOL_OPEN_BRACKET) {
                tokens.advance();
                return parseUnsizedArrays(tokens, sizes);
            }
            return sizes;
        }
        throw new ParseError("Expected ']'", tokens);
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
