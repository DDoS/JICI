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

public enum TokenID {
    IDENTIFIER(TokenType.IDENTIFIER),
    KEYWORD_ASSERT(TokenType.UNSPECIFIED),
    KEYWORD_IF(TokenType.UNSPECIFIED),
    KEYWORD_ELSE(TokenType.UNSPECIFIED),
    KEYWORD_WHILE(TokenType.UNSPECIFIED),
    KEYWORD_DO(TokenType.UNSPECIFIED),
    KEYWORD_FOR(TokenType.UNSPECIFIED),
    KEYWORD_BREAK(TokenType.UNSPECIFIED),
    KEYWORD_CONTINUE(TokenType.UNSPECIFIED),
    KEYWORD_SWITCH(TokenType.UNSPECIFIED),
    KEYWORD_CASE(TokenType.UNSPECIFIED),
    KEYWORD_DEFAULT(TokenType.UNSPECIFIED),
    KEYWORD_RETURN(TokenType.UNSPECIFIED),
    KEYWORD_THROW(TokenType.UNSPECIFIED),
    KEYWORD_TRY(TokenType.UNSPECIFIED),
    KEYWORD_CATCH(TokenType.UNSPECIFIED),
    KEYWORD_FINALLY(TokenType.UNSPECIFIED),
    KEYWORD_VOID(TokenType.UNSPECIFIED),
    KEYWORD_BOOLEAN(TokenType.PRIMITIVE_TYPE),
    KEYWORD_BYTE(TokenType.PRIMITIVE_TYPE),
    KEYWORD_SHORT(TokenType.PRIMITIVE_TYPE),
    KEYWORD_CHAR(TokenType.PRIMITIVE_TYPE),
    KEYWORD_INT(TokenType.PRIMITIVE_TYPE),
    KEYWORD_LONG(TokenType.PRIMITIVE_TYPE),
    KEYWORD_FLOAT(TokenType.PRIMITIVE_TYPE),
    KEYWORD_DOUBLE(TokenType.PRIMITIVE_TYPE),
    KEYWORD_CLASS(TokenType.CLASS_TYPE),
    KEYWORD_INTERFACE(TokenType.CLASS_TYPE),
    KEYWORD_ENUM(TokenType.CLASS_TYPE),
    KEYWORD_EXTENDS(TokenType.UNSPECIFIED),
    KEYWORD_IMPLEMENTS(TokenType.UNSPECIFIED),
    KEYWORD_SUPER(TokenType.SELF_REFERENCE),
    KEYWORD_THIS(TokenType.SELF_REFERENCE),
    KEYWORD_PACKAGE(TokenType.UNSPECIFIED),
    KEYWORD_IMPORT(TokenType.UNSPECIFIED),
    KEYWORD_PUBLIC(TokenType.ACCESS_MODIFIER),
    KEYWORD_PROTECTED(TokenType.ACCESS_MODIFIER),
    KEYWORD_PRIVATE(TokenType.ACCESS_MODIFIER),
    KEYWORD_THROWS(TokenType.UNSPECIFIED),
    KEYWORD_ABSTRACT(TokenType.OTHER_MODIFIER),
    KEYWORD_STRICTFP(TokenType.OTHER_MODIFIER),
    KEYWORD_TRANSIENT(TokenType.OTHER_MODIFIER),
    KEYWORD_VOLATILE(TokenType.OTHER_MODIFIER),
    KEYWORD_FINAL(TokenType.OTHER_MODIFIER),
    KEYWORD_STATIC(TokenType.OTHER_MODIFIER),
    KEYWORD_SYNCHRONIZED(TokenType.OTHER_MODIFIER),
    KEYWORD_NATIVE(TokenType.OTHER_MODIFIER),
    KEYWORD_NEW(TokenType.UNSPECIFIED),
    KEYWORD_INSTANCEOF(TokenType.BINARY_OPERATOR),
    KEYWORD_GOTO(TokenType.UNUSED),
    KEYWORD_CONST(TokenType.UNUSED),
    SYMBOL_PERIOD(TokenType.CALL_OPERATOR),
    SYMBOL_COMMA(TokenType.UNSPECIFIED),
    SYMBOL_COLON(TokenType.UNSPECIFIED),
    SYMBOL_SEMICOLON(TokenType.UNSPECIFIED),
    SYMBOL_PLUS(TokenType.ADD_OPERATOR),
    SYMBOL_MINUS(TokenType.ADD_OPERATOR),
    SYMBOL_MULTIPLY(TokenType.MULTIPLY_OPERATOR),
    SYMBOL_DIVIDE(TokenType.MULTIPLY_OPERATOR),
    SYMBOL_MODULO(TokenType.MULTIPLY_OPERATOR),
    SYMBOL_INCREMENT(TokenType.UNARY_OPERATOR),
    SYMBOL_DECREMENT(TokenType.UNARY_OPERATOR),
    SYMBOL_BITWISE_AND(TokenType.BINARY_OPERATOR),
    SYMBOL_BITWISE_OR(TokenType.BINARY_OPERATOR),
    SYMBOL_BITWISE_NOT(TokenType.UNARY_OPERATOR),
    SYMBOL_BITWISE_XOR(TokenType.BINARY_OPERATOR),
    SYMBOL_LOGICAL_LEFT_SHIFT(TokenType.SHIFT_OPERATOR),
    SYMBOL_ARITHMETIC_RIGHT_SHIFT(TokenType.SHIFT_OPERATOR),
    SYMBOL_LOGICAL_RIGHT_SHIFT(TokenType.SHIFT_OPERATOR),
    SYMBOL_BOOLEAN_NOT(TokenType.UNARY_OPERATOR),
    SYMBOL_BOOLEAN_AND(TokenType.BINARY_OPERATOR),
    SYMBOL_BOOLEAN_OR(TokenType.BINARY_OPERATOR),
    SYMBOL_LESSER(TokenType.COMPARISON_OPERATOR),
    SYMBOL_GREATER(TokenType.COMPARISON_OPERATOR),
    SYMBOL_GREATER_OR_EQUAL(TokenType.COMPARISON_OPERATOR),
    SYMBOL_LESSER_OR_EQUAL(TokenType.COMPARISON_OPERATOR),
    SYMBOL_EQUAL(TokenType.EQUAL_OPERATOR),
    SYMBOL_NOT_EQUAL(TokenType.EQUAL_OPERATOR),
    SYMBOL_ASSIGN(TokenType.ASSIGNMENT),
    SYMBOL_ADD_ASSIGN(TokenType.ASSIGNMENT),
    SYMBOL_SUBTRACT_ASSIGN(TokenType.ASSIGNMENT),
    SYMBOL_MULTIPLY_ASSIGN(TokenType.ASSIGNMENT),
    SYMBOL_DIVIDE_ASSIGN(TokenType.ASSIGNMENT),
    SYMBOL_REMAINDER_ASSIGN(TokenType.ASSIGNMENT),
    SYMBOL_BITWISE_AND_ASSIGN(TokenType.ASSIGNMENT),
    SYMBOL_BITWISE_OR_ASSIGN(TokenType.ASSIGNMENT),
    SYMBOL_BITWISE_XOR_ASSIGN(TokenType.ASSIGNMENT),
    SYMBOL_LOGICAL_LEFT_SHIFT_ASSIGN(TokenType.ASSIGNMENT),
    SYMBOL_ARITHMETIC_RIGHT_SHIFT_ASSIGN(TokenType.ASSIGNMENT),
    SYMBOL_LOGICAL_RIGHT_SHIFT_ASSIGN(TokenType.ASSIGNMENT),
    SYMBOL_OPEN_PARENTHESIS(TokenType.UNSPECIFIED),
    SYMBOL_CLOSE_PARENTHESIS(TokenType.UNSPECIFIED),
    SYMBOL_OPEN_BRACKET(TokenType.UNSPECIFIED),
    SYMBOL_CLOSE_BRACKET(TokenType.UNSPECIFIED),
    SYMBOL_OPEN_BRACE(TokenType.UNSPECIFIED),
    SYMBOL_CLOSE_BRACE(TokenType.UNSPECIFIED),
    SYMBOL_DOUBLE_SLASH(TokenType.COMMENT_DELIMITER),
    SYMBOL_SLASH_STAR(TokenType.COMMENT_DELIMITER),
    SYMBOL_STAR_SLASH(TokenType.COMMENT_DELIMITER),
    SYMBOL_QUESTION_MARK(TokenType.UNSPECIFIED),
    SYMBOL_DOUBLE_PERIOD(TokenType.UNUSED),
    SYMBOL_TRIPLE_PERIOD(TokenType.UNSPECIFIED),
    SYMBOL_DOUBLE_COLON(TokenType.UNSPECIFIED),
    SYMBOL_ARROW(TokenType.UNSPECIFIED),
    SYMBOL_AT(TokenType.UNSPECIFIED),
    LITERAL_TRUE(TokenType.LITERAL),
    LITERAL_FALSE(TokenType.LITERAL),
    LITERAL_CHARACTER(TokenType.LITERAL),
    LITERAL_NULL(TokenType.LITERAL),
    LITERAL_STRING(TokenType.LITERAL),
    LITERAL_DOUBLE(TokenType.LITERAL),
    LITERAL_FLOAT(TokenType.LITERAL),
    LITERAL_INT(TokenType.LITERAL),
    LITERAL_LONG(TokenType.LITERAL);
    private final TokenType type;

    private TokenID(TokenType type) {
        this.type = type;
    }

    public TokenType getType() {
        return type;
    }
}
