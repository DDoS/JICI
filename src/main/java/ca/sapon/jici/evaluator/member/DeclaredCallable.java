package ca.sapon.jici.evaluator.member;

import java.util.HashMap;
import java.util.Map;

import ca.sapon.jici.evaluator.Substitutions;
import ca.sapon.jici.evaluator.type.ComponentType;
import ca.sapon.jici.evaluator.type.LiteralReferenceType;
import ca.sapon.jici.evaluator.type.ReferenceType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.type.TypeArgument;
import ca.sapon.jici.evaluator.type.TypeVariable;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.util.ReflectionUtil;
import ca.sapon.jici.util.TypeUtil;

/**
 * A common class for declared callables, such as methods and constructors. This excludes built-in like the clone() method for arrays.
 */
public abstract class DeclaredCallable implements Callable {
    private final LiteralReferenceType declaror;
    private final TypeVariable[] typeParameters;
    private final Type[] parameterTypes;
    private final Type returnType;
    private boolean varargEnabled;

    protected DeclaredCallable(LiteralReferenceType declaror, TypeVariable[] typeParameters, Type returnType,
                               java.lang.reflect.Type[] genericParameterTypes, Substitutions substitutions) {
        this(declaror, typeParameters, getParameterTypes(genericParameterTypes, substitutions), returnType, false);
    }

    protected DeclaredCallable(LiteralReferenceType declaror, TypeVariable[] typeParameters, Type[] parameterTypes, Type returnType, boolean varargEnabled) {
        this.declaror = declaror;
        this.typeParameters = typeParameters;
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
        this.varargEnabled = varargEnabled;
    }

    @Override
    public LiteralReferenceType getDeclaringType() {
        return declaror;
    }

    @Override
    public TypeVariable[] getTypeParameters() {
        return typeParameters;
    }

    @Override
    public Type[] getParameterTypes() {
        return parameterTypes;
    }

    @Override
    public Type getReturnType() {
        return returnType;
    }

    @Override
    public boolean isVarargEnabled() {
        return varargEnabled;
    }

    @Override
    public abstract DeclaredCallable useVararg();

