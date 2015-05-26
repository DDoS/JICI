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
package ca.sapon.jici.evaluator.value.type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import ca.sapon.jici.util.ReflectionUtil;
import ca.sapon.jici.util.StringUtil;

/**
 *
 */
public class ObjectUnionType extends ObjectType {
    private final ObjectType[] types;
    private final Set<ObjectType> lowestUpperBound;

    public ObjectUnionType(ObjectType... types) {
        super(Object.class);
        if (types.length <= 1) {
            throw new IllegalArgumentException("Expected more than one type");
        }
        final Set<Class<?>> classes = new HashSet<>(types.length);
        boolean allEqual = true;
        Class<?> previous = null;
        for (ObjectType type : types) {
            if (type instanceof ObjectUnionType) {
                classes.addAll(((ObjectUnionType) type).getTypeClasses());
                allEqual = false;
            } else {
                final Class<?> _class = type.getTypeClass();
                classes.add(_class);
                if (previous == null) {
                    previous = _class;
                } else {
                    allEqual &= previous == _class;
                }
            }
        }
        if (allEqual) {
            throw new IllegalArgumentException("Expected differing types in the union");
        }
        this.types = types;
        final Set<Class<?>> bounds = ReflectionUtil.getLowestUpperBound(classes);
        lowestUpperBound = new HashSet<>(bounds.size());
        for (Class<?> bound : bounds) {
            final ObjectType type = new ObjectType(bound);
            lowestUpperBound.add(type);
        }
    }

    public Set<Class<?>> getTypeClasses() {
        final Set<Class<?>> classes = new HashSet<>(types.length);
        for (ObjectType type : types) {
            if (type instanceof ObjectUnionType) {
                classes.addAll(((ObjectUnionType) type).getTypeClasses());
            } else {
                classes.add(type.getTypeClass());
            }
        }
        return classes;
    }

    @Override
    public Class<?> getTypeClass() {
        throw new IllegalArgumentException("No type class for object type union");
    }

    @Override
    public String getName() {
        return StringUtil.toString(lowestUpperBound, ", ");
    }

    @Override
    public boolean is(Type type) {
        return type instanceof ObjectUnionType && this.lowestUpperBound.equals(((ObjectUnionType) type).lowestUpperBound);
    }

    @Override
    public boolean isArray() {
        for (ObjectType bound : lowestUpperBound) {
            if (bound.isArray()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Type unbox() {
        return this;
    }

    @Override
    public boolean convertibleTo(Type to) {
        for (ObjectType bound : lowestUpperBound) {
            if (bound.convertibleTo(to)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Constructor<?> getConstructor(Type[] arguments) {
        for (ObjectType bound : lowestUpperBound) {
            try {
                return bound.getConstructor(arguments);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return failGetConstructor(arguments);
    }

    @Override
    public Constructor<?> getVarargConstructor(Type[] arguments) {
        for (ObjectType bound : lowestUpperBound) {
            try {
                return bound.getVarargConstructor(arguments);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return failGetConstructor(arguments);
    }

    private Constructor<?> failGetConstructor(Type[] arguments) {
        throw new IllegalArgumentException("No constructor for signature: "
                + "(" + StringUtil.toString(Arrays.asList(arguments), ", ") + ") in " + getName());
    }

    @Override
    public Field getField(String name) {
        for (ObjectType bound : lowestUpperBound) {
            try {
                return bound.getField(name);
            } catch (IllegalArgumentException ignored) {
            }
        }
        throw new IllegalArgumentException("No field named " + name + " in " + getName());
    }

    @Override
    public Method getMethod(String name, Type[] arguments) {
        for (ObjectType bound : lowestUpperBound) {
            try {
                return bound.getMethod(name, arguments);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return failGetMethod(name, arguments);
    }

    @Override
    public Method getVarargMethod(String name, Type[] arguments) {
        for (ObjectType bound : lowestUpperBound) {
            try {
                return bound.getVarargMethod(name, arguments);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return failGetMethod(name, arguments);
    }

    private Method failGetMethod(String name, Type[] arguments) {
        throw new IllegalArgumentException("No method for signature: "
                + name + "(" + StringUtil.toString(Arrays.asList(arguments), ", ") + ") in " + getName());
    }
}
