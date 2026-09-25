package net.thisptr.jackson.jq.v2.spi.type;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * A string type, carrying the value when the string is known exactly.
 * <p>
 * A type carrying a value describes that one string and nothing else, which is what lets a
 * comparison against it be decided at compile time. Unlike {@link NumberKind}, the value is not a
 * hint: two types carrying different values describe disjoint sets of strings.
 */
public final class StringType implements Type {
	private static final StringType UNKNOWN = new StringType(null);

	private final @Nullable String value;

	/**
	 * Returns the unqualified string type, saying nothing about which string it describes.
	 *
	 * @return the unqualified string type
	 */
	public static StringType getInstance() {
		return UNKNOWN;
	}

	/**
	 * Returns the type of one known string.
	 *
	 * @param value the string described by the type
	 * @return the literal string type
	 * @throws NullPointerException if {@code value} is {@code null}
	 */
	public static StringType of(String value) {
		return new StringType(Objects.requireNonNull(value, "value"));
	}

	private StringType(@Nullable String value) {
		this.value = value;
	}

	/**
	 * Returns the string this type describes.
	 *
	 * @return the string, or {@code null} when nothing more than "a string" is known
	 */
	public @Nullable String value() {
		return value;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		return obj instanceof StringType other && Objects.equals(value, other.value);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(value);
	}

	@Override
	public String toString() {
		return TypeNotation.print(this);
	}
}
