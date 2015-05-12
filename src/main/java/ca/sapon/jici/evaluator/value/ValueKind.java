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
package ca.sapon.jici.evaluator.value;

import java.util.HashMap;
import java.util.Map;

public enum ValueKind {
    BOOLEAN(Boolean.class) {
        @Override
        public BooleanValue defaultValue() {
            return BooleanValue.defaultValue();
        }

        @Override
        public BooleanValue wrap(Object object) {
            if (object instanceof Boolean) {
                return BooleanValue.of((Boolean) object);
            }
            throw new IllegalArgumentException("Cannot cast an object to a boolean");
        }

        @Override
        public BooleanValue convert(Value value) {
            return BooleanValue.of(value.asBoolean());
        }
    },
    BYTE(Byte.class) {
        @Override
        public ByteValue defaultValue() {
            return ByteValue.defaultValue();
        }

        @Override
        public ByteValue wrap(Object object) {
            if (object instanceof Byte) {
                return ByteValue.of((Byte) object);
            }
            throw new IllegalArgumentException("Cannot cast an object to a byte");
        }

        @Override
        public ByteValue convert(Value value) {
            return ByteValue.of(value.asByte());
        }
    },
    SHORT(Short.class) {
        @Override
        public ShortValue defaultValue() {
            return ShortValue.defaultValue();
        }

        @Override
        public ShortValue wrap(Object object) {
            if (object instanceof Short) {
                return ShortValue.of((Short) object);
            }
            throw new IllegalArgumentException("Cannot cast an object to a short");
        }

        @Override
        public ShortValue convert(Value value) {
            return ShortValue.of(value.asShort());
        }
    },
    CHAR(Character.class) {
        @Override
        public CharValue defaultValue() {
            return CharValue.defaultValue();
        }

        @Override
        public CharValue wrap(Object object) {
            if (object instanceof Character) {
                return CharValue.of((Character) object);
            }
            throw new IllegalArgumentException("Cannot cast an object to a char");
        }

        @Override
        public CharValue convert(Value value) {
            return CharValue.of(value.asChar());
        }
    },
    INT(Integer.class) {
        @Override
        public IntValue defaultValue() {
            return IntValue.defaultValue();
        }

        @Override
        public IntValue wrap(Object object) {
            if (object instanceof Integer) {
                return IntValue.of((Integer) object);
            }
            throw new IllegalArgumentException("Cannot cast an object to an int");
        }

        @Override
        public IntValue convert(Value value) {
            return IntValue.of(value.asInt());
        }
    },
    LONG(Long.class) {
        @Override
        public LongValue defaultValue() {
            return LongValue.defaultValue();
        }

        @Override
        public LongValue wrap(Object object) {
            if (object instanceof Long) {
                return LongValue.of((Long) object);
            }
            throw new IllegalArgumentException("Cannot cast an object to a long");
        }

        @Override
        public LongValue convert(Value value) {
            return LongValue.of(value.asLong());
        }
    },
    FLOAT(Float.class) {
        @Override
        public FloatValue defaultValue() {
            return FloatValue.defaultValue();
        }

        @Override
        public FloatValue wrap(Object object) {
            if (object instanceof Float) {
                return FloatValue.of((Float) object);
            }
            throw new IllegalArgumentException("Cannot cast an object to a float");
        }

        @Override
        public FloatValue convert(Value value) {
            return FloatValue.of(value.asFloat());
        }
    },
    DOUBLE(Double.class) {
        @Override
        public DoubleValue defaultValue() {
            return DoubleValue.defaultValue();
        }

        @Override
        public DoubleValue wrap(Object object) {
            if (object instanceof Double) {
                return DoubleValue.of((Double) object);
            }
            throw new IllegalArgumentException("Cannot cast an object to a double");
        }

        @Override
        public DoubleValue convert(Value value) {
            return DoubleValue.of(value.asDouble());
        }
    },
    VOID(Void.class) {
        @Override
        public Value defaultValue() {
            return VoidValue.defaultValue();
        }

        @Override
        public Value wrap(Object object) {
            if (object instanceof Void) {
                return VoidValue.THE_VOID;
            }
            throw new IllegalArgumentException("Cannot cast an object to void");
        }

        @Override
        public Value convert(Value value) {
            throw new IllegalArgumentException("Cannot convert anything to void");
        }
    },
    OBJECT(null) {
        @Override
        public ObjectValue defaultValue() {
            return ObjectValue.defaultValue();
        }

        @Override
        public ObjectValue wrap(Object object) {
            return ObjectValue.of(object);
        }

        @Override
        public ObjectValue convert(Value value) {
            return ObjectValue.of(value.asObject());
        }
    };
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

    public abstract Value defaultValue();

    public abstract Value wrap(Object object);

    public abstract Value convert(Value value);

    public Class<?> getBoxingClass() {
        return boxingClass;
    }

    public static Value unbox(Object box) {
        final ValueKind kind = BOX_TO_KIND.get(box.getClass());
        if (kind == null) {
            return null;
        }
        if (kind == OBJECT) {
            throw new IllegalArgumentException("Not a box type: " + box.getClass().getSimpleName());
        }
        return kind.wrap(box);
    }
}
