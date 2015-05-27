package ca.sapon.jici.parser.name;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.type.VoidType;

/**
 *
 */
public class VoidTypeName implements TypeName {
    private final int index;

    public VoidTypeName(int index) {
        this.index = index;
    }

    @Override
    public Type getType(Environment environment) {
        return VoidType.THE_VOID;
    }

    @Override
    public int getStart() {
        return index;
    }

    @Override
    public int getEnd() {
        return index + 3;
    }

    @Override
    public String toString() {
        return "void";
    }
}
