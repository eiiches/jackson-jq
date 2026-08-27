package net.thisptr.jackson.jq.v2.spi.annotations;

import java.lang.annotation.Target;

import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.VersionRange;

/**
 * An annotation-compatible mirror of {@link VersionRange}, needed because annotation attributes
 * cannot reference arbitrary objects such as {@code VersionRange} directly. Converted to a
 * {@code VersionRange} via {@link VersionRange#from(VersionRangeSpec)}. Intended only as a
 * nested annotation value (e.g. {@code @FunctionRegistration}'s {@code version} attribute), never
 * applied directly to a program element.
 * <p>
 * Annotation attributes cannot hold {@code null}, so an absent bound (matching
 * {@code VersionRange}'s {@code null}-bound "unbounded" semantics) is instead represented by a
 * {@link VersionSpec} whose {@code major}, {@code minor}, and {@code patch} are <em>all exactly</em>
 * {@code -1}. This is an unambiguous sentinel because {@link Version} itself never permits
 * negative components. Only this exact triple denotes "absent" &mdash; any other negative
 * component (e.g. a stray {@code -1} alongside non-negative components, or a different negative
 * value) is a malformed spec and causes {@link VersionRange#from(VersionRangeSpec)} to throw
 * {@link IllegalArgumentException}, rather than being silently treated as unbounded. The defaults
 * for both {@link #min()} and {@link #max()} use the sentinel, so the default range is genuinely
 * unbounded on both ends, i.e. "all versions".
 */
@Target({})
public @interface VersionRangeSpec {
	/**
	 * The lower bound, or the {@code -1, -1, -1} sentinel (see the class javadoc) if unbounded
	 * below. Defaults to unbounded.
	 *
	 * @return the lower bound
	 */
	VersionSpec min() default @VersionSpec(major = -1, minor = -1, patch = -1);

	/**
	 * Returns whether {@link #min()} itself is included in the range. Ignored if {@link #min()}
	 * is the unbounded sentinel.
	 *
	 * @return whether {@link #min()} itself is included in the range
	 */
	boolean minInclusive() default true;

	/**
	 * The upper bound, or the {@code -1, -1, -1} sentinel (see the class javadoc) if unbounded
	 * above. Defaults to unbounded.
	 *
	 * @return the upper bound
	 */
	VersionSpec max() default @VersionSpec(major = -1, minor = -1, patch = -1);

	/**
	 * Returns whether {@link #max()} itself is included in the range. Ignored if {@link #max()}
	 * is the unbounded sentinel.
	 *
	 * @return whether {@link #max()} itself is included in the range
	 */
	boolean maxInclusive() default false;
}
