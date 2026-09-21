package net.thisptr.jackson.jq.v2.spi.type;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.jspecify.annotations.Nullable;

/**
 * An object type with known fields and a type for additional fields. A {@link NeverType} additional
 * field type makes the object closed.
 * <p>
 * Fields whose types include {@link UndefinedType} are optional; all other fields are required.
 */
public final class ObjectType implements Type {
	private final Map<String, Type> fields;
	private final Type additionalFieldType;

	/**
	 * Creates an open {@code ObjectType} with no statically known fields and the given undeclared field
	 * value type.
	 *
	 * @param valueType the type of undeclared present fields
	 * @return a new {@code ObjectType}
	 */
	public static ObjectType of(Type valueType) {
		return of(Map.of(), Objects.requireNonNull(valueType, "valueType"));
	}

	/**
	 * Creates an open {@code ObjectType} declaring the given fields and undeclared field type.
	 *
	 * @param fields declared field names and their types
	 * @param additionalFieldType the type of undeclared present fields
	 * @return a new {@code ObjectType} holding a snapshot of {@code fields}
	 */
	public static ObjectType of(Map<String, ? extends Type> fields, Type additionalFieldType) {
		return new ObjectType(fields, additionalFieldType);
	}

	/**
	 * Creates a closed {@code ObjectType} declaring the given fields.
	 * <p>
	 * A field is optional when its type includes {@link UndefinedType}; all other fields are
	 * required. A field whose type is exactly {@link UndefinedType} must be absent.
	 *
	 * @param fields declared field names and their types
	 * @return a new {@code ObjectType} holding a snapshot of {@code fields}
	 */
	public static ObjectType of(Map<String, ? extends Type> fields) {
		return of(fields, NeverType.getInstance());
	}

	/**
	 * Creates an empty closed {@code ObjectType}.
	 *
	 * @return a new empty closed {@code ObjectType}
	 */
	public static ObjectType of() {
		return of(Map.of());
	}

	public static ObjectType of(String k1, Type v1) {
		return of(Map.of(k1, v1));
	}

	public static ObjectType of(String k1, Type v1, String k2, Type v2) {
		return of(Map.of(k1, v1, k2, v2));
	}

	public static ObjectType of(String k1, Type v1, String k2, Type v2, String k3, Type v3) {
		return of(Map.of(k1, v1, k2, v2, k3, v3));
	}

	public static ObjectType of(
			String k1, Type v1,
			String k2, Type v2,
			String k3, Type v3,
			String k4, Type v4) {
		return of(Map.of(k1, v1, k2, v2, k3, v3, k4, v4));
	}

	public static ObjectType of(
			String k1, Type v1,
			String k2, Type v2,
			String k3, Type v3,
			String k4, Type v4,
			String k5, Type v5) {
		return of(Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5));
	}

	public static ObjectType of(
			String k1, Type v1,
			String k2, Type v2,
			String k3, Type v3,
			String k4, Type v4,
			String k5, Type v5,
			String k6, Type v6) {
		return of(Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6));
	}

	public static ObjectType of(
			String k1, Type v1,
			String k2, Type v2,
			String k3, Type v3,
			String k4, Type v4,
			String k5, Type v5,
			String k6, Type v6,
			String k7, Type v7) {
		return of(Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7));
	}

	public static ObjectType of(
			String k1, Type v1,
			String k2, Type v2,
			String k3, Type v3,
			String k4, Type v4,
			String k5, Type v5,
			String k6, Type v6,
			String k7, Type v7,
			String k8, Type v8) {
		return of(Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8));
	}

	public static ObjectType of(
			String k1, Type v1,
			String k2, Type v2,
			String k3, Type v3,
			String k4, Type v4,
			String k5, Type v5,
			String k6, Type v6,
			String k7, Type v7,
			String k8, Type v8,
			String k9, Type v9) {
		return of(Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9));
	}

	public static ObjectType of(
			String k1, Type v1,
			String k2, Type v2,
			String k3, Type v3,
			String k4, Type v4,
			String k5, Type v5,
			String k6, Type v6,
			String k7, Type v7,
			String k8, Type v8,
			String k9, Type v9,
			String k10, Type v10) {
		return of(Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10));
	}

	private ObjectType(Map<String, ? extends Type> fields, Type additionalFieldType) {
		Objects.requireNonNull(fields, "fields");
		Map<String, Type> copied = new TreeMap<>();
		for (Map.Entry<String, ? extends Type> field : fields.entrySet()) {
			String name = Objects.requireNonNull(field.getKey(), "fields must not contain null keys");
			Type type = Objects.requireNonNull(field.getValue(), "fields must not contain null values");
			copied.put(name, type);
		}
		this.fields = Collections.unmodifiableMap(copied);
		this.additionalFieldType = Objects.requireNonNull(additionalFieldType, "additionalFieldType");
	}

	/**
	 * Returns the declared fields, sorted by field name.
	 *
	 * @return the immutable field map
	 */
	public Map<String, Type> fields() {
		return fields;
	}

	/**
	 * Returns the type of undeclared present fields, or {@link NeverType} for a closed object.
	 */
	public Type additionalFieldType() {
		return additionalFieldType;
	}

	public boolean isClosed() {
		return additionalFieldType == NeverType.getInstance();
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		return obj instanceof ObjectType other && fields.equals(other.fields) && additionalFieldType.equals(other.additionalFieldType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(fields, additionalFieldType);
	}

	@Override
	public String toString() {
		return TypeNotation.print(this);
	}
}
