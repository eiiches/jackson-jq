package net.thisptr.jackson.jq.v2.spi.type;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * A boolean type, carrying the value when the boolean is known exactly.
 * <p>
 * A type carrying a value describes that one boolean and nothing else, which is what lets a
 * condition be decided at compile time. The two of them together say no more than the unqualified
 * boolean type does, so {@code true | false} normalizes back to it.
 */
public final class BooleanType implements Type {
	private static final BooleanType UNKNOWN = new BooleanType(null);
	private static final BooleanType TRUE = new BooleanType(Boolean.TRUE);
	private static final BooleanType FALSE = new BooleanType(Boolean.FALSE);

	private final @Nullable Boolean value;

	/**
	 * Returns the unqualified boolean type, saying nothing about which boolean it describes.
	 *
	 * @return the unqualified boolean type
	 */
	public static BooleanType getInstance() {
		return UNKNOWN;
	}

	/**
	 * Returns the type of one known boolean.
	 *
	 * @param value the boolean described by the type
	 * @return the literal boolean type, one instance per value
	 */
	public static BooleanType of(boolean value) {
		return value ? TRUE : FALSE;
	}

	private BooleanType(@Nullable Boolean value) {
		this.value = value;
	}

	/**
	 * Returns the boolean this type describes.
	 *
	 * @return the boolean, or {@code null} when nothing more than "a boolean" is known
	 */
	public @Nullable Boolean value() {
		return value;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		return obj instanceof BooleanType other && Objects.equals(value, other.value);
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
