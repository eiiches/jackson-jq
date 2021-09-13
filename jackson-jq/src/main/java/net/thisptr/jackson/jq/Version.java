package net.thisptr.jackson.jq;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Use {@link net.thisptr.jackson.jq.Versions} to get a {@link Version} instance.
 */
public class Version implements Comparable<Version> {
	public final int major;
	public final int minor;

	public static final Version LATEST = new Version(Integer.MAX_VALUE, Integer.MAX_VALUE);

	Version(final int major, final int minor) {
		this.major = major;
		this.minor = minor;
	}

	@Override
	public int compareTo(final Version o) {
		final int r = Integer.compare(major, o.major);
		if (r != 0)
			return r;
		return Integer.compare(minor, o.minor);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + major;
		result = prime * result + minor;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
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
		return true;
	}

	public int majorVersion() {
		return major;
	}

	public int minorVersion() {
		return minor;
	}

	public static Pattern VERSION_PATTERN = Pattern.compile("([0-9])\\.([0-9])");

	public static Version valueOf(final String text) {
		final Matcher m = VERSION_PATTERN.matcher(text);
		if (!m.matches())
			throw new IllegalArgumentException("Invalid Version: " + text);

		final String majorVersion = m.group(1);
		final String minorVersion = m.group(2);

		return new Version(Integer.parseInt(majorVersion), Integer.parseInt(minorVersion));
	}

	@Override
	public String toString() {
		return major + "." + minor;
	}
}
