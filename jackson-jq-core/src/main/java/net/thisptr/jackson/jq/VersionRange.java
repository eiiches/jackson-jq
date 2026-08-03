package net.thisptr.jackson.jq;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionRange {
	private final Version minVersion;
	private final boolean minInclusive;
	private final Version maxVersion;
	private final boolean maxInclusive;

	public VersionRange(final Version minVersion, final boolean minInclusive,
			final Version maxVersion, final boolean maxInclusive) {
		this.minVersion = minVersion;
		this.minInclusive = minInclusive;
		this.maxVersion = maxVersion;
		this.maxInclusive = maxInclusive;
	}

	public boolean contains(final Version version) {
		if (minVersion != null) {
			final int r = version.compareTo(minVersion);
			if (r < 0 || (!minInclusive && r == 0))
				return false;
		}
		if (maxVersion != null) {
			final int r = maxVersion.compareTo(version);
			if (r < 0 || (!maxInclusive && r == 0))
				return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (maxInclusive ? 1231 : 1237);
		result = prime * result + ((maxVersion == null) ? 0 : maxVersion.hashCode());
		result = prime * result + (minInclusive ? 1231 : 1237);
		result = prime * result + ((minVersion == null) ? 0 : minVersion.hashCode());
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
		VersionRange other = (VersionRange) obj;
		if (maxInclusive != other.maxInclusive)
			return false;
		if (maxVersion == null) {
			if (other.maxVersion != null)
				return false;
		} else if (!maxVersion.equals(other.maxVersion))
			return false;
		if (minInclusive != other.minInclusive)
			return false;
		if (minVersion == null) {
			if (other.minVersion != null)
				return false;
		} else if (!minVersion.equals(other.minVersion))
			return false;
		return true;
	}

	public static Pattern VERSION_RANGE_PATTERN = Pattern.compile("([\\[\\(])\\s*([0-9]\\.[0-9])?\\s*,\\s*([0-9]\\.[0-9])?([\\]\\)])");

	public static VersionRange valueOf(final String text) {
		final Matcher m = VERSION_RANGE_PATTERN.matcher(text);
		if (!m.matches())
			throw new IllegalArgumentException("Invalid VersionRange: " + text);

		final String minInclusive = m.group(1);
		final String minVersion = m.group(2);
		final String maxVersion = m.group(3);
		final String maxInclusive = m.group(4);

		return new VersionRange(minVersion != null && !minVersion.isEmpty() ? Version.valueOf(minVersion) : null,
				"[".equals(minInclusive),
				maxVersion != null && !maxVersion.isEmpty() ? Version.valueOf(maxVersion) : null,
				"]".equals(maxInclusive));
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append(minInclusive ? "[" : "(");
		builder.append(minVersion != null ? minVersion : "");
		builder.append(",");
		builder.append(maxVersion != null ? maxVersion : "");
		builder.append(maxInclusive ? "]" : ")");
		return builder.toString();
	}
}
