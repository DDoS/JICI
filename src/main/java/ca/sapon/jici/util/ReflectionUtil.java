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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import ca.sapon.jici.evaluator.type.ClassType;
import ca.sapon.jici.evaluator.type.NullType;
import ca.sapon.jici.evaluator.type.PrimitiveType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.type.VoidType;

/**
 *
 */
public final class ReflectionUtil {
    public static final Set<String> JAVA_LANG_CLASSES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                    "Appendable", "AutoCloseable", "CharSequence", "Cloneable", "Comparable", "Iterable", "Readable",
                    "Runnable", "Boolean", "Byte", "Character", "Class", "ClassLoader", "ClassValue", "Compiler", "Double",
                    "Enum", "Float", "InheritableThreadLocal", "Integer", "Long", "Math", "Number", "Object", "Package",
                    "Process", "ProcessBuilder", "Runtime", "RuntimePermission", "SecurityManager", "Short", "StackTraceElement",
                    "StrictMath", "String", "StringBuffer", "StringBuilder", "System", "Thread", "ThreadGroup", "ThreadLocal",
                    "Throwable", "Void", "ArithmeticException", "ArrayIndexOutOfBoundsException", "ArrayStoreException",
                    "ClassCastException", "ClassNotFoundException", "CloneNotSupportedException", "EnumConstantNotPresentException",
                    "Exception", "IllegalAccessException", "IllegalArgumentException", "IllegalMonitorStateException",
                    "IllegalStateException", "IllegalThreadStateException", "IndexOutOfBoundsException", "InstantiationException",
                    "InterruptedException", "NegativeArraySizeException", "NoSuchFieldException", "NoSuchMethodException",
                    "NullPointerException", "NumberFormatException", "ReflectiveOperationException", "RuntimeException",
                    "SecurityException", "StringIndexOutOfBoundsException", "TypeNotPresentException", "UnsupportedOperationException",
                    "AbstractMethodError", "AssertionError", "BootstrapMethodError", "ClassCircularityError", "ClassFormatError",
                    "Error", "ExceptionInInitializerError", "IllegalAccessError", "IncompatibleClassChangeError", "InstantiationError",
                    "InternalError", "LinkageError", "NoClassDefFoundError", "NoSuchFieldError", "NoSuchMethodError", "OutOfMemoryError",
                    "StackOverflowError", "ThreadDeath", "UnknownError", "UnsatisfiedLinkError", "UnsupportedClassVersionError",
                    "VerifyError", "VirtualMachineError", "Deprecated", "Override", "SafeVarargs", "SuppressWarnings"
            ))
    );
    private static final Map<Class<?>, Character> ARRAY_NAME_PRIMITIVE_ENCODING = new HashMap<>();

    static {
        ARRAY_NAME_PRIMITIVE_ENCODING.put(boolean.class, 'Z');
        ARRAY_NAME_PRIMITIVE_ENCODING.put(byte.class, 'B');
        ARRAY_NAME_PRIMITIVE_ENCODING.put(short.class, 'S');
        ARRAY_NAME_PRIMITIVE_ENCODING.put(char.class, 'C');
        ARRAY_NAME_PRIMITIVE_ENCODING.put(int.class, 'I');
        ARRAY_NAME_PRIMITIVE_ENCODING.put(long.class, 'J');
        ARRAY_NAME_PRIMITIVE_ENCODING.put(float.class, 'F');
        ARRAY_NAME_PRIMITIVE_ENCODING.put(double.class, 'D');
    }

    private ReflectionUtil() {
    }

    public static Class<?> findClass(List<?> name) {
        return decodeClassName(name, false);
    }

    public static Class<?> disambiguateClass(List<?> name) {
        return decodeClassName(name, true);
    }

    private static Class<?> decodeClassName(List<?> name, boolean trim) {
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
            final Class<?> nested = lookupNestedClass(_class, name.get(i).toString());
            if (nested == null) {
                break;
            }
            _class = nested;
            i++;
        }
        if (trim) {
            name.subList(0, i).clear();
        } else if (i < size) {
            _class = null;
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

    public static Class<?> asArrayType(Class<?> componentType, int dimensions) {
        if (dimensions == 0) {
            return componentType;
        }
        final Character character = ARRAY_NAME_PRIMITIVE_ENCODING.get(componentType);
        final String encodedName = character != null ? character.toString() : 'L' + componentType.getCanonicalName() + ';';
        return lookupClass(StringUtil.repeat("[", dimensions) + encodedName);
    }

    public static <C> C resolveOverloads(Map<C, Class<?>[]> candidates, Type[] arguments) {
        // fast-track the lack of candidates
        if (candidates.isEmpty()) {
            return null;
        }
        // particular case for varargs where the expansion can lead to many candidates with no arguments. This case cannot be resolved
        if (arguments.length == 0 && candidates.size() > 1) {
            return null;
        }
        // remove methods with un-applicable parameters
        candidates:
        for (Iterator<Entry<C, Class<?>[]>> iterator = candidates.entrySet().iterator(); iterator.hasNext(); ) {
            final Entry<C, Class<?>[]> entry = iterator.next();
            final Class<?>[] parameters = entry.getValue();
            boolean allEqual = true;
            for (int i = 0; i < parameters.length; i++) {
                final Type argument = arguments[i];
                final Type parameter = wrap(parameters[i]);
                if (!argument.convertibleTo(parameter)) {
                    iterator.remove();
                    continue candidates;
                }
                // look for a perfect match
                allEqual &= argument.is(parameter);
            }
            // fast track perfect matches
            if (allEqual) {
                return entry.getKey();
            }
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
                    if (isNarrowerParameter(challenges[i], parameters[i], arguments[i].isPrimitive())) {
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

    private static boolean isNarrowerParameter(Class<?> parameterA, Class<?> parameterB, boolean primitiveArgument) {
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
            return parameterB.isPrimitive() ? PrimitiveType.convertibleTo(parameterA, parameterB) : primitiveArgument;
        }
        return parameterB.isPrimitive() ? !primitiveArgument : ClassType.convertibleTo(parameterA, parameterB);
    }

    public static Type wrap(Class<?> type) {
        if (type == null) {
            return NullType.THE_NULL;
        }
        if (type == void.class) {
            return VoidType.THE_VOID;
        }
        if (type.isPrimitive()) {
            return PrimitiveType.of(type);
        }
        return ClassType.of(type);
    }

    // based on https://stackoverflow.com/questions/9797212/finding-the-nearest-common-superclass-or-superinterface-of-a-collection-of-cla
    public static Set<Class<?>> getLowestUpperBound(Iterable<Class<?>> classes) {
        final Set<Class<?>> common = getCommonSuperClasses(classes);
        final Set<Class<?>> lowest = new HashSet<>(common.size());
        while (!common.isEmpty()) {
            final Iterator<Class<?>> iterator = common.iterator();
            Class<?> _class = iterator.next();
            iterator.remove();
            while (iterator.hasNext()) {
                Class<?> candidate = iterator.next();
                if (candidate.isAssignableFrom(_class)) {
                    iterator.remove();
                } else if (_class.isAssignableFrom(candidate)) {
                    _class = candidate;
                    iterator.remove();
                }
            }
            lowest.add(_class);
        }
        return lowest;
    }

    public static Set<Class<?>> getCommonSuperClasses(Iterable<Class<?>> classes) {
        final Iterator<Class<?>> iterator = classes.iterator();
        if (!iterator.hasNext()) {
            return Collections.emptySet();
        }
        final Set<Class<?>> superClasses = getSuperClasses(iterator.next());
        while (iterator.hasNext()) {
            final Class<?> _class = iterator.next();
            final Iterator<Class<?>> candidates = superClasses.iterator();
            while (candidates.hasNext()) {
                final Class<?> superClass = candidates.next();
                if (!superClass.isAssignableFrom(_class)) {
                    candidates.remove();
                }
            }
        }
        return superClasses;
    }

    public static Set<Class<?>> getSuperClasses(Class<?> _class) {
        final Set<Class<?>> result = new HashSet<>();
        final Queue<Class<?>> queue = new ArrayDeque<>();
        queue.add(_class);
        if (_class.isInterface()) {
            queue.add(Object.class);
        }
        while (!queue.isEmpty()) {
            final Class<?> child = queue.remove();
            if (result.add(child)) {
                if (child.isArray()) {
                    addArraySuperClasses(child, queue);
                } else {
                    final Class<?> superClass = child.getSuperclass();
                    if (superClass != null) {
                        queue.add(superClass);
                    }
                    queue.addAll(Arrays.asList(child.getInterfaces()));
                }
            }
        }
        return result;
    }

    private static void addArraySuperClasses(Class<?> arrayType, Collection<Class<?>> to) {
        int dimensions = 0;
        Class<?> componentType = arrayType;
        do {
            componentType = componentType.getComponentType();
            to.add(asArrayType(Object.class, dimensions));
            to.add(asArrayType(Cloneable.class, dimensions));
            to.add(asArrayType(Serializable.class, dimensions));
            dimensions++;
        } while (componentType.isArray());
        if (!componentType.isPrimitive()) {
            final Class<?> superClass = componentType.getSuperclass();
            if (superClass != null) {
                to.add(asArrayType(superClass, dimensions));
            }
            for (Class<?> _interface : componentType.getInterfaces()) {
                to.add(asArrayType(_interface, dimensions));
            }
        }
    }

    public static Class<?>[] expandsVarargs(Class<?>[] parameters, int count) {
        final Class<?>[] expanded = new Class<?>[count];
        final int lastIndex = parameters.length - 1;
        System.arraycopy(parameters, 0, expanded, 0, lastIndex);
        final Class<?> varargType = parameters[lastIndex].getComponentType();
        for (int i = lastIndex; i < count; i++) {
            expanded[i] = varargType;
        }
        return expanded;
    }

    public static Object[] compactVarargs(Class<?> varargType, int varargIndex, Object[] arguments) {
        final Object[] compacted = new Object[varargIndex + 1];
        System.arraycopy(arguments, 0, compacted, 0, varargIndex);
        final int varargCount = arguments.length - varargIndex;
        final Object array = Array.newInstance(varargType, varargCount);
        for (int i = 0; i < varargCount; i++) {
            Array.set(array, i, arguments[i + varargIndex]);
        }
        compacted[varargIndex] = array;
        return compacted;
    }

    public static Set<Class<?>> findClasses(List<?> packageName) {
        final String packageString = StringUtil.toString(packageName, ".");
        final ClassLoader[] classLoaders = {ClassLoader.getSystemClassLoader(), Thread.currentThread().getContextClassLoader()};
        final Set<File> directories = new HashSet<>();
        for (ClassLoader classLoader : classLoaders) {
            if (classLoader == null) {
                continue;
            }
            try {
                final Enumeration<URL> resources = classLoader.getResources(StringUtil.toString(packageName, "/"));
                while (resources.hasMoreElements()) {
                    final File file = new File(URLDecoder.decode(resources.nextElement().getPath(), "UTF-8"));
                    directories.add(file);
                    findDirectories(directories, file);
                }
            } catch (Exception ignored) {
            }
        }
        final Set<Class<?>> classes = new HashSet<>();
        for (File directory : directories) {
            if (!directory.exists()) {
                continue;
            }
            for (String file : directory.list()) {
                if (!file.endsWith(".class")) {
                    continue;
                }
                final String name;
                try {
                    String path = directory.getCanonicalPath().replaceAll("/", ".").replaceAll("\\\\", ".");
                    path = path.substring(path.indexOf(packageString));
                    name = path + '.' + file.substring(0, file.length() - 6);
                } catch (IOException exception) {
                    return Collections.emptySet();
                }
                if (name.indexOf('$') != -1) {
                    continue;
                }
                try {
                    classes.add(Class.forName(name));
                } catch (ClassNotFoundException ignored) {
                }
            }
        }
        return classes;
    }

    private static void findDirectories(Set<File> directories, File directory) {
        final File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    directories.add(file);
                    findDirectories(directories, file);
                }
            }
        }
    }

    @SuppressWarnings("SuspiciousSystemArraycopy")
    public static Object cloneArray(Object array) {
        final int length = Array.getLength(array);
        final Object clone = Array.newInstance(array.getClass().getComponentType(), length);
        System.arraycopy(array, 0, clone, 0, length);
        return clone;
    }
}
