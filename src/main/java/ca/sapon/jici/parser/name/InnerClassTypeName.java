package ca.sapon.jici.parser.name;

import java.util.Collections;
import java.util.List;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.type.LiteralReferenceType;
import ca.sapon.jici.evaluator.type.ParametrizedType;
import ca.sapon.jici.lexer.Identifier;

/**
 *
 */
public class InnerClassTypeName extends ClassTypeName {
    private final ClassTypeName outerName;
    private ParametrizedType outerType;
    private boolean typeCached = false;

    public InnerClassTypeName(ClassTypeName outerName, List<Identifier> name) {
        this(outerName, name, Collections.<TypeArgumentName>emptyList());
    }

    public InnerClassTypeName(ClassTypeName outerName, List<Identifier> name, List<TypeArgumentName> arguments) {
        super(name, arguments);
        this.outerName = outerName;
    }

    @Override
    protected ParametrizedType getOwner(Environment environment) {
        if (!typeCached) {
            final LiteralReferenceType outerType = outerName.getType(environment);
            this.outerType = outerType instanceof ParametrizedType ? (ParametrizedType) outerType : null;
            typeCached = true;
        }
        return outerType;
    }

    @Override
    public int getStart() {
        return outerName.getStart();
    }

    @Override
    public String toString() {
        return outerName.toString() + '.' + super.toString();
    }
}
