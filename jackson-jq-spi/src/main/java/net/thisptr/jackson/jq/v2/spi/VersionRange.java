package net.thisptr.jackson.jq.v2.spi;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.annotations.VersionRangeSpec;

public class VersionRange {
	private final @Nullable Version minVersion;
	private final boolean minInclusive;
	private final @Nullable Version maxVersion;
	private final boolean maxInclusive;

	public VersionRange(@Nullable Version minVersion, boolean minInclusive,
						@Nullable Version maxVersion, boolean maxInclusive) {
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
	public boolean equals(@Nullable Object o) {
		if (!(o instanceof VersionRange))
			return false;
		VersionRange that = (VersionRange) o;
		return minInclusive == that.minInclusive && maxInclusive == that.maxInclusive && Objects.equals(minVersion, that.minVersion) && Objects.equals(maxVersion, that.maxVersion);
	}

	@Override
	public int hashCode() {
		return Objects.hash(minVersion, minInclusive, maxVersion, maxInclusive);
	}

	private static final Pattern VERSION_RANGE_PATTERN = Pattern.compile("^([\\[(])\\s*(" + Version.VERSION + ")?\\s*,\\s*(" + Version.VERSION + ")?\\s*([)\\]])$");

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

	public static VersionRange valueOf(VersionRangeSpec spec) {
		return new VersionRange(Version.valueOf(spec.min()), spec.minInclusive(),
				Version.valueOf(spec.max()), spec.maxInclusive());
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
