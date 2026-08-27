package net.thisptr.jackson.jq.v2.spi.annotations;

import net.thisptr.jackson.jq.v2.spi.VersionRange;

/**
 * An annotation-compatible mirror of {@link VersionRange}, needed because annotation attributes
 * cannot reference arbitrary objects such as {@code VersionRange} directly. Converted to a
 * {@code VersionRange} via {@link VersionRange#valueOf(VersionRangeSpec)}.
 * <p>
 * The defaults ({@code [0.0.0, Integer.MAX_VALUE.MAX_VALUE.MAX_VALUE)}) amount to an unbounded,
 * always-open range, i.e. "all versions".
 */
public @interface VersionRangeSpec {
	/**
	 * The lower bound. Defaults to the lowest possible version.
	 *
	 * @return the lower bound
	 */
	VersionSpec min() default @VersionSpec(major = 0, minor = 0, patch = 0);

	/**
	 * Returns whether {@link #min()} itself is included in the range.
	 *
	 * @return whether {@link #min()} itself is included in the range
	 */
	boolean minInclusive() default true;

	/**
	 * The upper bound. Defaults to the highest possible version.
	 *
	 * @return the upper bound
	 */
	VersionSpec max() default @VersionSpec(major = Integer.MAX_VALUE, minor = Integer.MAX_VALUE, patch = Integer.MAX_VALUE);

	/**
	 * Returns whether {@link #max()} itself is included in the range.
	 *
	 * @return whether {@link #max()} itself is included in the range
	 */
	boolean maxInclusive() default false;
}
