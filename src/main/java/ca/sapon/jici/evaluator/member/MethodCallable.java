package ca.sapon.jici.evaluator.member;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import ca.sapon.jici.evaluator.Substitutions;
import ca.sapon.jici.evaluator.type.LiteralReferenceType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.type.TypeArgument;
import ca.sapon.jici.evaluator.type.TypeVariable;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.evaluator.value.VoidValue;
import ca.sapon.jici.util.TypeUtil;

/**
 *
 */
public class MethodCallable extends DeclaredCallable {
    private final Method method;

    private MethodCallable(Method method, LiteralReferenceType declaror, TypeVariable[] typeParameters, Substitutions substitutions) {
        super(declaror, typeParameters, getReturnType(method, substitutions), method.getGenericParameterTypes(), substitutions);
        this.method = method;
    }

    private MethodCallable(Method method, LiteralReferenceType declaror, TypeVariable[] typeParameters, Type[] parameterTypes, Type returnType, boolean varargEnabled) {
        super(declaror, typeParameters, parameterTypes, returnType, varargEnabled);
        this.method = method;
    }

    @Override
    public boolean isStatic() {
        return Modifier.isStatic(method.getModifiers());
    }

    @Override
    public boolean supportsVararg() {
        return method.isVarArgs();
    }

    @Override
    public MethodCallable useVararg() {
        if (!supportsVararg()) {
            throw new UnsupportedOperationException("This method does not support vararg");
        }
        return new MethodCallable(method, getDeclaringType(), getTypeParameters(), getParameterTypes(), getReturnType(), true);
    }

    @Override
    public Value call(Value target, Value... arguments) {
        final Object value = target.asObject();
        final Object[] values = unwrapArguments(arguments);
        try {
            final Type returnType = getReturnType();
            if (returnType.isVoid()) {
                method.invoke(value, values);
                return VoidValue.THE_VOID;
            }
            return returnType.getKind().wrap(method.invoke(value, values));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public String toString() {
        return method.toString();
    }

    private static Type getReturnType(Method method, Substitutions substitutions) {
        // Get the return type by applying declaror and type argument substitutions
        Type type = TypeUtil.wrap(method.getGenericReturnType());
        if (type instanceof TypeArgument) {
            type = ((TypeArgument) type).substituteTypeVariables(substitutions);
        }
        return type.capture();
    }

    public static MethodCallable of(LiteralReferenceType declaror, Method method, TypeArgument[] typeArguments) {
        // Make sure the method actually belongs to the declaror
        if (method.getDeclaringClass() != declaror.getTypeClass()) {
            throw new IllegalArgumentException("The given method " + method + " was not declared by class " + declaror);
        }
        // Get the substitutions for the declaring type
        final Substitutions substitutions = declaror.capture().getSubstitutions();
        // Get the type parameters of the method
        final java.lang.reflect.TypeVariable<Method>[] typeParameters = method.getTypeParameters();
        // Check the validity of the type arguments and get the combined substitutions from the declaror and type arguments
        if (typeParameters.length == 0) {
            // It's allowed to call a non-parametrized method with type arguments
            return new MethodCallable(method, declaror, new TypeVariable[0], substitutions);
        }
        // Check the type arguments
        final TypeArgumentChecker checker = new TypeArgumentChecker(typeArguments, substitutions, typeParameters);
        if (!checker.check()) {
            return null;
        }
        // Create an return the method callable
        return new MethodCallable(method, declaror, checker.getTypeParameters(), checker.getSubstitutions());
    }
}
