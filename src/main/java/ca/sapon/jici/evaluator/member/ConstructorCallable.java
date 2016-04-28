package ca.sapon.jici.evaluator.member;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import ca.sapon.jici.evaluator.Substitutions;
import ca.sapon.jici.evaluator.type.LiteralReferenceType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.type.TypeArgument;
import ca.sapon.jici.evaluator.type.TypeVariable;
import ca.sapon.jici.evaluator.value.Value;

/**
 *
 */
public class ConstructorCallable extends DeclaredCallable {
    private final Constructor<?> constructor;

    private ConstructorCallable(Constructor<?> constructor, LiteralReferenceType declaror, TypeVariable[] typeParameters, Substitutions substitutions) {
        super(declaror, typeParameters, declaror.capture(), constructor.getGenericParameterTypes(), substitutions);
        this.constructor = constructor;
    }

    private ConstructorCallable(Constructor<?> constructor, LiteralReferenceType declaror, TypeVariable[] typeParameters, Type[] parameterTypes, Type returnType, boolean varargEnabled) {
        super(declaror, typeParameters, parameterTypes, returnType, varargEnabled);
        this.constructor = constructor;
    }

    @Override
    public String getName() {
        return constructor.getName();
    }

    @Override
    public boolean isStatic() {
        return Modifier.isStatic(constructor.getModifiers());
    }

    @Override
    public boolean supportsVararg() {
        return constructor.isVarArgs();
    }

    @Override
    public ConstructorCallable useVararg() {
        if (!supportsVararg()) {
            throw new UnsupportedOperationException("This constructor does not support vararg");
        }
        return new ConstructorCallable(constructor, getDeclaringType(), getTypeParameters(), getParameterTypes(), getReturnType(), true);
    }

    @Override
    public Value call(Value target, Value... arguments) {
        final Object[] values = unwrapArguments(arguments);
        try {
            return getReturnType().getKind().wrap(constructor.newInstance(values));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public String toString() {
        return constructor.toString();
    }

    public static ConstructorCallable of(LiteralReferenceType declaror, Constructor<?> constructor, TypeArgument[] typeArguments) {
        // Make sure the constructor actually belongs to the declaror
        if (constructor.getDeclaringClass() != declaror.getTypeClass()) {
            throw new IllegalArgumentException("The given constructor " + constructor + " was not declared by class " + declaror);
        }
        // Get the substitutions for the declaring type
        final Substitutions substitutions = declaror.capture().getSubstitutions();
        // Get the type parameters of the constructor
        final java.lang.reflect.TypeVariable<? extends Constructor<?>>[] typeParameters = constructor.getTypeParameters();
        // Check the validity of the type arguments and get the combined substitutions from the declaror and type arguments
        if (typeParameters.length == 0) {
            // It's allowed to call a non-parametrized constructor with type arguments
            return new ConstructorCallable(constructor, declaror, new TypeVariable[0], substitutions);
        }
        // Check the type arguments
        final TypeArgumentChecker checker = new TypeArgumentChecker(typeArguments, substitutions, typeParameters);
        if (!checker.check()) {
            return null;
        }
        // Create an return the constructor callable
        return new ConstructorCallable(constructor, declaror, checker.getTypeParameters(), checker.getSubstitutions());
    }
}
