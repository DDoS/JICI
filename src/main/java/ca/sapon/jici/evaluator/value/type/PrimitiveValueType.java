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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ca.sapon.jici.evaluator.value.ValueKind;
import ca.sapon.jici.util.ReflectionUtil;

/**
 *
 */
public class PrimitiveValueType implements ValueType {
    public static final PrimitiveValueType THE_BOOLEAN;
    public static final PrimitiveValueType THE_BYTE;
    public static final PrimitiveValueType THE_SHORT;
    public static final PrimitiveValueType THE_CHAR;
    public static final PrimitiveValueType THE_INT;
    public static final PrimitiveValueType THE_LONG;
    public static final PrimitiveValueType THE_FLOAT;
    public static final PrimitiveValueType THE_DOUBLE;
    private static final Map<Class<?>, PrimitiveValueType> ALL_TYPES = new HashMap<>();
    private static final Map<Class<?>, RangeChecker> NARROW_CHECKERS = new HashMap<>();
    private static final Set<Class<?>> UNARY_WIDENS_INT = new HashSet<>();
    private static final Map<Class<?>, Widener> BINARY_WIDENERS = new HashMap<>();
    private static final Map<Class<?>, Set<Class<?>>> VALID_CONVERSIONS = new HashMap<>();
    private static final Map<Class<?>, ObjectValueType> BOXING_CONVERSIONS = new HashMap<>();
    private final Class<?> type;
    private final ValueKind kind;

    static {
        THE_BOOLEAN = new PrimitiveValueType(boolean.class, ValueKind.BOOLEAN);
        THE_BYTE = new PrimitiveValueType(byte.class, ValueKind.BYTE);
        THE_SHORT = new PrimitiveValueType(short.class, ValueKind.SHORT);
        THE_CHAR = new PrimitiveValueType(char.class, ValueKind.CHAR);
        THE_INT = new PrimitiveValueType(int.class, ValueKind.INT);
        THE_LONG = new PrimitiveValueType(long.class, ValueKind.LONG);
        THE_FLOAT = new PrimitiveValueType(float.class, ValueKind.FLOAT);
        THE_DOUBLE = new PrimitiveValueType(double.class, ValueKind.DOUBLE);

        ALL_TYPES.put(boolean.class, THE_BOOLEAN);
        ALL_TYPES.put(byte.class, THE_BYTE);
        ALL_TYPES.put(short.class, THE_SHORT);
        ALL_TYPES.put(char.class, THE_CHAR);
        ALL_TYPES.put(int.class, THE_INT);
        ALL_TYPES.put(long.class, THE_LONG);
        ALL_TYPES.put(float.class, THE_FLOAT);
        ALL_TYPES.put(double.class, THE_DOUBLE);

        NARROW_CHECKERS.put(byte.class, new RangeChecker(-128, 0xFF));
        NARROW_CHECKERS.put(short.class, new RangeChecker(-32768, 0xFFFF));
        NARROW_CHECKERS.put(char.class, new RangeChecker(0, 0xFFFF));

        Collections.addAll(UNARY_WIDENS_INT, byte.class, short.class, char.class);

        final Widener intWidener = new Widener(int.class, byte.class, short.class, char.class);
        BINARY_WIDENERS.put(byte.class, intWidener);
        BINARY_WIDENERS.put(short.class, intWidener);
        BINARY_WIDENERS.put(char.class, intWidener);
        BINARY_WIDENERS.put(int.class, intWidener);
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

        BOXING_CONVERSIONS.put(boolean.class, ObjectValueType.of(Boolean.class));
        BOXING_CONVERSIONS.put(byte.class, ObjectValueType.of(Byte.class));
        BOXING_CONVERSIONS.put(short.class, ObjectValueType.of(Short.class));
        BOXING_CONVERSIONS.put(char.class, ObjectValueType.of(Character.class));
        BOXING_CONVERSIONS.put(int.class, ObjectValueType.of(Integer.class));
        BOXING_CONVERSIONS.put(long.class, ObjectValueType.of(Long.class));
        BOXING_CONVERSIONS.put(float.class, ObjectValueType.of(Float.class));
        BOXING_CONVERSIONS.put(double.class, ObjectValueType.of(Double.class));
    }

