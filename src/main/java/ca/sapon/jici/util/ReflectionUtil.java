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
package ca.sapon.jici.util;

import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ca.sapon.jici.evaluator.value.type.ObjectValueType;
import ca.sapon.jici.evaluator.value.type.PrimitiveValueType;
import ca.sapon.jici.evaluator.value.type.ValueType;

/**
 *
 */
public class ReflectionUtil {
    public static Class<?> disambiguateClass(List<?> name) {
        final int size = name.size();
        String nameString = "";
        Class<?> _class = null;
        int i = 0;
        while (i < size) {
            nameString += name.get(i++).toString();
            if ((_class = lookupClass(nameString)) != null) {
                break;
            }
            nameString += ".";
        }
        while (i < size) {
            if ((_class = lookupNestedClass(_class, name.get(i++).toString())) != null) {
                break;
            }
        }
        return _class;
    }

    public static Class<?> lookupClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException exception) {
            return null;
        }
    }

    public static Class<?> lookupNestedClass(Class<?> enclosing, String name) {
        for (Class<?> nested : enclosing.getDeclaredClasses()) {
            if (nested.getSimpleName().equals(name)) {
                final int modifiers = nested.getModifiers();
                if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)) {
                    return nested;
                }
            }
        }
        return null;
    }

    public static <C> C resolveOverloads(Map<C, Class<?>[]> candidates, ValueType[] arguments) {
        // remove methods with un-applicable parameters and look for perfect matches
        candidates:
        for (Iterator<Entry<C, Class<?>[]>> iterator = candidates.entrySet().iterator(); iterator.hasNext(); ) {
            final Entry<C, Class<?>[]> entry = iterator.next();
            final Class<?>[] parameters = entry.getValue();
            for (int i = 0; i < parameters.length; i++) {
                final ValueType argument = arguments[i];
                final Class<?> parameter = parameters[i];
                if (!argument.convertibleTo(parameter)) {
                    iterator.remove();
                    continue candidates;
                }
                if (argument.getTypeClass() != parameter) {
                    continue candidates;
                }
            }
            return entry.getKey();
        }
        // remove methods with the corresponding wider types
        C callable = null;
        int candidateCount = candidates.size();
        candidates:
        for (final Entry<C, Class<?>[]> entry : candidates.entrySet()) {
            final Class<?>[] parameters = entry.getValue();
            for (Class<?>[] challenges : candidates.values()) {
                if (challenges == parameters) {
                    continue;
                }
                for (int i = 0; i < parameters.length; i++) {
                    // remove when the challenge is narrower than the parameter
                    if (ReflectionUtil.isNarrower(challenges[i], parameters[i])) {
                        candidateCount--;
                        continue candidates;
                    }
                }
            }
            // cache the candidate because getting a single element from a set is awkward
            callable = entry.getKey();
        }
        // we need exactly one match
        return candidateCount != 1 || callable == null ? null : callable;
    }

    public static boolean isNarrower(Class<?> a, Class<?> b) {
        if (a.isPrimitive()) {
            // a !=b and a <= b
            return a != b && PrimitiveValueType.convertibleTo(a, b);
        }
        // a < b and !a.boxOf(b)
        return !a.isAssignableFrom(b) && ObjectValueType.unbox(a) != b;
    }
}
