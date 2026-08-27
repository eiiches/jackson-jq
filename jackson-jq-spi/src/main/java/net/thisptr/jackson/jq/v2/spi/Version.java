package net.thisptr.jackson.jq.v2.spi;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.annotations.VersionSpec;

/**
 * Use {@code Versions} to get a {@link Version} instance.
 */
public class Version implements Comparable<Version> {
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

	public int major() {
		return major;
	}

	public int minor() {
		return minor;
	}

	public int patch() {
		return patch;
	}

	static final String INTEGER = "0|[1-9][0-9]*";
	static final String VERSION = "(?:" + INTEGER + ")\\.(?:" + INTEGER + ")(?:\\.(?:" + INTEGER + "))?";

	private static final Pattern VERSION_PATTERN = Pattern.compile("^(" + INTEGER + ")\\.(" + INTEGER + ")(?:\\.(" + INTEGER + "))?$");

	public static Version valueOf(int major, int minor, int patch) {
		return new Version(major, minor, patch);
	}

	public static Version valueOf(VersionSpec spec) {
		return new Version(spec.major(), spec.minor(), spec.patch());
	}

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
