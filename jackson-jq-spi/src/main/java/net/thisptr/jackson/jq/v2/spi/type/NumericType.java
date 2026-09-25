package net.thisptr.jackson.jq.v2.spi.type;

import java.math.BigInteger;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * A number type, carrying what is known about the kind of number or its exact integral value.
 */
public final class NumericType implements Type {
	private static final NumericType UNKNOWN = new NumericType(NumberKind.UNKNOWN, null);
	private static final NumericType INT = new NumericType(NumberKind.INT, null);
	private static final NumericType FLOAT = new NumericType(NumberKind.FLOAT, null);

	private static final int CACHE_LOW = -1;
	private static final int CACHE_HIGH = 16;
	private static final NumericType[] CACHED = new NumericType[CACHE_HIGH - CACHE_LOW + 1];

	static {
		for (int i = CACHE_LOW; i <= CACHE_HIGH; i++) {
			CACHED[i - CACHE_LOW] = new NumericType(NumberKind.INT, BigInteger.valueOf(i));
		}
	}

	private final NumberKind numberKind;
	private final @Nullable BigInteger value;

	/**
	 * Returns the unqualified number type, saying nothing about the kind of number.
	 *
	 * @return the unqualified number type
	 */
	public static NumericType getInstance() {
		return UNKNOWN;
	}

	/**
	 * Returns the number type of the given kind.
	 *
	 * @param numberKind what is known about the number
	 * @return the number type, one instance per kind
	 * @throws NullPointerException if {@code numberKind} is {@code null}
	 */
	public static NumericType of(NumberKind numberKind) {
		Objects.requireNonNull(numberKind, "numberKind");
		return switch (numberKind) {
			case INT -> INT;
			case FLOAT -> FLOAT;
			case UNKNOWN -> UNKNOWN;
		};
	}

	/**
	 * Returns the type of one known integer value.
	 *
	 * @param value the integer value described by the type
	 * @return the literal integer type
	 */
	public static NumericType of(long value) {
		if (value >= CACHE_LOW && value <= CACHE_HIGH)
			return CACHED[(int) value - CACHE_LOW];
		return new NumericType(NumberKind.INT, BigInteger.valueOf(value));
	}

	/**
	 * Returns the type of one known integer value.
	 *
	 * @param value the integer value described by the type
	 * @return the literal integer type
	 * @throws NullPointerException if {@code value} is {@code null}
	 */
	public static NumericType of(BigInteger value) {
		Objects.requireNonNull(value, "value");
		if (value.compareTo(BigInteger.valueOf(CACHE_LOW)) >= 0 && value.compareTo(BigInteger.valueOf(CACHE_HIGH)) <= 0)
			return CACHED[value.intValue() - CACHE_LOW];
		return new NumericType(NumberKind.INT, value);
	}

	private NumericType(NumberKind numberKind, @Nullable BigInteger value) {
		this.numberKind = numberKind;
		this.value = value;
	}

	/**
	 * Returns what is known about the kind of number.
	 *
	 * @return the number kind, {@link NumberKind#UNKNOWN} when nothing more than "a number" is known
	 */
	public NumberKind numberKind() {
		return numberKind;
	}

	/**
	 * Returns the integer value this type describes.
	 *
	 * @return the integer value, or {@code null} when not a known integer
	 */
	public @Nullable BigInteger value() {
		return value;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		return obj instanceof NumericType other
				&& numberKind == other.numberKind
				&& Objects.equals(value, other.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(numberKind, value);
	}

	@Override
	public String toString() {
		return TypeNotation.print(this);
	}
}
