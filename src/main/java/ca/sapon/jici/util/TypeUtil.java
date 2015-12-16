package ca.sapon.jici.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.sapon.jici.evaluator.type.ReferenceIntersectionType;
import ca.sapon.jici.evaluator.type.ParametrizedType;
import ca.sapon.jici.evaluator.type.PrimitiveType;
import ca.sapon.jici.evaluator.type.SingleReferenceType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.type.TypeArgument;

/**
 *
 */
public final class TypeUtil {
    private static final Map<Class<?>, Set<Class<?>>> VALID_CONVERSIONS = new HashMap<>();

    static {
        VALID_CONVERSIONS.put(boolean.class, new HashSet<Class<?>>(Collections.singletonList(boolean.class)));
        VALID_CONVERSIONS.put(byte.class, new HashSet<Class<?>>(Arrays.asList(byte.class, short.class, int.class, long.class, float.class, double.class)));
        VALID_CONVERSIONS.put(short.class, new HashSet<Class<?>>(Arrays.asList(short.class, int.class, long.class, float.class, double.class)));
        VALID_CONVERSIONS.put(char.class, new HashSet<Class<?>>(Arrays.asList(char.class, int.class, long.class, float.class, double.class)));
        VALID_CONVERSIONS.put(int.class, new HashSet<Class<?>>(Arrays.asList(int.class, long.class, float.class, double.class)));
        VALID_CONVERSIONS.put(long.class, new HashSet<Class<?>>(Arrays.asList(long.class, float.class, double.class)));
        VALID_CONVERSIONS.put(float.class, new HashSet<Class<?>>(Arrays.asList(float.class, double.class)));
        VALID_CONVERSIONS.put(double.class, new HashSet<Class<?>>(Collections.singletonList(double.class)));
    }

    private TypeUtil() {
    }

    public static boolean convertibleTo(Class<?> from, Class<?> to) {
        if (from.isPrimitive()) {
            if (to.isPrimitive()) {
                return VALID_CONVERSIONS.get(from).contains(to);
            }
            return convertibleTo(PrimitiveType.box(from).getTypeClass(), to);
        }
        if (to.isPrimitive()) {
            final Class<?> unbox = SingleReferenceType.unbox(from);
            return unbox != from && convertibleTo(unbox, to);
        }
        return to.isAssignableFrom(from);
    }

    public static boolean convertibleTo(Type from, Type to) {
        // Null can be converted to anything that isn't a primitive type
        if (from.isNull()) {
            return !to.isPrimitive();
        }
        // Void can't be converted to anything
        if (from.isVoid()) {
            return false;
        }
        // Primitive types can be converted to certain primitive types
        // If the target type isn't primitive, box the source and try again
        if (from.isPrimitive()) {
            final PrimitiveType source = (PrimitiveType) from;
            if (to.isPrimitive()) {
                final PrimitiveType target = (PrimitiveType) to;
                return VALID_CONVERSIONS.get(source.getTypeClass()).contains(target.getTypeClass());
            }
            return convertibleTo(source.box(), to);
        }
        // Parametrized types are a special case of single class types
        // They are only convertible between each other if the raw types are
        // And the target parameter types contains the source ones
        if (from instanceof ParametrizedType && to instanceof ParametrizedType) {
            final ParametrizedType source = (ParametrizedType) from;
            final ParametrizedType target = (ParametrizedType) to;
            if (!convertibleTo(source.getRaw(), target.getRaw())) {
                return false;
            }
            final List<TypeArgument> sourceArguments = source.getArguments();
            final List<TypeArgument> targetArguments = target.getArguments();
            if (sourceArguments.size() != targetArguments.size()) {
                return false;
            }
            for (int i = 0; i < sourceArguments.size(); i++) {
                if (!targetArguments.get(i).contains(sourceArguments.get(i))) {
                    return false;
                }
            }
            return true;
        }
        // Single class types might be convertible to a primitive if they can be unboxed
        // Else they can be cast to another single class if they are a subtype
        // They can also be converted to an intersection if they can be converted to each member
        if (from instanceof SingleReferenceType) {
            final SingleReferenceType source = (SingleReferenceType) from;
            if (to.isPrimitive()) {
                return source.isBox() && convertibleTo(source.unbox(), to);
            }
            if (to instanceof SingleReferenceType) {
                final SingleReferenceType target = (SingleReferenceType) to;
                return target.getTypeClass().isAssignableFrom(source.getTypeClass());
            }
            if (to instanceof ReferenceIntersectionType) {
                final ReferenceIntersectionType target = (ReferenceIntersectionType) to;
                for (Class<?> _class : target.getTypeClasses()) {
                    if (!_class.isAssignableFrom(source.getTypeClass())) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
        // An intersection of class types can be treated as any of its bounds
        if (from instanceof ReferenceIntersectionType) {
            final ReferenceIntersectionType source = (ReferenceIntersectionType) from;
            for (SingleReferenceType bound : source.getLowestUpperBound()) {
                if (convertibleTo(bound, to)) {
                    return true;
                }
            }
            return false;
        }
        throw new UnsupportedOperationException("Cannot convert from " + from.getName() + " to " + to.getName());
    }
}
