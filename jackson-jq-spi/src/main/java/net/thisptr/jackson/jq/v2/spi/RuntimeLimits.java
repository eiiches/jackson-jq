package net.thisptr.jackson.jq.v2.spi;

/**
 * Per-invocation budgets that bound how much memory a running query may consume.
 * <p>
 * Limits are read from the {@link RuntimeContext} an {@link Expression} is evaluated with, so a
 * single compiled query can be run concurrently under different limits. Every limit is unbounded
 * unless the caller sets one, which is how jq itself behaves.
 * <p>
 * Implementations must be immutable and safe to share across threads.
 */
public interface RuntimeLimits {

	/**
	 * Returns the largest number of elements an array produced during evaluation may have.
	 *
	 * @return the maximum array length, or {@link Integer#MAX_VALUE} for no limit
	 */
	int getMaxArrayLength();

	/**
	 * Returns the largest number of members an object produced during evaluation may have.
	 *
	 * @return the maximum object member count, or {@link Integer#MAX_VALUE} for no limit
	 */
	int getMaxObjectMemberCount();

	/**
	 * Returns the largest number of characters a string produced during evaluation may have.
	 * <p>
	 * Length is counted in UTF-16 code units -- what {@link String#length()} reports -- because that
	 * is what a string actually costs on the heap and the only measure cheap enough to test before
	 * the string is built. Note this differs from jq's {@code length}, which counts Unicode
	 * codepoints: a string of astral characters such as {@code "😀"} has a {@code length}
	 * of 1 but a size of 2 here, so the limit can be up to twice as strict as {@code length}
	 * suggests. For text in the Basic Multilingual Plane the two agree.
	 * <p>
	 * Operations that can grow a string without bound -- {@code +}, {@code *}, string interpolation,
	 * {@code join}, and {@code sub}/{@code gsub} -- are rejected before they allocate. The
	 * formatters whose output is a bounded multiple of their input ({@code @uri}, {@code @csv},
	 * {@code tojson}, and the like) build their result first and reject it afterwards, so no string
	 * over the limit is ever returned, but such a string may briefly exist.
	 *
	 * @return the maximum string length in UTF-16 code units, or {@link Integer#MAX_VALUE} for no limit
	 */
	int getMaxStringLength();
}
