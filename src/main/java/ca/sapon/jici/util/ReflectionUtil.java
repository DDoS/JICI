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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class ReflectionUtil {
    private static final Set<Class<?>> INTEGRAL_TYPES = new HashSet<>();
    private static final Set<Class<?>> NUMERIC_TYPES = new HashSet<>();
    private static final Set<Class<?>> LOGICAL_TYPES = new HashSet<>();
    private static final Map<Class<?>, Class<?>> UNBOXED_TYPES = new HashMap<>();
    private static final Map<Class<?>, RangeChecker> NARROW_CHECKERS = new HashMap<>();
    private static final Set<Class<?>> UNARY_WIDENS_INT = new HashSet<>();
    private static final Map<Class<?>, Widener> BINARY_WIDENERS = new HashMap<>();
    private static final Map<Class<?>, Set<Class<?>>> VALID_CONVERSIONS = new HashMap<>();
    private static final Map<Class<?>, Set<Class<?>>> VALID_UNBOX_CONVERSIONS = new HashMap<>();

    static {
        Collections.addAll(INTEGRAL_TYPES, byte.class, Byte.class, short.class, Short.class, char.class, Character.class, int.class, Integer.class, long.class, Long.class);

        NUMERIC_TYPES.addAll(INTEGRAL_TYPES);
        Collections.addAll(NUMERIC_TYPES, float.class, Float.class, double.class, Double.class);

        LOGICAL_TYPES.addAll(INTEGRAL_TYPES);
        Collections.addAll(LOGICAL_TYPES, boolean.class, Boolean.class);

        UNBOXED_TYPES.put(Boolean.class, boolean.class);
        UNBOXED_TYPES.put(Byte.class, byte.class);
        UNBOXED_TYPES.put(Short.class, short.class);
        UNBOXED_TYPES.put(Character.class, char.class);
        UNBOXED_TYPES.put(Integer.class, int.class);
        UNBOXED_TYPES.put(Long.class, long.class);
        UNBOXED_TYPES.put(Float.class, float.class);
        UNBOXED_TYPES.put(Double.class, double.class);

        NARROW_CHECKERS.put(byte.class, new RangeChecker(-128, 0xFF));
        NARROW_CHECKERS.put(short.class, new RangeChecker(-32768, 0xFFFF));
        NARROW_CHECKERS.put(char.class, new RangeChecker(0, 0xFFFF));

        Collections.addAll(UNARY_WIDENS_INT, byte.class, short.class, char.class);

        final Widener intWidener = new Widener(int.class, byte.class, short.class, char.class);
        BINARY_WIDENERS.put(byte.class, intWidener);
        BINARY_WIDENERS.put(short.class, intWidener);
        BINARY_WIDENERS.put(char.class, intWidener);
        BINARY_WIDENERS.put(long.class, new Widener(long.class, byte.class, short.class, char.class, int.class));
        BINARY_WIDENERS.put(float.class, new Widener(float.class, byte.class, short.class, char.class, int.class, long.class));
        BINARY_WIDENERS.put(double.class, new Widener(double.class, byte.class, short.class, char.class, int.class, long.class, float.class));

        VALID_CONVERSIONS.put(boolean.class, toSet(boolean.class));
        VALID_CONVERSIONS.put(byte.class, toSet(byte.class, short.class, int.class, long.class, float.class, double.class));
        VALID_CONVERSIONS.put(short.class, toSet(short.class, int.class, long.class, float.class, double.class));
        VALID_CONVERSIONS.put(char.class, toSet(char.class, int.class, long.class, float.class, double.class));
        VALID_CONVERSIONS.put(int.class, toSet(int.class, long.class, float.class, double.class));
        VALID_CONVERSIONS.put(long.class, toSet(long.class, float.class, double.class));
        VALID_CONVERSIONS.put(float.class, toSet(float.class, double.class));
        VALID_CONVERSIONS.put(double.class, toSet(double.class));

        VALID_UNBOX_CONVERSIONS.putAll(VALID_CONVERSIONS);
        VALID_UNBOX_CONVERSIONS.put(Boolean.class, toSet(boolean.class));
        VALID_UNBOX_CONVERSIONS.put(Byte.class, toSet(byte.class, short.class, int.class, long.class, float.class, double.class));
        VALID_UNBOX_CONVERSIONS.put(Short.class, toSet(short.class, int.class, long.class, float.class, double.class));
        VALID_UNBOX_CONVERSIONS.put(Character.class, toSet(char.class, int.class, long.class, float.class, double.class));
        VALID_UNBOX_CONVERSIONS.put(Integer.class, toSet(int.class, long.class, float.class, double.class));
        VALID_UNBOX_CONVERSIONS.put(Long.class, toSet(long.class, float.class, double.class));
        VALID_UNBOX_CONVERSIONS.put(Float.class, toSet(float.class, double.class));
        VALID_UNBOX_CONVERSIONS.put(Double.class, toSet(double.class));
    }

    public static boolean isNumeric(Class<?> type) {
        return NUMERIC_TYPES.contains(type);
    }

    public static boolean isLogical(Class<?> type) {
        return LOGICAL_TYPES.contains(type);
    }

    public static boolean isIntegral(Class<?> type) {
        return INTEGRAL_TYPES.contains(type);
    }

    public static boolean isBoolean(Class<?> type) {
        return type == boolean.class || type == Boolean.class;
    }

    public static Class<?> unbox(Class<?> type) {
        final Class<?> unboxed = UNBOXED_TYPES.get(type);
        return unboxed == null ? type : unboxed;
    }

    public static boolean canNarrowTo(Class<?> type, int value) {
        final RangeChecker checker = NARROW_CHECKERS.get(type);
        return checker != null && checker.contains(value);
    }

    public static Class<?> unaryWiden(Class<?> inner) {
        return UNARY_WIDENS_INT.contains(inner) ? int.class : inner;
    }

    public static Class<?> binaryWiden(Class<?> left, Class<?> right) {
        return BINARY_WIDENERS.get(left).widen(right);
    }

    public static boolean convertibleTo(Class<?>[] types, Class<?>[] arguments) {
        final int length = types.length;
        if (length != arguments.length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (!convertibleTo(arguments[i], types[i])) {
                return false;
            }
        }
        return true;
    }

    public static boolean convertibleTo(Class<?> from, Class<?> to) {
        // TODO: generics
        if (to.isPrimitive()) {
            if (from == null) {
                return false;
            }
            final Set<Class<?>> conversions = VALID_UNBOX_CONVERSIONS.get(from);
            if (conversions == null || !conversions.contains(to)) {
                return false;
            }
        } else {
            if (from != null && !to.isAssignableFrom(from)) {
                return false;
            }
        }
        return true;
    }

    public static Class<?> lookupClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException exception) {
            return null;
        }
    }

    private static Set<Class<?>> toSet(Class<?>... values) {
        final Set<Class<?>> set = new HashSet<>(values.length);
        Collections.addAll(set, values);
        return set;
    }

    private static class RangeChecker {
        private final int minValue;
        private final int invertedMask;

        private RangeChecker(int minValue, int mask) {
            this.minValue = minValue;
            invertedMask = ~mask;
        }

        private boolean contains(int value) {
            return (value - minValue & invertedMask) == 0;
        }
    }

    private static class Widener {
        private final Class<?> wider;
        private final Set<Class<?>> widens = new HashSet<>();

        private Widener(Class<?> wider, Class<?>... widens) {
            this.wider = wider;
            Collections.addAll(this.widens, widens);
        }

        private Class<?> widen(Class<?> type) {
            return widens.contains(type) ? wider : type;
        }
    }
}
