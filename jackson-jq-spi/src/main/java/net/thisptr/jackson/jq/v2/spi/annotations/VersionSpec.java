package net.thisptr.jackson.jq.v2.spi.annotations;

import java.lang.annotation.Target;

import net.thisptr.jackson.jq.v2.spi.Version;

/**
 * An annotation-compatible mirror of {@link Version}, needed because annotation attributes cannot
 * reference arbitrary objects such as {@code Version} directly. Intended only as a nested
 * annotation value (e.g. on {@link VersionRangeSpec#min()}/{@link VersionRangeSpec#max()}), never
 * applied directly to a program element.
 */
@Target({})
public @interface VersionSpec {
	/**
	 * Returns the major version component.
	 *
	 * @return the major version component
	 */
	int major();

	/**
	 * Returns the minor version component.
	 *
	 * @return the minor version component
	 */
	int minor();

	/**
	 * Returns the patch version component.
	 *
	 * @return the patch version component
	 */
	int patch();
}
