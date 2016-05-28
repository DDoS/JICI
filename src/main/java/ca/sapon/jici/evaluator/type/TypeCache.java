package ca.sapon.jici.evaluator.type;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class TypeCache {
    private static final ThreadLocal<Map<java.lang.reflect.Type, Type>> CACHE = new ThreadLocal<Map<java.lang.reflect.Type, Type>>() {
        @Override
        protected Map<java.lang.reflect.Type, Type> initialValue() {
            return new HashMap<>();
        }
    };

    public int getCacheCount() {
        return CACHE.get().size();
    }

    public void purgeCache() {
        CACHE.get().clear();
    }

    public static Type[] wrapClasses(Class<?>[] types) {
        final Type[] wrapped = new Type[types.length];
        for (int i = 0; i < types.length; i++) {
            wrapped[i] = wrapClass(types[i]);
        }
        return wrapped;
    }

    public static Type[] wrapTypes(java.lang.reflect.Type[] types) {
        final Type[] wrapped = new Type[types.length];
        for (int i = 0; i < types.length; i++) {
            wrapped[i] = wrapType(types[i]);
        }
        return wrapped;
    }

    public static TypeVariable[] wrapTypeVariables(java.lang.reflect.TypeVariable<?>[] typeVariables) {
        final TypeVariable[] wrapped = new TypeVariable[typeVariables.length];
        for (int i = 0; i < typeVariables.length; i++) {
            wrapped[i] = wrapTypeVariable(typeVariables[i]);
        }
        return wrapped;
    }

    public static Type wrapType(java.lang.reflect.Type type) {
        if (type == null) {
            throw new NullPointerException("type");
        }
        if (type instanceof Class<?>) {
            return wrapClass((Class<?>) type);
        }
        if (type instanceof GenericArrayType) {
            return wrapGenericArrayType((GenericArrayType) type);
        }
        if (type instanceof ParameterizedType) {
            return wrapParameterizedType((ParameterizedType) type);
        }
        if (type instanceof java.lang.reflect.TypeVariable) {
            return wrapTypeVariable((java.lang.reflect.TypeVariable<?>) type);
        }
        if (type instanceof java.lang.reflect.WildcardType) {
            return wrapWildcard((java.lang.reflect.WildcardType) type);
        }
        throw new UnsupportedOperationException(type.getClass().getCanonicalName());
    }

    public static Type wrapClass(Class<?> type) {
        if (type == null) {
            throw new NullPointerException("type");
        }
        if (type == void.class) {
            return VoidType.THE_VOID;
        }
        // Primitive types are enumerated, a cache would be redundant
        if (type.isPrimitive()) {
            return PrimitiveType.of(type);
        }
        // Check cache
        final TypeArgument cached = checkCache(type);
        if (cached != null) {
            return cached;
        }
        // Not in cache, create
        return cache(type, LiteralReferenceType.of(type));
    }

    public static SingleReferenceType wrapGenericArrayType(GenericArrayType type) {
        // Check cache
        final SingleReferenceType cached = checkCache(type);
        if (cached != null) {
            return cached;
        }
        // Not in cache, create
        java.lang.reflect.Type componentType = type.getGenericComponentType();
        int dimensions = 1;
        while (componentType instanceof GenericArrayType) {
            componentType = ((GenericArrayType) componentType).getGenericComponentType();
            dimensions++;
        }
        final Type wrapped = wrapType(componentType);
        if (wrapped instanceof SingleReferenceType) {
            return cache(type, ((SingleReferenceType) wrapped).asArray(dimensions));
        }
        throw new UnsupportedOperationException("Invalid component type: " + wrapped.getName());
    }

    public static LiteralReferenceType wrapParameterizedType(ParameterizedType type) {
        // Check cache
        final LiteralReferenceType cached = checkCache(type);
        if (cached != null) {
            return cached;
        }
        // Not in cache, create
        final java.lang.reflect.Type[] args = type.getActualTypeArguments();
        final List<TypeArgument> wrappedArgs;
        if (args.length <= 0) {
            wrappedArgs = Collections.emptyList();
        } else {
            wrappedArgs = new ArrayList<>(args.length);
            for (java.lang.reflect.Type param : args) {
                final Type wrap = wrapType(param);
                if (!(wrap instanceof TypeArgument)) {
                    throw new UnsupportedOperationException("Invalid type for generic parameter: " + wrap.getName());
                }
                wrappedArgs.add(((TypeArgument) wrap));
            }
        }
        final java.lang.reflect.Type ownerType = type.getOwnerType();
        final ParametrizedType wrappedOwner;
        if (ownerType != null) {
            final Type wrapped = wrapType(ownerType);
            wrappedOwner = wrapped instanceof ParametrizedType ? (ParametrizedType) wrapped : null;
        } else {
            wrappedOwner = null;
        }
        if (!wrappedArgs.isEmpty() || wrappedOwner != null) {
            return cache(type, ParametrizedType.of(wrappedOwner, (Class<?>) type.getRawType(), wrappedArgs));
        }
        return cache(type, LiteralReferenceType.of((Class<?>) type.getRawType()));
    }

    public static TypeVariable wrapTypeVariable(java.lang.reflect.TypeVariable<?> type) {
        // Check cache
        final TypeVariable cached = checkCache(type);
        if (cached != null) {
            return cached;
        }
        // Not in cache, create
        // Place a boundless version in the cache so any type variable cycle will be broken in subsequent calls to wrap the bounds
        // This instance will be replaced by the correct bounded version after it gets wrapped
        cache(type, TypeVariable.of(type.getName(), IntersectionType.EVERYTHING, IntersectionType.NOTHING, type.getGenericDeclaration()));
        final List<SingleReferenceType> wrappedUpper = wrapBounds(type.getBounds(), new ArrayList<SingleReferenceType>());
        return cache(type, TypeVariable.of(type.getName(), wrappedUpper, type.getGenericDeclaration()));
    }

    public static WildcardType wrapWildcard(java.lang.reflect.WildcardType type) {
        // Check cache
        final WildcardType cached = checkCache(type);
        if (cached != null) {
            return cached;
        }
        // Not in cache, create
        final Set<SingleReferenceType> wrappedLower = wrapBounds(type.getLowerBounds(), new HashSet<SingleReferenceType>());
        final Set<SingleReferenceType> wrappedUpper = wrapBounds(type.getUpperBounds(), new HashSet<SingleReferenceType>());
        return cache(type, WildcardType.of(wrappedLower, wrappedUpper));
    }

    private static <C extends Collection<SingleReferenceType>> C wrapBounds(java.lang.reflect.Type[] types, C to) {
        for (java.lang.reflect.Type type : types) {
            final Type wrap = wrapType(type);
            if (!(wrap instanceof SingleReferenceType)) {
                throw new UnsupportedOperationException("Invalid type for bound: " + wrap.getName());
            }
            to.add((SingleReferenceType) wrap);
        }
        return to;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Type> T checkCache(java.lang.reflect.Type type) {
        return (T) CACHE.get().get(type);
    }

    private static <T extends Type> T cache(java.lang.reflect.Type type, T cache) {
        CACHE.get().put(type, cache);
        return cache;
    }
}
