package net.thisptr.jackson.jq.v2.spi.version;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.annotations.VersionSpec;

/**
 * Represents a semantic version of jq (e.g. {@code 1.6} or {@code 1.7.1}).
 * <p>
 * Instances are immutable and can be created using the static factory methods
 * {@link #of(int, int)}, {@link #of(int, int, int)}, or parsed from text via {@link #valueOf(String)}.
 */
public final class Version implements Comparable<Version> {
	private final int major;
	private final int minor;
	private final int patch;

	Version(int major, int minor, int patch) {
		if (major < 0 || minor < 0 || patch < 0)
			throw new IllegalArgumentException("Invalid version components (must be non-negative): " + major + "." + minor + "." + patch);
		this.major = major;
		this.minor = minor;
		this.patch = patch;
	}

	@Override
	public int compareTo(Version o) {
		@Var int r = Integer.compare(major, o.major);
		if (r != 0)
			return r;
		r = Integer.compare(minor, o.minor);
		if (r != 0)
			return r;
		return Integer.compare(patch, o.patch);
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (!(o instanceof Version))
			return false;
		Version version = (Version) o;
		return major == version.major && minor == version.minor && patch == version.patch;
	}

	@Override
	public int hashCode() {
		return Objects.hash(major, minor, patch);
	}

	/**
	 * Returns the major version component.
	 *
	 * @return the major version component
	 */
	public int major() {
		return major;
	}

	/**
	 * Returns the minor version component.
	 *
	 * @return the minor version component
	 */
	public int minor() {
		return minor;
	}

	/**
	 * Returns the patch version component.
	 *
	 * @return the patch version component
	 */
	public int patch() {
		return patch;
	}

	static final String INTEGER = "0|[1-9][0-9]*";
	static final String VERSION = "(?:" + INTEGER + ")\\.(?:" + INTEGER + ")(?:\\.(?:" + INTEGER + "))?";

	private static final Pattern VERSION_PATTERN = Pattern.compile("^(" + INTEGER + ")\\.(" + INTEGER + ")(?:\\.(" + INTEGER + "))?$");

	/**
	 * Creates a version from its components.
	 *
	 * @param major the major version component
	 * @param minor the minor version component
	 * @param patch the patch version component
	 * @return the version
	 * @throws IllegalArgumentException if any component is negative
	 */
	public static Version of(int major, int minor, int patch) {
		return new Version(major, minor, patch);
	}

	/**
	 * Creates a version from its major and minor components, with patch set to 0.
	 *
	 * @param major the major version component
	 * @param minor the minor version component
	 * @return the version
	 * @throws IllegalArgumentException if any component is negative
	 */
	public static Version of(int major, int minor) {
		return new Version(major, minor, 0);
	}

	/**
	 * Bridges the annotation-compatible {@link VersionSpec} form to a runtime {@code Version}.
	 *
	 * @param spec the annotation form to convert
	 * @return the equivalent version
	 */
	public static Version from(VersionSpec spec) {
		return new Version(spec.major(), spec.minor(), spec.patch());
	}

	/**
	 * Parses a version from its {@code major.minor[.patch]} string form (patch defaults to 0 if
	 * omitted).
	 *
	 * @param text the string form to parse
	 * @return the parsed version
	 * @throws IllegalArgumentException if {@code text} does not match the expected syntax
	 */
	public static Version valueOf(String text) {
		Matcher m = VERSION_PATTERN.matcher(text);
		if (!m.matches())
			throw new IllegalArgumentException("Invalid Version: " + text);

		String majorVersion = m.group(1);
		String minorVersion = m.group(2);
		String patchVersion = m.group(3);

		return new Version(Integer.parseInt(majorVersion), Integer.parseInt(minorVersion), patchVersion != null ? Integer.parseInt(patchVersion) : 0);
	}

	@Override
	public String toString() {
		return major + "." + minor + "." + patch;
	}
}
