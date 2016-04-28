package ca.sapon.jici.evaluator.member;

import ca.sapon.jici.evaluator.type.LiteralReferenceType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.type.TypeVariable;
import ca.sapon.jici.evaluator.value.ObjectValue;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.util.ReflectionUtil;

/**
 *
 */
public class ArrayCloneCallable implements Callable {
    private final LiteralReferenceType declaror;

    private ArrayCloneCallable(LiteralReferenceType declaror) {
        this.declaror = declaror;
    }

    @Override
    public LiteralReferenceType getDeclaringType() {
        return declaror;
    }

    @Override
    public TypeVariable[] getTypeParameters() {
        return new TypeVariable[0];
    }

    @Override
    public Type[] getParameterTypes() {
        return new Type[0];
    }

    @Override
    public Type getReturnType() {
        return declaror;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public boolean supportsVararg() {
        return false;
    }

    @Override
    public boolean isVarargEnabled() {
        return false;
    }

    @Override
    public ArrayCloneCallable useVararg() {
        throw new UnsupportedOperationException("Array constructor clone method does not support vararg");
    }

    @Override
    public boolean isApplicable(Type[] argumentTypes) {
        return argumentTypes.length == 0;
    }

    @Override
    public boolean isMoreApplicableThan(Callable other, Type[] argumentTypes) {
        return other.getParameterTypes().length > 0;
    }

    @Override
    public Value call(Value target, Value... arguments) {
        try {
            return ObjectValue.of(ReflectionUtil.cloneArray(target.asObject()));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public String toString() {
        return "public " + declaror.getName() + "[] clone()";
    }

    public static ArrayCloneCallable of(LiteralReferenceType declaror) {
        if (!declaror.isArray()) {
            throw new IllegalArgumentException("Cannot declaror an array cloning method on the non-array type " + declaror);
        }
        return new ArrayCloneCallable(declaror);
    }
}
