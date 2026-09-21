package net.thisptr.jackson.jq.v2.spi.type;

import java.util.Objects;

/**
 * An immutable description of a JSON value type or the absence of a value.
 * <p>
 * Object types may be open or closed. A declared field is optional when its type includes
 * {@link UndefinedType}; all other declared fields are required. Array types describe their leading
 * positions the same way, with a type for every element past them. Union types use set semantics and
 * are normalized by their factory methods.
 * <p>
 * Custom implementations are not supported. Use the factory and {@code getInstance()} methods of the
 * specific type classes; the implementations they return are the only ones this interface permits,
 * so a {@code switch} over them is exhaustive.
 */
public sealed interface Type permits AnyType, ArrayType, BinaryType, BooleanType, NeverType, NullType, NumericType, ObjectType, RecursiveType, StringType, TypeVariable, UndefinedType, UnionType {
	/**
	 * Parses a type written in the notation of this package.
	 * <p>
	 * A name with no binder in scope parses as an unbounded {@link TypeVariable}, since only a binder
	 * can state an upper bound.
	 *
	 * @param text the string form to parse
	 * @return the parsed type
	 * @throws IllegalArgumentException if {@code text} does not match the expected syntax
	 * @throws NullPointerException if {@code text} is {@code null}
	 */
	static Type valueOf(String text) {
		return TypeNotation.parseType(Objects.requireNonNull(text, "text"));
	}
}
