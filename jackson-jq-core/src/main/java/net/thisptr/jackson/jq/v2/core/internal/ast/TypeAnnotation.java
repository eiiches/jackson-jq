package net.thisptr.jackson.jq.v2.core.internal.ast;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;

/**
 * The {@code #jackson-jq:type} comment: the signature a {@code def} states for itself, instead of
 * leaving a caller with whatever its body happens to infer.
 * <p>
 * jq reads the line as an ordinary comment, so an annotated query still runs there -- it just says
 * nothing about types.
 */
public final class TypeAnnotation {
	/**
	 * What a comment must open with to be read as a declaration rather than skipped as a comment. The
	 * grammar matches it ahead of {@code <COMMENT>}, and matches the whole prefix rather than the
	 * prefix followed by a space: a comment that opens with it and then says something this cannot
	 * read is a mistake worth reporting, not a comment to pass over.
	 */
	public static final String MARKER = "#jackson-jq:type";

	private TypeAnnotation() {
	}

	/**
	 * Reads the scheme one annotation states.
	 *
	 * @param comment the whole comment, marker and line terminator included
	 * @param parameterCount the number of parameters the annotated definition declares
	 * @param location where the comment is, to place a rejection
	 * @return the declared scheme
	 * @throws IllegalStateException if the text is not the notation of
	 * {@link net.thisptr.jackson.jq.v2.spi.type.TypeScheme}, or states a different number of
	 * parameters than the definition takes
	 */
	public static TypeScheme<FunctionType> parse(String comment, int parameterCount, SourceLocation location) {
		String text = comment.substring(MARKER.length()).trim();
		TypeScheme<FunctionType> scheme;
		try {
			scheme = TypeScheme.ofFunction(text);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException(MARKER + " at " + location + " is not a type scheme: " + e.getMessage(), e);
		}
		int declared = scheme.body().parameterTypes().size();
		if (declared != parameterCount)
			throw new IllegalStateException(MARKER + " at " + location + " states " + declared
					+ " parameter" + (declared == 1 ? "" : "s")
					+ ", but the definition it annotates takes " + parameterCount + ".");
		return scheme;
	}
}
