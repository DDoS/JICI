package ca.sapon.jici.evaluator.type;

/**
 * A type that can be the component of an array, such as {@code int}, {@code int[]}, {@code String}, {@code String[]}, {@code List<Integer>} or {@code <T>}.
 */
public interface ComponentType extends Type {
    SingleReferenceType asArray(int dimensions);

    Object newArray(int length);

    Object newArray(int[] lengths);
}
