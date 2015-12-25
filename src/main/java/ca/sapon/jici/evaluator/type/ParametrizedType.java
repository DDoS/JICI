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
public class ParametrizedType extends SingleReferenceType implements TypeArgument {
    private final LiteralReferenceType raw;
    private final List<TypeArgument> arguments;

    private ParametrizedType(LiteralReferenceType raw, List<TypeArgument> arguments) {
        this.raw = raw;
        this.arguments = arguments;
    }

    @Override
    public String getName() {
        Class<?> _class = raw.getTypeClass();
        int dimensions = 0;
        while (_class.isArray()) {
            _class = _class.getComponentType();
            dimensions++;
        }
        return _class.getCanonicalName() + '<' + StringUtil.toString(arguments, ", ") + '>' + StringUtil.repeat("[]", dimensions);
    }

    @Override
    public boolean isArray() {
        return raw.isArray();
    }

    @Override
    public Class<?> getTypeClass() {
        return raw.getTypeClass();
    }

    public LiteralReferenceType getRaw() {
        return raw;
    }

    public List<TypeArgument> getArguments() {
        return arguments;
    }

    @Override
    public ParametrizedType getComponentType() {
        final LiteralType componentType = raw.getComponentType();
        if (componentType == null) {
            throw new UnsupportedOperationException("Not an array type");
        }
        if (!(componentType instanceof LiteralReferenceType)) {
            throw new UnsupportedOperationException("Component type is not a literal reference type");
        }
        return new ParametrizedType((LiteralReferenceType) componentType, arguments);
    }

    @Override
    public SingleReferenceType asArray(int dimensions) {
        return new ParametrizedType(raw.asArray(dimensions), arguments);
    }

    @Override
    public boolean contains(TypeArgument other) {
        return equals(other);
    }

    @Override
    public boolean convertibleTo(Type to) {
        // Parametrized types are a special case of single class types
        // They are only convertible between each other if the raw types are
        // And the target parameter types contains the source ones
        if (to instanceof ParametrizedType) {
            final ParametrizedType target = (ParametrizedType) to;
            if (!getRaw().convertibleTo(target.getRaw())) {
                return false;
            }
            final List<TypeArgument> targetArguments = target.getArguments();
            if (arguments.size() != targetArguments.size()) {
                return false;
            }
            for (int i = 0; i < arguments.size(); i++) {
                if (!targetArguments.get(i).contains(arguments.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return super.convertibleTo(to);
    }

    @Override
    public SingleReferenceType getSuperType() {
        final SingleReferenceType superType = raw.getSuperType();
        if (superType instanceof ParametrizedType) {
            return substituteTypeVariables((ParametrizedType) superType);
        }
        return superType;
    }

    @Override
    public SingleReferenceType[] getInterfaces() {
        final SingleReferenceType[] interfaces = raw.getInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            final SingleReferenceType _interface = interfaces[i];
            if (_interface instanceof ParametrizedType) {
                interfaces[i] = substituteTypeVariables((ParametrizedType) _interface);
            }
        }
        return interfaces;
    }

    private SingleReferenceType substituteTypeVariables(ParametrizedType parametrizedType) {
        // Substitute type variables in the super type by the proper arguments
        final java.lang.reflect.TypeVariable<?>[] parameters = raw.getTypeClass().getTypeParameters();
        for (int i = 0; i < parametrizedType.arguments.size(); i++) {
            final TypeArgument argument = parametrizedType.arguments.get(i);
            if (argument instanceof TypeVariable) {
                // Find index of type variable by name in the parameters, use it to substitute for the argument
                parametrizedType.arguments.set(i, arguments.get(indexOf(parameters, (TypeVariable) argument)));
            }
        }
        return parametrizedType;
    }

    private int indexOf(java.lang.reflect.TypeVariable<?>[] parameters, TypeVariable variable) {
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getName().equals(variable.getName())) {
                return i;
            }
        }
        throw new IllegalArgumentException("Couldn't find " + variable + " in parameters");
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof ParametrizedType) {
            final ParametrizedType that = (ParametrizedType) other;
            return raw.equals(that.raw) && arguments.equals(that.arguments);
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
