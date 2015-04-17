package ca.sapon.jici.parser;

import ca.sapon.jici.lexer.Symbol;

public class BinaryArithmetic extends BinaryOperation {
    private final Symbol operator;

    public BinaryArithmetic(Expression left, Expression right, Symbol operator) {
        super(left, right);
        this.operator = operator;
    }

    @Override
    public String toString() {
        return "BinaryArithmetic(" + left + " " + operator + " " + right + ")";
    }
}
