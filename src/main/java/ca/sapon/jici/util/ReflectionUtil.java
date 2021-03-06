/*
 * This file is part of JICI, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015-2016 Aleksi Sapon <http://sapon.ca/jici/>
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
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.sapon.jici.evaluator.type.ComponentType;
import ca.sapon.jici.evaluator.type.ReferenceType;
import ca.sapon.jici.evaluator.type.Type;

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
    )));
    static final Map<Class<?>, Character> ARRAY_NAME_PRIMITIVE_ENCODING = new HashMap<>();

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
            if (nested.getSimpleName().equals(name) && Modifier.isPublic(nested.getModifiers())) {
                return nested;
            }
        }
        return null;
    }

    public static Class<?> asArrayType(Class<?> componentType, int dimensions) {
        if (dimensions < 0) {
            throw new IllegalArgumentException("Cannot have negative dimensions");
        }
        if (dimensions == 0) {
            return componentType;
        }
        final StringBuilder encodedName = new StringBuilder();
        StringUtil.repeat("[", dimensions, encodedName);
        final Character character = ARRAY_NAME_PRIMITIVE_ENCODING.get(componentType);
        if (character == null) {
            encodedName.append('L').append(componentType.getName()).append(';');
        } else {
            encodedName.append(character);
        }
        final Class<?> arrayType = lookupClass(encodedName.toString());
        if (arrayType == null) {
            throw new UnsupportedOperationException("No array type possible for " + componentType.getName());
        }
        return arrayType;
    }

    public static Type[] expandsVarargs(Type[] parameters, int count) {
        final Type[] expanded = new Type[count];
        final int lastIndex = parameters.length - 1;
        final Type lastParameter = parameters[lastIndex];
        if (!lastParameter.isArray()) {
            throw new IllegalArgumentException("Expected last parameter to be an array type, but got " + lastParameter);
        }
        final Type varargType = ((ReferenceType) lastParameter).getComponentType();
        System.arraycopy(parameters, 0, expanded, 0, lastIndex);
        for (int i = lastIndex; i < count; i++) {
            expanded[i] = varargType;
        }
        return expanded;
    }

    public static Object[] compactVarargs(ComponentType varargType, int varargIndex, Object[] arguments) {
        final Object[] compacted = new Object[varargIndex + 1];
        System.arraycopy(arguments, 0, compacted, 0, varargIndex);
        final int varargCount = arguments.length - varargIndex;
        final Object array = varargType.newArray(varargCount);
        for (int i = 0; i < varargCount; i++) {
            Array.set(array, i, arguments[i + varargIndex]);
        }
        compacted[varargIndex] = array;
        return compacted;
    }

    @SuppressWarnings("SuspiciousSystemArraycopy")
    public static Object cloneArray(Object array) {
        final int length = Array.getLength(array);
        final Object clone = Array.newInstance(array.getClass().getComponentType(), length);
        System.arraycopy(array, 0, clone, 0, length);
        return clone;
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
}
