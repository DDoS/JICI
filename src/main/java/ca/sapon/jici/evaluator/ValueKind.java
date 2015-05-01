/**
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

import java.util.HashMap;
import java.util.Map;

public enum ValueKind {
    BOOLEAN(Boolean.class),
    BYTE(Byte.class),
    SHORT(Short.class),
    CHAR(Character.class),
    INT(Integer.class),
    LONG(Long.class),
    FLOAT(Float.class),
    DOUBLE(Double.class),
    OBJECT(null);
    private static final Map<Class<?>, ValueKind> BOX_TO_KIND = new HashMap<>();
    private final Class<?> boxingClass;

    static {
        for (ValueKind kind : values()) {
            BOX_TO_KIND.put(kind.getBoxingClass(), kind);
        }
    }

    ValueKind(Class<?> boxingClass) {
        this.boxingClass = boxingClass;
    }

    public Class<?> getBoxingClass() {
        return boxingClass;
    }

    public static Value boxToValue(Object box) {
        final ValueKind kind = BOX_TO_KIND.get(box.getClass());
        if (kind == null) {
            return null;
        }
        switch (kind) {
            case BOOLEAN:
                return BooleanValue.of((Boolean) box);
            case BYTE:
                return ByteValue.of((Byte) box);
            case SHORT:
                return ShortValue.of((Short) box);
            case CHAR:
                return CharValue.of((Character) box);
            case INT:
                return IntValue.of((Integer) box);
            case LONG:
                return LongValue.of((Long) box);
            case FLOAT:
                return FloatValue.of((Float) box);
            case DOUBLE:
                return DoubleValue.of((Double) box);
            default:
                throw new IllegalArgumentException("Not a box type: " + box.getClass().getSimpleName());
        }
    }

    public static boolean canNarrowTo(ValueKind kind, int value) {
        switch (kind) {
            case BYTE:
                return (value + 128 & ~0xFF) == 0;
            case SHORT:
                return (value + 32768 & ~0xFFFF) == 0;
            case CHAR:
                return (value & ~0xFFFF) == 0;
            default:
                return false;
        }
    }

    public static ValueKind unaryWidensTo(ValueKind inner) {
        // BYTE, SHORT CHAR to INT
        if ((1 << inner.ordinal() & 0b1110) != 0) {
            return INT;
        }
        // anything else to itself
        return inner;
    }

    public static ValueKind binaryWidensTo(ValueKind left, ValueKind right) {
        final int mask = 1 << left.ordinal() | 1 << right.ordinal();
        // either DOUBLE to DOUBLE
        if ((mask & 0b10000000) != 0) {
            return DOUBLE;
        }
        // either FLOAT to FLOAT
        if ((mask & 0b1000000) != 0) {
            return FLOAT;
        }
        // either LONG to LONG
        if ((mask & 0b100000) != 0) {
            return LONG;
        }
        // both BOOLEAN to BOOLEAN
        if (mask == 0b1) {
            return BOOLEAN;
        }
        // anything else to INT
        return INT;
    }
}
