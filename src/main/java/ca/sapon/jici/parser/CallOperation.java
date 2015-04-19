package ca.sapon.jici.parser;

import java.util.List;

public class CallOperation implements Expression, Statement {
    private final Expression method;
    private final List<Expression> arguments;

    public CallOperation(Expression method, List<Expression> arguments) {
        this.method = method;
        this.arguments = arguments;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder("CallOperation(");
        builder.append(method).append('(');
        final int size = arguments.size() - 1;
        if (size >= 0) {
            for (int i = 0; i < size; i++) {
                builder.append(arguments.get(i));
                builder.append(", ");
            }
            builder.append(arguments.get(size));
        }
        builder.append("))");
        return builder.toString();
    }
}
