package ca.sapon.jici.parser.expression.call;

import java.util.List;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.evaluator.member.Callable;
import ca.sapon.jici.evaluator.type.LiteralReferenceType;
import ca.sapon.jici.evaluator.type.ReferenceType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.type.TypeArgument;
import ca.sapon.jici.evaluator.type.WildcardType;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.lexer.Identifier;
import ca.sapon.jici.parser.expression.Expression;
import ca.sapon.jici.parser.name.TypeArgumentName;
import ca.sapon.jici.parser.statement.Statement;
import ca.sapon.jici.util.StringUtil;

/**
 *
 */
public class QualifiedConstructorCall implements Statement, Expression {
    private final Expression target;
    private final Identifier inner;
    private final List<TypeArgumentName> typeArguments;
    private final List<TypeArgumentName> classTypeArguments;
    private final boolean diamondOperator;
    private final List<Expression> arguments;
    private final int classTypeEnd;
    private int start;
    private int end;
    private Callable callable = null;

    public QualifiedConstructorCall(Expression target, Identifier inner, List<TypeArgumentName> typeArguments, List<TypeArgumentName> classTypeArguments, boolean diamondOperator,
                                    List<Expression> arguments, int classTypeEnd, int start, int end) {
        if (diamondOperator && !classTypeArguments.isEmpty()) {
            throw new IllegalArgumentException("Cannot use the diamond operator and have class type arguments at the same time");
        }
        this.target = target;
        this.inner = inner;
        this.typeArguments = typeArguments;
        this.classTypeArguments = classTypeArguments;
        this.diamondOperator = diamondOperator;
        this.arguments = arguments;
        this.classTypeEnd = classTypeEnd;
        this.start = start;
        this.end = end;
    }

    @Override
    public void execute(Environment environment) {
        try {
            getType(environment);
            getValue(environment);
        } catch (EvaluatorException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new EvaluatorException(exception, this);
        }
    }

    @Override
    public Type getType(Environment environment) {
        if (callable != null) {
            return callable.getReturnType();
        }
        // Check the target type first
        final Type targetType = target.getType(environment);
        if (!(targetType instanceof ReferenceType)) {
            throw new EvaluatorException("Expected a reference type", target);
        }
        final ReferenceType enclosingType = (ReferenceType) targetType;
        // Now check the class type arguments
        final TypeArgument[] classTypeArguments = checkClassTypeArguments(environment);
        // Check the type to be instantiated
        final LiteralReferenceType type = checkInstantiationType(enclosingType, classTypeArguments);
        // Check type arguments
        final TypeArgument[] typeArguments = ConstructorCall.checkTypeArguments(environment, this.typeArguments, diamondOperator);
        // Check argument types
        final Type[] argumentTypes = MethodCall.checkArguments(environment, arguments);
        // The enclosing class is the first argument of the constructor
        final Type[] innerArgumentTypes = new Type[argumentTypes.length + 1];
        System.arraycopy(argumentTypes, 0, innerArgumentTypes, 1, argumentTypes.length);
        innerArgumentTypes[0] = enclosingType;
        // Get the constructor to call
        try {
            callable = type.getConstructor(typeArguments, innerArgumentTypes);
        } catch (UnsupportedOperationException exception) {
            throw new EvaluatorException(exception.getMessage(), this);
        }
        return callable.getReturnType();
    }

    private TypeArgument[] checkClassTypeArguments(Environment environment) {
        final TypeArgument[] typeArguments = new TypeArgument[classTypeArguments.size()];
        for (int i = 0; i < classTypeArguments.size(); i++) {
            final TypeArgumentName typeArgumentName = classTypeArguments.get(i);
            final TypeArgument typeArgument = typeArgumentName.getType(environment);
            if (typeArgument instanceof WildcardType) {
                throw new EvaluatorException("Cannot use wildcards as type arguments in constructor calls", typeArgumentName);
            }
            typeArguments[i] = typeArgument;
        }
        return typeArguments;
    }

    private LiteralReferenceType checkInstantiationType(ReferenceType enclosingType, TypeArgument[] classTypeArguments) {
        // Check that the diamond operator is not being used when type arguments are already given
        if (!typeArguments.isEmpty() && diamondOperator) {
            throw new EvaluatorException("Cannot use the diamond operator when type arguments are provided",
                    typeArguments.get(0).getStart(), typeArguments.get(typeArguments.size() - 1).getEnd());
        }
        // Go looking for an inner class of the enclosing type
        final LiteralReferenceType type;
        try {
            type = enclosingType.getInnerClass(inner.getSource(), classTypeArguments);
        } catch (UnsupportedOperationException exception) {
            throw new EvaluatorException(exception.getMessage(), this);
        }
        // Now check that we can actually instantiate the type
        if (type.isEnum()) {
            throw new EvaluatorException("Cannot instantiate the enum class " + type, inner.getStart(), classTypeEnd);
        }
        if (type.isAbstract()) {
            throw new EvaluatorException("Cannot instantiate the abstract class " + type, inner.getStart(), classTypeEnd);
        }
        if (!type.isPublic()) {
            throw new EvaluatorException("Cannot access class " + type, inner.getStart(), classTypeEnd);
        }
        if (diamondOperator && !type.isRaw()) {
            // Check that the diamond operator is being used on a generic type
            throw new EvaluatorException("Cannot use the diamond operator on the non-generic type " + type, inner.getStart(), classTypeEnd);
        }
        return type;
    }

    @Override
    public Value getValue(Environment environment) {
        final Value enclosing = target.getValue(environment);
        final int size = arguments.size();
        final Value[] values = new Value[size + 1];
        values[0] = enclosing;
        for (int i = 0; i < size; i++) {
            values[i + 1] = arguments.get(i).getValue(environment);
        }
        try {
            return callable.call(null, values);
        } catch (Exception exception) {
            throw new EvaluatorException("Could not call constructor", exception, this);
        }
    }

    @Override
    public int getStart() {
        return start;
    }

    @Override
    public int getEnd() {
        return end;
    }

    @Override
    public void setStart(int start) {
        this.start = start;
    }

    @Override
    public void setEnd(int end) {
        this.end = end;
    }

    @Override
    public String toString() {
        return "QualifiedConstructorCall(" + target + ".new " + toString(typeArguments) +
                inner + (diamondOperator ? "<>" : toString(classTypeArguments)) + "(" + StringUtil.toString(arguments, ", ") + ")" + ")";
    }

    private static String toString(List<TypeArgumentName> typeArguments) {
        return !typeArguments.isEmpty() ? '<' + StringUtil.toString(typeArguments, ", ") + '>' : "";
    }
}
