package net.thisptr.jackson.jq.v2.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.spi.Version;

public class VersionRange {
	private final Version minVersion;
	private final boolean minInclusive;
	private final Version maxVersion;
	private final boolean maxInclusive;

	public VersionRange(Version minVersion, boolean minInclusive,
						Version maxVersion, boolean maxInclusive) {
		this.minVersion = minVersion;
		this.minInclusive = minInclusive;
		this.maxVersion = maxVersion;
		this.maxInclusive = maxInclusive;
	}

	public boolean contains(Version version) {
		if (minVersion != null) {
			int r = version.compareTo(minVersion);
			if (r < 0 || (!minInclusive && r == 0))
				return false;
		}
		if (maxVersion != null) {
			int r = maxVersion.compareTo(version);
			if (r < 0 || (!maxInclusive && r == 0))
				return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int prime = 31;
		@Var int result = 1;
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

	public static VersionRange valueOf(String text) {
		Matcher m = VERSION_RANGE_PATTERN.matcher(text);
		if (!m.matches())
			throw new IllegalArgumentException("Invalid VersionRange: " + text);

		String minInclusive = m.group(1);
		String minVersion = m.group(2);
		String maxVersion = m.group(3);
		String maxInclusive = m.group(4);

		return new VersionRange(minVersion != null && !minVersion.isEmpty() ? Version.valueOf(minVersion) : null,
				"[".equals(minInclusive),
				maxVersion != null && !maxVersion.isEmpty() ? Version.valueOf(maxVersion) : null,
				"]".equals(maxInclusive));
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(minInclusive ? "[" : "(");
		builder.append(minVersion != null ? minVersion : "");
		builder.append(",");
		builder.append(maxVersion != null ? maxVersion : "");
		builder.append(maxInclusive ? "]" : ")");
		return builder.toString();
	}
}
