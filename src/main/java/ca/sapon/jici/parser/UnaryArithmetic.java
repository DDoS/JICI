package ca.sapon.jici.parser;

import ca.sapon.jici.lexer.Symbol;

public class UnaryArithmetic extends UnaryOperation {
    private final Symbol operator;

    public UnaryArithmetic(Expression inner, boolean post, Symbol operator) {
        super(inner, post);
        this.operator = operator;
    }

    @Override
    public String toString() {
        return "UnaryArithmetic(" + (post ? inner.toString() + operator.toString() : operator.toString() + inner.toString()) + ")";
    }
}
