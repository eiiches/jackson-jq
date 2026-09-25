package net.thisptr.jackson.jq.v2.spi.type;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * An immutable type signature for a jq function.
 * <p>
 * This is a signature, not a jq runtime value type. The return type describes the filter produced
 * by the function, including the implicit {@code .} input it accepts and the possible values it may
 * emit. Call cardinality is modeled separately by {@link net.thisptr.jackson.jq.v2.spi.ExpressionProperties}.
 * The parameter types describe its filter-valued jq arguments, in declaration order.
 * <p>
 * Any free {@link TypeVariable} occurring in the signature, including within the {@link #returnType()}
 * or a parameter's {@link FilterType}, must be quantified by the enclosing {@link TypeScheme}.
 * <p>
 * Its string representation uses {@code (parameters) => (input -> output)}. The double arrow
 * reflects that a jq function takes filter parameters and produces a filter, while the single
 * arrow describes the input-to-output relationship of that filter. Each parameter is itself
 * written as a filter type using a single arrow, and the parameters are separated by {@code ;},
 * as they are at a jq call site.
 */
public final class FunctionType {
	private final FilterType returnType;
	private final List<FilterType> parameterTypes;

	private FunctionType(FilterType returnType, List<FilterType> parameterTypes) {
		this.returnType = Objects.requireNonNull(returnType, "returnType");
		Objects.requireNonNull(parameterTypes, "parameterTypes");
		this.parameterTypes = parameterTypes;
	}

	/**
	 * Creates a function type signature.
	 *
	 * @param returnType the filter type produced by the function
	 * @param parameterTypes the type signatures of the filter-valued jq arguments, in declaration order
	 * @return the function type signature
	 */
	public static FunctionType of(FilterType returnType, FilterType... parameterTypes) {
		return new FunctionType(returnType, List.of(parameterTypes));
	}

	/**
	 * Creates a function type signature.
	 *
	 * @param returnType the filter type produced by the function
	 * @param parameterTypes the type signatures of the filter-valued jq arguments, in declaration order
	 * @return the function type signature
	 */
	public static FunctionType of(FilterType returnType, List<FilterType> parameterTypes) {
		return new FunctionType(returnType, new ArrayList<>(parameterTypes));
	}

	/**
	 * Creates a function type signature.
	 *
	 * @param inputType the input type of the filter produced by the function
	 * @param outputType the output type of the filter produced by the function
	 * @param parameterTypes the type signatures of the filter-valued jq arguments, in declaration order
	 * @return the function type signature
	 */
	public static FunctionType of(Type inputType, Type outputType, FilterType... parameterTypes) {
		return new FunctionType(FilterType.of(inputType, outputType), List.of(parameterTypes));
	}

	/**
	 * Creates a function type signature.
	 *
	 * @param inputType the input type of the filter produced by the function
	 * @param outputType the output type of the filter produced by the function
	 * @param parameterTypes the type signatures of the filter-valued jq arguments, in declaration order
	 * @return the function type signature
	 */
	public static FunctionType of(Type inputType, Type outputType, List<FilterType> parameterTypes) {
		return new FunctionType(FilterType.of(inputType, outputType), new ArrayList<>(parameterTypes));
	}

	/**
	 * Parses a function type written in the notation of this package,
	 * {@code (parameters) => (input -> output)}.
	 *
	 * @param text the string form to parse
	 * @return the parsed function type
	 * @throws IllegalArgumentException if {@code text} does not match the expected syntax
	 * @throws NullPointerException if {@code text} is {@code null}
	 */
	public static FunctionType valueOf(String text) {
		return TypeNotation.parseFunctionType(Objects.requireNonNull(text, "text"));
	}

	/**
	 * Returns the filter type produced by the function.
	 *
	 * @return the return filter type
	 */
	public FilterType returnType() {
		return returnType;
	}

	/**
	 * Returns the type signatures of the function's filter-valued jq arguments, in declaration order.
	 *
	 * @return the filter-parameter type signatures
	 */
	public List<FilterType> parameterTypes() {
		return parameterTypes;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		return obj instanceof FunctionType other && returnType.equals(other.returnType)
				&& parameterTypes.equals(other.parameterTypes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(returnType, parameterTypes);
	}

	@Override
	public String toString() {
		return TypeNotation.print(this);
	}
}
