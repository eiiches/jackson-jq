package net.thisptr.jackson.jq.v2.spi;

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
	public int hashCode() {
		int prime = 31;
		@Var int result = 1;
		result = prime * result + major;
		result = prime * result + minor;
		result = prime * result + patch;
		return result;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Version other = (Version) obj;
		if (major != other.major)
			return false;
		if (minor != other.minor)
			return false;
		if (patch != other.patch)
			return false;
		return true;
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

	public static Pattern VERSION_PATTERN = Pattern.compile("([0-9])\\.([0-9])(?:\\.([0-9]+))?");

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
