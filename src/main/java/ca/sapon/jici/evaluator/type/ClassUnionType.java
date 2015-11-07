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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import ca.sapon.jici.evaluator.Accessible;
import ca.sapon.jici.evaluator.Callable;
import ca.sapon.jici.evaluator.value.ValueKind;
import ca.sapon.jici.util.ReflectionUtil;
import ca.sapon.jici.util.StringUtil;
import ca.sapon.jici.util.TypeUtil;

/**
 *
 */
public class ClassUnionType implements ClassType {
    private final Set<Class<?>> classes;
    private final Set<SingleClassType> lowestUpperBound;

    private ClassUnionType(Collection<ClassType> union) {
        if (union.size() <= 1) {
            throw new UnsupportedOperationException("Expected more than one type");
        }
        classes = new HashSet<>(union.size());
        boolean allEqual = true;
        Class<?> previous = null;
        for (ClassType type : union) {
            if (type instanceof ClassUnionType) {
                classes.addAll(((ClassUnionType) type).getTypeClasses());
                allEqual = false;
            } else {
                final Class<?> _class = ((SingleClassType) type).getTypeClass();
                classes.add(_class);
                if (previous == null) {
                    previous = _class;
                } else {
                    allEqual &= previous == _class;
                }
            }
        }
        if (allEqual) {
            throw new UnsupportedOperationException("Expected differing types in the union");
        }
        final Set<Class<?>> bounds = ReflectionUtil.getLowestUpperBound(classes);
        lowestUpperBound = new HashSet<>(bounds.size());
        for (Class<?> bound : bounds) {
            lowestUpperBound.add(SingleClassTypeLiteral.of(bound));
        }
    }

    public Set<Class<?>> getTypeClasses() {
        return classes;
    }

    public Set<SingleClassType> getLowestUpperBound() {
        return lowestUpperBound;
    }

    @Override
    public String getName() {
        return '(' + StringUtil.toString(lowestUpperBound, " | ") + ')';
    }

    @Override
    public ValueKind getKind() {
        return ValueKind.OBJECT;
    }

    @Override
    public boolean isVoid() {
        return false;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public boolean isNumeric() {
        return false;
    }

    @Override
    public boolean isIntegral() {
        return false;
    }

    @Override
    public boolean isBoolean() {
        return false;
    }

    @Override
    public boolean isArray() {
        for (SingleClassType bound : lowestUpperBound) {
            if (bound.isArray()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isObject() {
        return true;
    }

    @Override
    public boolean convertibleTo(Type to) {
        return TypeUtil.convertibleTo(this, to);
    }

    @Override
    public Callable getConstructor(Type[] arguments) {
        for (SingleClassType bound : lowestUpperBound) {
            try {
                return bound.getConstructor(arguments);
            } catch (UnsupportedOperationException ignored) {
            }
        }
        throw new UnsupportedOperationException("No constructor for signature: "
                + "(" + StringUtil.toString(Arrays.asList(arguments), ", ") + ") in " + getName());
    }

    @Override
    public Accessible getField(String name) {
        for (SingleClassType bound : lowestUpperBound) {
            try {
                return bound.getField(name);
            } catch (UnsupportedOperationException ignored) {
            }
        }
        throw new UnsupportedOperationException("No field named " + name + " in " + getName());
    }

    @Override
    public Callable getMethod(String name, Type[] arguments) {
        for (SingleClassType bound : lowestUpperBound) {
            try {
                return bound.getMethod(name, arguments);
            } catch (UnsupportedOperationException ignored) {
            }
        }
        throw new UnsupportedOperationException("No method for signature: "
                + name + "(" + StringUtil.toString(Arrays.asList(arguments), ", ") + ") in " + getName());
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof ClassUnionType && this.lowestUpperBound.equals(((ClassUnionType) other).lowestUpperBound);
    }

    @Override
    public int hashCode() {
        return lowestUpperBound.hashCode();
    }

    public static ClassUnionType of(ClassType... union) {
        return of(Arrays.asList(union));
    }

    public static ClassUnionType of(Collection<ClassType> union) {
        return new ClassUnionType(union);
    }
}
