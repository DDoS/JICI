package ca.sapon.jici.evaluator.member;

import java.lang.reflect.Array;

import ca.sapon.jici.evaluator.type.LiteralReferenceType;
import ca.sapon.jici.evaluator.type.PrimitiveType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.value.IntValue;
import ca.sapon.jici.evaluator.value.Value;

/**
 *
 */
public class ArrayLengthVariable implements ClassVariable {
    private final LiteralReferenceType declaror;

    private ArrayLengthVariable(LiteralReferenceType declaror) {
        this.declaror = declaror;
    }

    @Override
    public LiteralReferenceType getDeclaringType() {
        return declaror;
    }

    @Override
    public String getName() {
        return "length";
    }

    @Override
    public Type getType() {
        return PrimitiveType.THE_INT;
    }

    @Override
    public Type getTargetType() {
        return PrimitiveType.THE_INT;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public Value getValue(Value target) {
        try {
            return IntValue.of(Array.getLength(target.asObject()));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public void setValue(Value target, Value value) {
        throw new UnsupportedOperationException("Array length field is final");
    }

    @Override
    public String toString() {
        return "length";
    }

    public static ArrayLengthVariable of(LiteralReferenceType declaror) {
        if (!declaror.isArray()) {
            throw new IllegalArgumentException("Cannot declaror an array cloning method on the non-array type " + declaror);
        }
        return new ArrayLengthVariable(declaror);
    }
}