    @Override
    public boolean isApplicable(Type[] argumentTypes) {
        final Type[] expandedParameter;
        if (varargEnabled) {
            // When we use vararg, we can expand the last type parameter to have the required count
            if (argumentTypes.length < parameterTypes.length - 1) {
                return false;
            }
            expandedParameter = ReflectionUtil.expandsVarargs(parameterTypes, argumentTypes.length);
        } else {
            if (argumentTypes.length != parameterTypes.length) {
                return false;
            }
            expandedParameter = parameterTypes;
        }
        // Argument types must be convertible to the parameter types
        for (int i = 0; i < expandedParameter.length; i++) {
            if (!argumentTypes[i].convertibleTo(expandedParameter[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isMoreApplicableThan(Callable other, Type[] argumentTypes) {
        final Type[] otherParameterTypes = other.getParameterTypes();
        // When we use vararg, we compare extra arguments with the last one
        if (!varargEnabled && this.parameterTypes.length != otherParameterTypes.length) {
            return true;
        }
        // Parameter types must be convertible to the other parameter types
        final int largestLength = Math.max(this.parameterTypes.length, otherParameterTypes.length);
        for (int i = 0; i < largestLength; i++) {
            if (!isNarrowerParameter(
                    this.parameterTypes[Math.min(i, this.parameterTypes.length - 1)],
                    otherParameterTypes[Math.min(i, otherParameterTypes.length - 1)],
                    argumentTypes.length > 0 && argumentTypes[Math.min(i, argumentTypes.length - 1)].isPrimitive()
            )) {
                return false;
            }
        }
        return true;
    }

    protected Object[] unwrapArguments(Value[] arguments) {
        final int argumentCount = arguments.length;
        Object[] values = new Object[argumentCount];
        for (int i = 0; i < argumentCount; i++) {
            values[i] = arguments[i].asObject();
        }
        final int parameterCount = parameterTypes.length;
        if (varargEnabled) {
            final int varargIndex = parameterCount - 1;
            if (argumentCount < varargIndex) {
                throw new UnsupportedOperationException("Not enough argument values, expected at least " + varargIndex + " but got " + argumentCount);
            }
            final ComponentType varargType = ((ReferenceType) parameterTypes[varargIndex]).getComponentType();
            values = ReflectionUtil.compactVarargs(varargType, varargIndex, values);
        } else {
            if (argumentCount != parameterCount) {
                throw new UnsupportedOperationException("Wrong argument value count, expected " + parameterCount + " but got " + argumentCount);
            }
        }
        return values;
    }

    private static boolean isNarrowerParameter(Type parameterA, Type parameterB, boolean primitiveArgument) {
        // if A is primitive
        //   if B is primitive
        //     A < B
        //   else
        //     argument is primitive
        // else
        //   if B is primitive
        //     argument is not primitive
        //   else
        //     A < B
        if (parameterA.isPrimitive()) {
            return parameterB.isPrimitive() ? parameterA.convertibleTo(parameterB) : primitiveArgument;
        }
        return parameterB.isPrimitive() ? !primitiveArgument : parameterA.convertibleTo(parameterB);
    }

    private static Type[] getParameterTypes(java.lang.reflect.Type[] genericParameterTypes, Substitutions substitutions) {
        // Get the parameter types by applying declaror and type argument substitutions
        final Type[] parameterTypes = TypeUtil.wrap(genericParameterTypes);
        for (int i = 0; i < parameterTypes.length; i++) {
            final Type parameterType = parameterTypes[i];
            if (parameterType instanceof TypeArgument) {
                parameterTypes[i] = ((TypeArgument) parameterType).substituteTypeVariables(substitutions);
            }
        }
        return parameterTypes;
    }

    public static class TypeArgumentChecker {
        private TypeArgument[] typeArguments;
        private Substitutions substitutions;
        private java.lang.reflect.TypeVariable<?>[] typeParameters;
        private TypeVariable[] signatureParameters;
        private Substitutions combinedSubstitutions;
        private boolean valid;
        private boolean checked = false;

        public TypeArgumentChecker(TypeArgument[] typeArguments, Substitutions substitutions, java.lang.reflect.TypeVariable<?>[] typeParameters) {
            this.typeArguments = typeArguments;
            this.substitutions = substitutions;
            this.typeParameters = typeParameters;
        }

        public TypeVariable[] getTypeParameters() {
            return signatureParameters;
        }

        public Substitutions getSubstitutions() {
            return combinedSubstitutions;
        }

        public boolean check() {
            if (checked) {
                return valid;
            }
            checked = true;
            // Check for length match
            if (typeParameters.length != typeArguments.length) {
                return valid = false;
            }
            // Wrap the parameters
            signatureParameters = new TypeVariable[typeParameters.length];
            for (int i = 0; i < typeParameters.length; i++) {
                signatureParameters[i] = (TypeVariable) TypeUtil.wrap(typeParameters[i]);
            }
            // Generate the combined substitution of the declaring class and method parameters, following the shadowing rules
            final Map<String, TypeArgument> namesToArguments = new HashMap<>();
            // First add all declaring class arguments from the original substitutions
            namesToArguments.putAll(substitutions.getMap());
            // Now add the method ones, replacing the declaring class arguments (shadowing)
            namesToArguments.putAll(Substitutions.toSubstitutionMap(signatureParameters, typeArguments));
            combinedSubstitutions = new Substitutions(namesToArguments);
            // Apply the substitutions to the method parameter bounds to get the final method signature parameters
            for (int i = 0; i < signatureParameters.length; i++) {
                signatureParameters[i] = signatureParameters[i].substituteBoundTypeVariables(combinedSubstitutions);
            }
            // Check if arguments are within bounds
            for (int i = 0; i < signatureParameters.length; i++) {
                if (!signatureParameters[i].boundsContain(typeArguments[i])) {
                    return valid = false;
                }
            }
            return valid = true;
        }
    }
}
