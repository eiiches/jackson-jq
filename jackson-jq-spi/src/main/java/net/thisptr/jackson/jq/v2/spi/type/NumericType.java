package net.thisptr.jackson.jq.v2.spi.type;

import java.util.Objects;

/**
 * A number type, carrying what is known about the kind of number.
 */
public final class NumericType implements Type {
	private static final NumericType UNKNOWN = new NumericType(NumberKind.UNKNOWN);
	private static final NumericType INT = new NumericType(NumberKind.INT);
	private static final NumericType FLOAT = new NumericType(NumberKind.FLOAT);

	private final NumberKind numberKind;

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

	private NumericType(NumberKind numberKind) {
		this.numberKind = numberKind;
	}

	/**
	 * Returns what is known about the kind of number.
	 *
	 * @return the number kind, {@link NumberKind#UNKNOWN} when nothing more than "a number" is known
	 */
	public NumberKind numberKind() {
		return numberKind;
	}

	@Override
	public String toString() {
		return TypeNotation.print(this);
	}
}
