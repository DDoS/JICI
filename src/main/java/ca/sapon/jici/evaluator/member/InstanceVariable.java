package ca.sapon.jici.evaluator.member;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import ca.sapon.jici.evaluator.type.LiteralReferenceType;
import ca.sapon.jici.evaluator.type.ReferenceType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.type.TypeArgument;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.util.TypeUtil;

/**
 *
 */
public class InstanceVariable implements ClassVariable {
    private final LiteralReferenceType declaror;
    private final Field field;
    private final Type type;
    private final Type targetType;

    private InstanceVariable(LiteralReferenceType declaror, Field field) {
        this.declaror = declaror;
        this.field = field;
        // Raw types require the erasure of all type information on non-static members
        if (declaror.isRaw() && !Modifier.isStatic(field.getModifiers())) {
            Type type = TypeUtil.wrap(field.getGenericType());
            if (type instanceof ReferenceType) {
                type = ((ReferenceType) type).getErasure();
            }
            this.type = type;
            targetType = type;
        } else {
            // Get the type be applying the substitutions from the declaror capture
            Type type = TypeUtil.wrap(field.getGenericType());
            if (type instanceof TypeArgument) {
                type = ((TypeArgument) type).substituteTypeVariables(declaror.capture().getSubstitutions());
            }
            this.type = type.capture();
            targetType = type;
        }
    }

    @Override
    public LiteralReferenceType getDeclaringType() {
        return declaror;
    }

    @Override
    public String getName() {
        return field.getName();
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Type getTargetType() {
        return targetType;
    }

    @Override
    public boolean isStatic() {
        return Modifier.isStatic(field.getModifiers());
    }

    @Override
    public Value getValue(Value target) {
        try {
            return type.getKind().wrap(field.get(target.asObject()));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public void setValue(Value target, Value value) {
        try {
            field.set(target.asObject(), value.asObject());
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public String toString() {
        return field.getName();
    }

    public static InstanceVariable of(LiteralReferenceType declaror, Field field) {
        // Make sure the field actually belongs to the declaror
        if (field.getDeclaringClass() != declaror.getTypeClass()) {
            throw new IllegalArgumentException("The given field " + field + " was not declared by class " + declaror);
        }
        return new InstanceVariable(declaror, field);
    }
}
