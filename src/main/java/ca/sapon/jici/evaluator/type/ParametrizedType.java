/*
 * This file is part of JICI, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015-2015 Aleksi Sapon <http://sapon.ca/jici/>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package ca.sapon.jici.evaluator.type;

import java.util.List;

import ca.sapon.jici.util.StringUtil;

/**
 * A type that takes type arguments, such as {@code Set<T>} or {@code Map<String, Integer>}.
 */
public class ParametrizedType extends LiteralReferenceType {
    private final LiteralReferenceType erased;
    private final List<TypeArgument> arguments;
    // Cache these to prevent a new array from being created every time
    private java.lang.reflect.TypeVariable<?>[] parameters = null;

    private ParametrizedType(LiteralReferenceType erased, List<TypeArgument> arguments) {
        super(erased.getTypeClass());
        if (arguments.size() <= 0) {
            throw new IllegalArgumentException("Expected at least one type argument");
        }
        this.erased = erased;
        this.arguments = arguments;
    }

    public List<TypeArgument> getArguments() {
        return arguments;
    }

    @Override
    public String getName() {
        Class<?> _class = erased.getTypeClass();
        int dimensions = 0;
        while (_class.isArray()) {
            _class = _class.getComponentType();
            dimensions++;
        }
        return _class.getCanonicalName() + '<' + StringUtil.toString(arguments, ", ") + '>' + StringUtil.repeat("[]", dimensions);
    }

    @Override
    public boolean isReifiable() {
        for (TypeArgument argument : arguments) {
            if (!(argument instanceof WildcardType) || !((WildcardType) argument).isUnbounded()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isRaw() {
        return false;
    }

    @Override
    public LiteralReferenceType getErasure() {
        return erased;
    }

    @Override
    public ParametrizedType getComponentType() {
        final ComponentType componentType = erased.getComponentType();
        if (!(componentType instanceof LiteralReferenceType)) {
            throw new UnsupportedOperationException("Component type is not a literal reference type");
        }
        return new ParametrizedType((LiteralReferenceType) componentType, arguments);
    }

    @Override
    public ParametrizedType asArray(int dimensions) {
        return new ParametrizedType(erased.asArray(dimensions), arguments);
    }

    @Override
    public boolean contains(TypeArgument other) {
        return equals(other);
    }

    public boolean argumentsContain(List<TypeArgument> otherArguments) {
        if (arguments.size() != otherArguments.size()) {
            return false;
        }
        for (int i = 0; i < arguments.size(); i++) {
            if (!arguments.get(i).contains(otherArguments.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public LiteralReferenceType getDirectSuperClass() {
        final LiteralReferenceType superType = erased.getDirectSuperClass();
        if (superType instanceof ParametrizedType) {
            return substituteTypeVariables((ParametrizedType) superType);
        }
        return superType;
    }

    @Override
    public LiteralReferenceType[] getDirectlyImplementedInterfaces() {
        final LiteralReferenceType[] interfaces = erased.getDirectlyImplementedInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            final LiteralReferenceType _interface = interfaces[i];
            if (_interface instanceof ParametrizedType) {
                interfaces[i] = substituteTypeVariables((ParametrizedType) _interface);
            }
        }
        return interfaces;
    }

    private LiteralReferenceType substituteTypeVariables(ParametrizedType parametrizedType) {
        // Substitute type variables in the super type by the proper arguments
        for (int i = 0; i < parametrizedType.arguments.size(); i++) {
            final TypeArgument argument = parametrizedType.arguments.get(i);
            if (argument instanceof TypeVariable) {
                // Find index of type variable by name in the parameters, use it to substitute for the argument
                parametrizedType.arguments.set(i, arguments.get(indexOf((TypeVariable) argument)));
            } else if (argument instanceof ParametrizedType) {
                // Apply recursively since a type variable can be nested into an argument's arguments
                substituteTypeVariables((ParametrizedType) argument);
            }
        }
        return parametrizedType;
    }

    private int indexOf(TypeVariable variable) {
        final java.lang.reflect.TypeVariable<?>[] parameters = getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getName().equals(variable.getName())) {
                return i;
            }
        }
        throw new IllegalArgumentException("Couldn't find " + variable + " in parameters");
    }

    private java.lang.reflect.TypeVariable<?>[] getParameters() {
        if (parameters == null) {
            parameters = erased.getTypeClass().getTypeParameters();
        }
        return parameters;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof ParametrizedType) {
            final ParametrizedType that = (ParametrizedType) other;
            return erased.equals(that.erased) && arguments.equals(that.arguments);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + arguments.hashCode();
    }

    public static ParametrizedType of(Class<?> raw, List<TypeArgument> arguments) {
        return of(LiteralReferenceType.of(raw), arguments);
    }

    public static ParametrizedType of(LiteralReferenceType raw, List<TypeArgument> arguments) {
        return new ParametrizedType(raw, arguments);
    }
}
