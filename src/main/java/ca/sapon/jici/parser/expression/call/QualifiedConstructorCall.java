package ca.sapon.jici.parser.expression.call;

import java.util.List;

import ca.sapon.jici.parser.expression.Expression;
import ca.sapon.jici.parser.name.ClassTypeName;
import ca.sapon.jici.parser.name.TypeArgumentName;

/**
 *
 */
public class QualifiedConstructorCall extends ConstructorCall {
    private final Expression target;

    public QualifiedConstructorCall(Expression target, ClassTypeName typeName, List<Expression> arguments, boolean diamondOperator, int start, int end) {
        super(typeName, arguments, diamondOperator, start, end);
        this.target = target;
    }

    public QualifiedConstructorCall(Expression target, ClassTypeName typeName, List<TypeArgumentName> typeArguments, boolean diamondOperator, List<Expression> arguments, int start, int end) {
        super(typeName, typeArguments, diamondOperator, arguments, start, end);
        this.target = target;
    }

    @Override
    protected String expressionString() {
        return target + "." + super.expressionString();
    }
}
