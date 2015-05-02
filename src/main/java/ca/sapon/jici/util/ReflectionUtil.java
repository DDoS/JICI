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
    private static final Map<Class<?>, Set<Class<?>>> VALID_UNBOXED_CONVERSIONS = new HashMap<>();
    private static final Map<Class<?>, Set<Class<?>>> VALID_BOXED_CONVERSIONS = new HashMap<>();

    static {
        VALID_UNBOXED_CONVERSIONS.put(Boolean.class, toSet(boolean.class));
        VALID_UNBOXED_CONVERSIONS.put(Byte.class, toSet(byte.class, short.class, int.class, long.class, float.class, double.class));
        VALID_UNBOXED_CONVERSIONS.put(Short.class, toSet(short.class, int.class, long.class, float.class, double.class));
        VALID_UNBOXED_CONVERSIONS.put(Character.class, toSet(char.class, int.class, long.class, float.class, double.class));
        VALID_UNBOXED_CONVERSIONS.put(Integer.class, toSet(int.class, long.class, float.class, double.class));
        VALID_UNBOXED_CONVERSIONS.put(Long.class, toSet(long.class, float.class, double.class));
        VALID_UNBOXED_CONVERSIONS.put(Float.class, toSet(float.class, double.class));
        VALID_UNBOXED_CONVERSIONS.put(Double.class, toSet(double.class));

        VALID_BOXED_CONVERSIONS.put(Boolean.class, toSet(Boolean.class));
        VALID_BOXED_CONVERSIONS.put(Byte.class, toSet(Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class));
        VALID_BOXED_CONVERSIONS.put(Short.class, toSet(Short.class, Integer.class, Long.class, Float.class, Double.class));
        VALID_BOXED_CONVERSIONS.put(Character.class, toSet(Character.class, Integer.class, Long.class, Float.class, Double.class));
        VALID_BOXED_CONVERSIONS.put(Integer.class, toSet(Integer.class, Long.class, Float.class, Double.class));
        VALID_BOXED_CONVERSIONS.put(Long.class, toSet(Long.class, Float.class, Double.class));
        VALID_BOXED_CONVERSIONS.put(Float.class, toSet(Float.class, Double.class));
        VALID_BOXED_CONVERSIONS.put(Double.class, toSet(Double.class));
    }

    public static boolean validateArgumentTypes(Class<?>[] types, Object[] arguments) {
        final int length = types.length;
        if (length != arguments.length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            final Class<?> type = types[i];
            final Class<?> argument = arguments[i].getClass();
            if (type.isPrimitive()) {
                if (argument == null) {
                    return false;
                }
                final Set<Class<?>> conversions = VALID_UNBOXED_CONVERSIONS.get(argument);
                if (conversions == null || !conversions.contains(type)) {
                    return false;
                }
            } else {
                if (argument != null && !type.isAssignableFrom(argument)) {
                    final Set<Class<?>> conversions = VALID_BOXED_CONVERSIONS.get(argument);
                    if (conversions == null || !conversions.contains(type)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static Set<Class<?>> toSet(Class<?>... values) {
        final Set<Class<?>> set = new HashSet<>(values.length);
        Collections.addAll(set, values);
        return set;
    }
}
