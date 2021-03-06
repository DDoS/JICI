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
package ca.sapon.jici.lexer;

public enum TokenID {
    IDENTIFIER(TokenGroup.IDENTIFIER),
    KEYWORD_ASSERT(TokenGroup.UNSPECIFIED),
    KEYWORD_IF(TokenGroup.UNSPECIFIED),
    KEYWORD_ELSE(TokenGroup.UNSPECIFIED),
    KEYWORD_WHILE(TokenGroup.UNSPECIFIED),
    KEYWORD_DO(TokenGroup.UNSPECIFIED),
    KEYWORD_FOR(TokenGroup.UNSPECIFIED),
    KEYWORD_BREAK(TokenGroup.UNSPECIFIED),
    KEYWORD_CONTINUE(TokenGroup.UNSPECIFIED),
    KEYWORD_SWITCH(TokenGroup.UNSPECIFIED),
    KEYWORD_CASE(TokenGroup.UNSPECIFIED),
    KEYWORD_DEFAULT(TokenGroup.UNSPECIFIED),
    KEYWORD_RETURN(TokenGroup.UNSPECIFIED),
    KEYWORD_THROW(TokenGroup.UNSPECIFIED),
    KEYWORD_TRY(TokenGroup.UNSPECIFIED),
    KEYWORD_CATCH(TokenGroup.UNSPECIFIED),
    KEYWORD_FINALLY(TokenGroup.UNSPECIFIED),
    KEYWORD_VOID(TokenGroup.UNSPECIFIED),
    KEYWORD_BOOLEAN(TokenGroup.PRIMITIVE_TYPE),
    KEYWORD_BYTE(TokenGroup.PRIMITIVE_TYPE),
    KEYWORD_SHORT(TokenGroup.PRIMITIVE_TYPE),
    KEYWORD_CHAR(TokenGroup.PRIMITIVE_TYPE),
    KEYWORD_INT(TokenGroup.PRIMITIVE_TYPE),
    KEYWORD_LONG(TokenGroup.PRIMITIVE_TYPE),
    KEYWORD_FLOAT(TokenGroup.PRIMITIVE_TYPE),
    KEYWORD_DOUBLE(TokenGroup.PRIMITIVE_TYPE),
    KEYWORD_CLASS(TokenGroup.CLASS_TYPE),
    KEYWORD_INTERFACE(TokenGroup.CLASS_TYPE),
    KEYWORD_ENUM(TokenGroup.CLASS_TYPE),
    KEYWORD_EXTENDS(TokenGroup.UNSPECIFIED),
    KEYWORD_IMPLEMENTS(TokenGroup.UNSPECIFIED),
    KEYWORD_SUPER(TokenGroup.SELF_REFERENCE),
    KEYWORD_THIS(TokenGroup.SELF_REFERENCE),
    KEYWORD_PACKAGE(TokenGroup.UNSPECIFIED),
    KEYWORD_IMPORT(TokenGroup.UNSPECIFIED),
    KEYWORD_PUBLIC(TokenGroup.ACCESS_MODIFIER),
    KEYWORD_PROTECTED(TokenGroup.ACCESS_MODIFIER),
    KEYWORD_PRIVATE(TokenGroup.ACCESS_MODIFIER),
    KEYWORD_THROWS(TokenGroup.UNSPECIFIED),
    KEYWORD_ABSTRACT(TokenGroup.OTHER_MODIFIER),
    KEYWORD_STRICTFP(TokenGroup.OTHER_MODIFIER),
    KEYWORD_TRANSIENT(TokenGroup.OTHER_MODIFIER),
    KEYWORD_VOLATILE(TokenGroup.OTHER_MODIFIER),
    KEYWORD_FINAL(TokenGroup.OTHER_MODIFIER),
    KEYWORD_STATIC(TokenGroup.OTHER_MODIFIER),
    KEYWORD_SYNCHRONIZED(TokenGroup.OTHER_MODIFIER),
    KEYWORD_NATIVE(TokenGroup.OTHER_MODIFIER),
    KEYWORD_NEW(TokenGroup.UNSPECIFIED),
    KEYWORD_INSTANCEOF(TokenGroup.BINARY_OPERATOR),
    KEYWORD_GOTO(TokenGroup.UNUSED),
    KEYWORD_CONST(TokenGroup.UNUSED),
    SYMBOL_PERIOD(TokenGroup.CALL_OPERATOR),
    SYMBOL_COMMA(TokenGroup.UNSPECIFIED),
    SYMBOL_COLON(TokenGroup.UNSPECIFIED),
    SYMBOL_SEMICOLON(TokenGroup.UNSPECIFIED),
    SYMBOL_PLUS(TokenGroup.ADD_OPERATOR),
    SYMBOL_MINUS(TokenGroup.ADD_OPERATOR),
    SYMBOL_MULTIPLY(TokenGroup.MULTIPLY_OPERATOR),
    SYMBOL_DIVIDE(TokenGroup.MULTIPLY_OPERATOR),
    SYMBOL_MODULO(TokenGroup.MULTIPLY_OPERATOR),
    SYMBOL_INCREMENT(TokenGroup.UNARY_OPERATOR),
    SYMBOL_DECREMENT(TokenGroup.UNARY_OPERATOR),
    SYMBOL_BITWISE_AND(TokenGroup.BINARY_OPERATOR),
    SYMBOL_BITWISE_OR(TokenGroup.BINARY_OPERATOR),
    SYMBOL_BITWISE_NOT(TokenGroup.UNARY_OPERATOR),
    SYMBOL_BITWISE_XOR(TokenGroup.BINARY_OPERATOR),
    SYMBOL_LOGICAL_LEFT_SHIFT(TokenGroup.SHIFT_OPERATOR),
    SYMBOL_ARITHMETIC_RIGHT_SHIFT(TokenGroup.SHIFT_OPERATOR),
    SYMBOL_LOGICAL_RIGHT_SHIFT(TokenGroup.SHIFT_OPERATOR),
    SYMBOL_BOOLEAN_NOT(TokenGroup.UNARY_OPERATOR),
    SYMBOL_BOOLEAN_AND(TokenGroup.BINARY_OPERATOR),
    SYMBOL_BOOLEAN_OR(TokenGroup.BINARY_OPERATOR),
    SYMBOL_LESSER(TokenGroup.COMPARISON_OPERATOR),
    SYMBOL_GREATER(TokenGroup.COMPARISON_OPERATOR),
    SYMBOL_GREATER_OR_EQUAL(TokenGroup.COMPARISON_OPERATOR),
    SYMBOL_LESSER_OR_EQUAL(TokenGroup.COMPARISON_OPERATOR),
    SYMBOL_EQUAL(TokenGroup.EQUAL_OPERATOR),
    SYMBOL_NOT_EQUAL(TokenGroup.EQUAL_OPERATOR),
    SYMBOL_ASSIGN(TokenGroup.ASSIGNMENT),
    SYMBOL_ADD_ASSIGN(TokenGroup.ASSIGNMENT),
    SYMBOL_SUBTRACT_ASSIGN(TokenGroup.ASSIGNMENT),
    SYMBOL_MULTIPLY_ASSIGN(TokenGroup.ASSIGNMENT),
    SYMBOL_DIVIDE_ASSIGN(TokenGroup.ASSIGNMENT),
    SYMBOL_REMAINDER_ASSIGN(TokenGroup.ASSIGNMENT),
    SYMBOL_BITWISE_AND_ASSIGN(TokenGroup.ASSIGNMENT),
    SYMBOL_BITWISE_OR_ASSIGN(TokenGroup.ASSIGNMENT),
    SYMBOL_BITWISE_XOR_ASSIGN(TokenGroup.ASSIGNMENT),
    SYMBOL_LOGICAL_LEFT_SHIFT_ASSIGN(TokenGroup.ASSIGNMENT),
    SYMBOL_ARITHMETIC_RIGHT_SHIFT_ASSIGN(TokenGroup.ASSIGNMENT),
    SYMBOL_LOGICAL_RIGHT_SHIFT_ASSIGN(TokenGroup.ASSIGNMENT),
    SYMBOL_OPEN_PARENTHESIS(TokenGroup.UNSPECIFIED),
    SYMBOL_CLOSE_PARENTHESIS(TokenGroup.UNSPECIFIED),
    SYMBOL_OPEN_BRACKET(TokenGroup.UNSPECIFIED),
    SYMBOL_CLOSE_BRACKET(TokenGroup.UNSPECIFIED),
    SYMBOL_OPEN_BRACE(TokenGroup.UNSPECIFIED),
    SYMBOL_CLOSE_BRACE(TokenGroup.UNSPECIFIED),
    SYMBOL_DOUBLE_SLASH(TokenGroup.COMMENT_DELIMITER),
    SYMBOL_SLASH_STAR(TokenGroup.COMMENT_DELIMITER),
    SYMBOL_STAR_SLASH(TokenGroup.COMMENT_DELIMITER),
    SYMBOL_QUESTION_MARK(TokenGroup.UNSPECIFIED),
    SYMBOL_DOUBLE_PERIOD(TokenGroup.UNUSED),
    SYMBOL_TRIPLE_PERIOD(TokenGroup.UNSPECIFIED),
    SYMBOL_DOUBLE_COLON(TokenGroup.UNSPECIFIED),
    SYMBOL_ARROW(TokenGroup.UNSPECIFIED),
    SYMBOL_AT(TokenGroup.UNSPECIFIED),
    LITERAL_TRUE(TokenGroup.LITERAL),
    LITERAL_FALSE(TokenGroup.LITERAL),
    LITERAL_CHARACTER(TokenGroup.LITERAL),
    LITERAL_NULL(TokenGroup.LITERAL),
    LITERAL_STRING(TokenGroup.LITERAL),
    LITERAL_DOUBLE(TokenGroup.LITERAL),
    LITERAL_FLOAT(TokenGroup.LITERAL),
    LITERAL_INT(TokenGroup.LITERAL),
    LITERAL_LONG(TokenGroup.LITERAL);
    private final TokenGroup group;

    TokenID(TokenGroup group) {
        this.group = group;
    }

    public TokenGroup getGroup() {
        return group;
    }
}
