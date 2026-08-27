package net.thisptr.jackson.jq.v2.spi;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.annotations.VersionRangeSpec;

/**
 * An inclusive/exclusive range of {@link Version}s.
 * <p>
 * The canonical string form is {@code [min, max)} (see {@link #toString()} / {@link #valueOf(String)}):
 * a missing bound means "unbounded" on that side, e.g. {@code [1.6, )} means "1.6 and above" and
 * {@code (, 1.8)} means "below 1.8". Test-case YAML in this repository follows the same
 * {@code [inclusive, exclusive)} convention.
 */
public class VersionRange {
	private final @Nullable Version minVersion;
	private final boolean minInclusive;
	private final @Nullable Version maxVersion;
	private final boolean maxInclusive;

	/**
	 * Creates a version range.
	 * <p>
	 * A {@code null} bound means unbounded on that side; the corresponding inclusive flag is then
	 * ignored (treated as {@code false}).
	 *
	 * @param minVersion the lower bound, or {@code null} if unbounded
	 * @param minInclusive whether {@code minVersion} itself is included in the range
	 * @param maxVersion the upper bound, or {@code null} if unbounded
	 * @param maxInclusive whether {@code maxVersion} itself is included in the range
	 * @throws IllegalArgumentException if {@code minVersion} is greater than {@code maxVersion}, or
	 *                                   they are equal but not both inclusive
	 */
	public VersionRange(@Nullable Version minVersion, boolean minInclusive,
						@Nullable Version maxVersion, boolean maxInclusive) {
		this.minVersion = minVersion;
		this.minInclusive = minVersion != null && minInclusive;
		this.maxVersion = maxVersion;
		this.maxInclusive = maxVersion != null && maxInclusive;

		if (this.minVersion != null && this.maxVersion != null) {
			int r = this.minVersion.compareTo(this.maxVersion);
			if (r > 0 || (r == 0 && (!this.minInclusive || !this.maxInclusive)))
				throw new IllegalArgumentException("Invalid VersionRange (min must be less than or equal to max): " + (this.minInclusive ? "[" : "(") + this.minVersion + ", " + this.maxVersion + (this.maxInclusive ? "]" : ")"));
		}
	}

	/**
	 * Returns whether the given version falls within this range.
	 *
	 * @param version the version to test
	 * @return {@code true} if {@code version} is within this range
	 */
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

	/**
	 * Parses a version range from its canonical string form, e.g. {@code [1.6, 1.8)} or
	 * {@code [1.6, )}. Either bound may be omitted to denote "unbounded".
	 *
	 * @param text the string form to parse
	 * @return the parsed version range
	 * @throws IllegalArgumentException if {@code text} does not match the expected syntax
	 */
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

	/**
	 * Bridges the annotation-compatible {@link VersionRangeSpec} form (as used in
	 * {@code @FunctionRegistration}) to a runtime {@code VersionRange}.
	 *
	 * @param spec the annotation form to convert
	 * @return the equivalent version range
	 */
	public static VersionRange valueOf(VersionRangeSpec spec) {
		return new VersionRange(Version.valueOf(spec.min()), spec.minInclusive(),
				Version.valueOf(spec.max()), spec.maxInclusive());
	}

	/**
	 * Returns the canonical string form of this range, e.g. {@code [1.6, 1.8)}.
	 *
	 * @return the string form
	 */
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
