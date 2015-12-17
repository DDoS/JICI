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
package ca.sapon.jici.evaluator;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import ca.sapon.jici.evaluator.type.LiteralType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.value.ObjectValue;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.evaluator.value.VoidValue;
import ca.sapon.jici.util.ReflectionUtil;
import ca.sapon.jici.util.TypeUtil;

/**
 *
 */
public abstract class Callable {
    protected final Type returnType;

    protected Callable(Type returnType) {
        this.returnType = returnType;
    }

    public Type getReturnType() {
        return returnType;
    }

    public abstract Value call(Value target, Value... arguments);

    public static Callable forMethod(Method method) {
        return new MethodCallable(method, false);
    }

    public static Callable forVarargMethod(Method method) {
        return new MethodCallable(method, true);
    }

    public static Callable forArrayClone(LiteralType type) {
        return new ArrayCloneCallable(type);
    }

    public static Callable forConstructor(Constructor<?> constructor) {
        return new ConstructorCallable(constructor, false);
    }

    public static Callable forVarargConstructor(Constructor<?> constructor) {
        return new ConstructorCallable(constructor, true);
    }

    private static class MethodCallable extends Callable {
        private final Method method;
        private final Class<?> varargType;
        private final int varargIndex;

        private MethodCallable(Method method, boolean vararg) {
            super(TypeUtil.wrap(method.getGenericReturnType()));
            this.method = method;
            if (vararg) {
                final Class<?>[] parameters = method.getParameterTypes();
                varargIndex = parameters.length - 1;
                varargType = parameters[varargIndex].getComponentType();
            } else {
                varargIndex = -1;
                varargType = null;
            }
        }

        @Override
        public Value call(Value target, Value... arguments) {
            final Object value = target.asObject();
            final int length = arguments.length;
            Object[] values = new Object[length];
            for (int i = 0; i < length; i++) {
                values[i] = arguments[i].asObject();
            }
            if (varargIndex >= 0) {
                values = ReflectionUtil.compactVarargs(varargType, varargIndex, values);
            }
            try {
                if (returnType.isVoid()) {
                    method.invoke(value, values);
                    return VoidValue.THE_VOID;
                }
                return returnType.getKind().wrap(method.invoke(value, values));
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }
    }

    private static class ConstructorCallable extends Callable {
        private final Constructor<?> constructor;
        private final Class<?> varargType;
        private final int varargIndex;

        private ConstructorCallable(Constructor<?> constructor, boolean vararg) {
            super(TypeUtil.wrap(constructor.getDeclaringClass()));
            this.constructor = constructor;
            if (vararg) {
                final Class<?>[] parameters = constructor.getParameterTypes();
                varargIndex = parameters.length - 1;
                varargType = parameters[varargIndex].getComponentType();
            } else {
                varargIndex = -1;
                varargType = null;
            }
        }

        @Override
        public Value call(Value target, Value... arguments) {
            final int length = arguments.length;
            Object[] values = new Object[length];
            for (int i = 0; i < length; i++) {
                values[i] = arguments[i].asObject();
            }
            if (varargIndex >= 0) {
                values = ReflectionUtil.compactVarargs(varargType, varargIndex, values);
            }
            try {
                return returnType.getKind().wrap(constructor.newInstance(values));
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }
    }

    private static class ArrayCloneCallable extends Callable {
        private ArrayCloneCallable(LiteralType type) {
            super(type);
        }

        @Override
        public Value call(Value target, Value... arguments) {
            try {
                return ObjectValue.of(ReflectionUtil.cloneArray(target.asObject()));
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }
    }
}