    private PrimitiveValueType(Class<?> type, ValueKind kind) {
        this.type = type;
        this.kind = kind;
    }

    @Override
    public Class<?> getTypeClass() {
        return type;
    }

    @Override
    public String getName() {
        return type.getCanonicalName();
    }

    @Override
    public ValueKind getKind() {
        return kind;
    }

    @Override
    public boolean is(ValueType type) {
        return type instanceof PrimitiveValueType && this.type == ((PrimitiveValueType) type).getTypeClass();
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
        return true;
    }

    @Override
    public boolean isNumeric() {
        return isIntegral() || type == float.class || type == double.class;
    }

    @Override
    public boolean isIntegral() {
        return type == byte.class || type == short.class || type == char.class || type == int.class || type == long.class;
    }

    @Override
    public boolean isBoolean() {
        return type == boolean.class;
    }

    @Override
    public boolean isArray() {
        return false;
    }

    @Override
    public boolean isObject() {
        return false;
    }

    @Override
    public ValueType unbox() {
        return this;
    }

    @Override
    public ObjectValueType box() {
        return box(type);
    }

    @Override
    public boolean canNarrowFrom(int value) {
        final RangeChecker checker = NARROW_CHECKERS.get(type);
        return checker != null && checker.contains(value);
    }

    @Override
    public PrimitiveValueType unaryWiden() {
        return of(unaryWiden(type));
    }

    @Override
    public PrimitiveValueType binaryWiden(ValueType with) {
        if (!(with instanceof PrimitiveValueType)) {
            throw new IllegalArgumentException("Cannot binary widen an object type");
        }
        return of(binaryWiden(type, ((PrimitiveValueType) with).getTypeClass()));
    }

    @Override
    public boolean convertibleTo(ValueType to) {
        if (to instanceof ObjectUnionValueType) {
            throw new IllegalArgumentException("Cannot convert to an object union type");
        }
        return convertibleTo(type, to.getTypeClass());
    }

    @Override
    public Constructor<?> getConstructor(ValueType[] arguments) {
        throw new IllegalArgumentException("Cannot dereference a primitive type");
    }

    @Override
    public Constructor<?> getVarargConstructor(ValueType[] arguments) {
        return getConstructor(arguments);
    }

    @Override
    public Field getField(String name) {
        throw new IllegalArgumentException("Cannot dereference a primitive type");
    }

    @Override
    public Method getMethod(String name, ValueType[] arguments) {
        throw new IllegalArgumentException("Cannot dereference a primitive type");
    }

    @Override
    public Method getVarargMethod(String name, ValueType[] arguments) {
        return getMethod(name, arguments);
    }

    @Override
    public String toString() {
        return getName();
    }

    public static ObjectValueType box(Class<?> type) {
        return BOXING_CONVERSIONS.get(type);
    }

    public static Class<?> unaryWiden(Class<?> type) {
        return UNARY_WIDENS_INT.contains(type) ? int.class : type;
    }

    public static Class<?> binaryWiden(Class<?> type, Class<?> with) {
        return BINARY_WIDENERS.get(type).widen(with);
    }

    public static boolean convertibleTo(Class<?> from, Class<?> to) {
        if (!to.isPrimitive()) {
            to = ObjectValueType.unbox(to);
            if (!to.isPrimitive()) {
                return box(from).convertibleTo(ReflectionUtil.wrap(to));
            }
        }
        return VALID_CONVERSIONS.get(from).contains(to);
    }

    public static PrimitiveValueType of(Class<?> type) {
        return ALL_TYPES.get(type);
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
