package net.thisptr.jackson.jq.v2.spi.type;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * An immutable type signature for a jq filter.
 * <p>
 * This is a signature, not a jq runtime value type. The input type describes the values the filter
 * accepts as its implicit {@code .} input, and the output type describes the possible values it may
 * emit. The output type does not describe how many values are emitted; output cardinality is modeled
 * separately by {@link net.thisptr.jackson.jq.v2.spi.ExpressionProperties} for function calls and by
 * the compiler's internal expression analysis for other expressions.
 * <p>
 * Any free {@link TypeVariable} occurring in the signature must be quantified by the enclosing
 * {@link TypeScheme}.
 */
public final class FilterType {
	private final Type inputType;
	private final Type outputType;

	private FilterType(Type inputType, Type outputType) {
		this.inputType = Objects.requireNonNull(inputType, "inputType");
		this.outputType = Objects.requireNonNull(outputType, "outputType");
	}

	/**
	 * Creates a filter type signature.
	 *
	 * @param inputType the type of the filter's implicit {@code .} input
	 * @param outputType the type of the possible values emitted by the filter
	 * @return the filter type signature
	 */
	public static FilterType of(Type inputType, Type outputType) {
		return new FilterType(inputType, outputType);
	}

	/**
	 * Parses a filter type written in the notation of this package, {@code input -> output}.
	 *
	 * @param text the string form to parse
	 * @return the parsed filter type
	 * @throws IllegalArgumentException if {@code text} does not match the expected syntax
	 * @throws NullPointerException if {@code text} is {@code null}
	 */
	public static FilterType valueOf(String text) {
		return TypeNotation.parseFilterType(Objects.requireNonNull(text, "text"));
	}

	/**
	 * Returns the type of the filter's implicit {@code .} input.
	 *
	 * @return the input type
	 */
	public Type inputType() {
		return inputType;
	}

	/**
	 * Returns the type of the possible values emitted by the filter.
	 *
	 * @return the output type
	 */
	public Type outputType() {
		return outputType;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		return obj instanceof FilterType other && inputType.equals(other.inputType) && outputType.equals(other.outputType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(inputType, outputType);
	}

	@Override
	public String toString() {
		return TypeNotation.print(this);
	}
}
