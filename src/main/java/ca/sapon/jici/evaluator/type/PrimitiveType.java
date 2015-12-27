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

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ca.sapon.jici.evaluator.value.ValueKind;
import ca.sapon.jici.util.ReflectionUtil;

/**
 * One of the eight primitive types: {@code boolean}, {@code byte}, {@code short}, {@code char}, {@code int}, {@code long}, {@code float} or {@code double}.
 */
public class PrimitiveType implements LiteralType {
    public static final PrimitiveType THE_BOOLEAN;
    public static final PrimitiveType THE_BYTE;
    public static final PrimitiveType THE_SHORT;
    public static final PrimitiveType THE_CHAR;
    public static final PrimitiveType THE_INT;
    public static final PrimitiveType THE_LONG;
    public static final PrimitiveType THE_FLOAT;
    public static final PrimitiveType THE_DOUBLE;
    private static final Map<Class<?>, PrimitiveType> ALL_TYPES = new HashMap<>();
    private static final Map<Class<?>, Set<Class<?>>> PRIMITIVE_CONVERSIONS = new HashMap<>();
    private static final Map<Class<?>, LiteralReferenceType> BOXING_CONVERSIONS = new HashMap<>();
    private static final Map<Class<?>, RangeChecker> NARROW_CHECKERS = new HashMap<>();
    private static final Set<Class<?>> UNARY_WIDENS_INT = new HashSet<>();
    private static final Map<Class<?>, Widener> BINARY_WIDENERS = new HashMap<>();
    private final Class<?> type;
    private final ValueKind kind;

    static {
        THE_BOOLEAN = new PrimitiveType(boolean.class, ValueKind.BOOLEAN);
        THE_BYTE = new PrimitiveType(byte.class, ValueKind.BYTE);
        THE_SHORT = new PrimitiveType(short.class, ValueKind.SHORT);
        THE_CHAR = new PrimitiveType(char.class, ValueKind.CHAR);
        THE_INT = new PrimitiveType(int.class, ValueKind.INT);
        THE_LONG = new PrimitiveType(long.class, ValueKind.LONG);
        THE_FLOAT = new PrimitiveType(float.class, ValueKind.FLOAT);
        THE_DOUBLE = new PrimitiveType(double.class, ValueKind.DOUBLE);

        ALL_TYPES.put(boolean.class, THE_BOOLEAN);
        ALL_TYPES.put(byte.class, THE_BYTE);
        ALL_TYPES.put(short.class, THE_SHORT);
        ALL_TYPES.put(char.class, THE_CHAR);
        ALL_TYPES.put(int.class, THE_INT);
        ALL_TYPES.put(long.class, THE_LONG);
        ALL_TYPES.put(float.class, THE_FLOAT);
        ALL_TYPES.put(double.class, THE_DOUBLE);

        PRIMITIVE_CONVERSIONS.put(boolean.class, new HashSet<Class<?>>(Collections.singletonList(boolean.class)));
        PRIMITIVE_CONVERSIONS.put(byte.class, new HashSet<Class<?>>(Arrays.asList(byte.class, short.class, int.class, long.class, float.class, double.class)));
        PRIMITIVE_CONVERSIONS.put(short.class, new HashSet<Class<?>>(Arrays.asList(short.class, int.class, long.class, float.class, double.class)));
        PRIMITIVE_CONVERSIONS.put(char.class, new HashSet<Class<?>>(Arrays.asList(char.class, int.class, long.class, float.class, double.class)));
        PRIMITIVE_CONVERSIONS.put(int.class, new HashSet<Class<?>>(Arrays.asList(int.class, long.class, float.class, double.class)));
        PRIMITIVE_CONVERSIONS.put(long.class, new HashSet<Class<?>>(Arrays.asList(long.class, float.class, double.class)));
        PRIMITIVE_CONVERSIONS.put(float.class, new HashSet<Class<?>>(Arrays.asList(float.class, double.class)));
        PRIMITIVE_CONVERSIONS.put(double.class, new HashSet<Class<?>>(Collections.singletonList(double.class)));

        BOXING_CONVERSIONS.put(boolean.class, LiteralReferenceType.of(Boolean.class));
        BOXING_CONVERSIONS.put(byte.class, LiteralReferenceType.of(Byte.class));
        BOXING_CONVERSIONS.put(short.class, LiteralReferenceType.of(Short.class));
        BOXING_CONVERSIONS.put(char.class, LiteralReferenceType.of(Character.class));
        BOXING_CONVERSIONS.put(int.class, LiteralReferenceType.of(Integer.class));
        BOXING_CONVERSIONS.put(long.class, LiteralReferenceType.of(Long.class));
        BOXING_CONVERSIONS.put(float.class, LiteralReferenceType.of(Float.class));
        BOXING_CONVERSIONS.put(double.class, LiteralReferenceType.of(Double.class));

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
    }

    private PrimitiveType(Class<?> type, ValueKind kind) {
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
    public boolean isReference() {
        return false;
    }

    @Override
    public boolean isReifiable() {
        return true;
    }

    @Override
    public LiteralReferenceType asArray(int dimensions) {
        return LiteralReferenceType.of(ReflectionUtil.asArrayType(type, dimensions));
    }

    @Override
    public Object newArray(int length) {
        return Array.newInstance(type, length);
    }

    @Override
    public Object newArray(int[] lengths) {
        return Array.newInstance(type, lengths);
    }

    public SingleReferenceType box() {
        return BOXING_CONVERSIONS.get(type);
    }

    public boolean canNarrowFrom(int value) {
        final RangeChecker checker = NARROW_CHECKERS.get(type);
        return checker != null && checker.contains(value);
    }

    public PrimitiveType unaryWiden() {
        return of(UNARY_WIDENS_INT.contains(type) ? int.class : type);
    }

    public PrimitiveType binaryWiden(PrimitiveType with) {
        return of(BINARY_WIDENERS.get(type).widen(with.getTypeClass()));
    }

    @Override
    public boolean convertibleTo(Type to) {
        // Primitive types can be converted to certain primitive types
        // If the target type isn't primitive, box the source and try again
        if (to.isPrimitive()) {
            final PrimitiveType target = (PrimitiveType) to;
            return PRIMITIVE_CONVERSIONS.get(type).contains(target.getTypeClass());
        }
        return box().convertibleTo(to);
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof PrimitiveType && this.type == ((PrimitiveType) other).getTypeClass();
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    public static PrimitiveType of(Class<?> type) {
        return ALL_TYPES.get(type);
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
