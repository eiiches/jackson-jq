package net.thisptr.jackson.jq.v2.test.testcase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.version.Version;
import net.thisptr.jackson.jq.v2.spi.version.VersionRange;
import net.thisptr.jackson.jq.v2.test.comparator.FloatTolerance;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestCase {
	public enum OperatingSystem {
		MACOS,
		LINUX;

		public static OperatingSystem fromSystemProperty(String osName) {
			if (osName.startsWith("Mac"))
				return MACOS;
			if (osName.startsWith("Linux"))
				return LINUX;
			throw new IllegalArgumentException("unsupported operating system: " + osName);
		}
	}

	public enum Architecture {
		AARCH64,
		AMD64;

		public static Architecture fromSystemProperty(String osArch) {
			if (osArch.equals("aarch64") || osArch.equals("arm64"))
				return AARCH64;
			if (osArch.equals("amd64") || osArch.equals("x86_64"))
				return AMD64;
			throw new IllegalArgumentException("unsupported architecture: " + osArch);
		}
	}

	@JsonProperty("q")
	public String q = "";

	@JsonProperty("in")
	public JsonNode in = NullNode.getInstance();

	@JsonProperty("out")
	public List<JsonNode> out = Collections.emptyList();

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Expectation {
		@JsonProperty("v")
		@JsonDeserialize(using = VersionRangeDeserializer.class)
		@JsonSerialize(using = ToStringSerializer.class)
		public @Nullable VersionRange version;

		@JsonProperty("out")
		public @Nullable List<JsonNode> out;

		@JsonIgnore
		public List<JsonNode> values() {
			return Objects.requireNonNull(out, "expectation row requires out");
		}

		@JsonProperty("error")
		public boolean error;

		@JsonProperty("timeout")
		@JsonInclude(JsonInclude.Include.NON_DEFAULT)
		public boolean timeout;

		@JsonProperty("os")
		public @Nullable OperatingSystem os;

		@JsonProperty("arch")
		public @Nullable Architecture arch;

		public boolean contains(Version version) {
			return Objects.requireNonNull(this.version, "expectation row requires v").contains(version);
		}

		public boolean matchesPlatform(@Nullable OperatingSystem os, @Nullable Architecture arch) {
			return (this.os == null || this.os.equals(os)) && (this.arch == null || this.arch.equals(arch));
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Expectations {
		@JsonProperty("default")
		public List<Expectation> defaultRows = Collections.emptyList();

		@JsonProperty("overrides")
		public List<Expectation> overrides = Collections.emptyList();

		@JsonProperty("jjq")
		public List<Expectation> jjq = Collections.emptyList();

		public Expectation resolve(Version version, boolean realJq, String osName, String osArch) {
			Expectation base = find(defaultRows, version);
			if (base == null) {
				Expectation unsupported = new Expectation();
				unsupported.out = Collections.emptyList();
				unsupported.error = true;
				return unsupported;
			}
			if (!realJq) {
				Expectation jjqRow = find(jjq, version);
				return jjqRow != null ? jjqRow : base;
			}
			@Var @Nullable OperatingSystem os;
			try {
				os = OperatingSystem.fromSystemProperty(osName);
			} catch (IllegalArgumentException unsupportedOs) {
				os = null;
			}
			@Var @Nullable Architecture arch;
			try {
				arch = Architecture.fromSystemProperty(osArch);
			} catch (IllegalArgumentException unsupportedArch) {
				arch = null;
			}
			Expectation override = findOverride(version, os, arch);
			return override != null ? override : base;
		}

		private @Nullable Expectation findOverride(Version version, @Nullable OperatingSystem os, @Nullable Architecture arch) {
			for (Expectation row : overrides) {
				if (row.contains(version) && row.matchesPlatform(os, arch))
					return row;
			}
			return null;
		}

		private static @Nullable Expectation find(List<Expectation> rows, Version version) {
			for (Expectation row : rows) {
				if (row.contains(version))
					return row;
			}
			return null;
		}

		public boolean hasDefault(Version version) {
			return find(defaultRows, version) != null;
		}

		public void validate() {
			validateDefaultExpectations();
			validateOverrideExpectations();
			validateJjqExpectations();
			for (Expectation row : concat(overrides, jjq)) {
				VersionRange range = Objects.requireNonNull(row.version);
				if (!coveredByDefault(range))
					throw new IllegalArgumentException("expectation override range is not covered by default: " + row.version);
				for (Expectation base : defaultRows) {
					if (overlaps(range, Objects.requireNonNull(base.version)) && row.timeout == base.timeout && row.error == base.error && Objects.equals(row.out, base.out))
						throw new IllegalArgumentException("expectation override equals default over " + base.version);
				}
			}
		}

		private boolean coveredByDefault(VersionRange range) {
			// Check boundaries as well as configured versions, so a gap between releases is rejected.
			List<VersionRange> ranges = new ArrayList<>();
			for (Expectation row : defaultRows)
				ranges.add(Objects.requireNonNull(row.version));
			ranges.sort((a, b) -> compareLower(a, b));
			@Var VersionRange remainder = range;
			for (VersionRange candidate : ranges) {
				if (endsBefore(candidate, remainder))
					continue;
				if (compareLower(candidate, remainder) > 0)
					return false;
				if (compareUpper(candidate, remainder) >= 0)
					return true;
				remainder = VersionRange.of(candidate.maxVersion(), !candidate.maxInclusive(), remainder.maxVersion(), remainder.maxInclusive());
			}
			return false;
		}

		private static List<Expectation> concat(List<Expectation> first, List<Expectation> second) {
			List<Expectation> result = new ArrayList<>(first);
			result.addAll(second);
			return result;
		}

		private void validateDefaultExpectations() {
			if (defaultRows.isEmpty())
				throw new IllegalArgumentException("expectations.default must contain at least one row");
			for (Expectation row : defaultRows)
				validatePlainExpectation(row, "default");
			validateDisjointRanges(defaultRows, "default");
		}

		private void validateOverrideExpectations() {
			for (Expectation row : overrides) {
				if (row.version == null)
					throw new IllegalArgumentException("expectations.overrides row requires v");
				if (row.os == null && row.arch == null)
					throw new IllegalArgumentException("expectations.overrides row requires os or arch");
				if (row.timeout) {
					if (row.out != null || row.error)
						throw new IllegalArgumentException("expectations.overrides timeout row cannot specify out or error");
				} else if (row.out == null) {
					throw new IllegalArgumentException("expectations.overrides row requires out");
				}
			}
			for (OperatingSystem os : OperatingSystem.values()) {
				for (Architecture arch : Architecture.values()) {
					for (int i = 0; i < overrides.size(); i++) {
						Expectation row = overrides.get(i);
						if (!row.matchesPlatform(os, arch))
							continue;
						for (int j = 0; j < i; j++) {
							Expectation previous = overrides.get(j);
							if (previous.matchesPlatform(os, arch) && overlaps(Objects.requireNonNull(row.version), Objects.requireNonNull(previous.version)))
								throw new IllegalArgumentException("overlapping expectations.overrides ranges for " + os + "/" + arch);
						}
					}
				}
			}
		}

		private void validateJjqExpectations() {
			for (Expectation row : jjq)
				validatePlainExpectation(row, "jjq");
			validateDisjointRanges(jjq, "jjq");
		}

		private static void validatePlainExpectation(Expectation row, String name) {
			if (row.version == null)
				throw new IllegalArgumentException("expectations." + name + " row requires v");
			if (row.os != null || row.arch != null || row.timeout)
				throw new IllegalArgumentException("expectations." + name + " row cannot specify os, arch, or timeout");
			if (row.out == null)
				throw new IllegalArgumentException("expectations." + name + " row requires out");
		}

		private static void validateDisjointRanges(List<Expectation> rows, String name) {
			for (int i = 0; i < rows.size(); i++) {
				VersionRange range = Objects.requireNonNull(rows.get(i).version);
				for (int j = 0; j < i; j++) {
					if (overlaps(range, Objects.requireNonNull(rows.get(j).version)))
						throw new IllegalArgumentException("overlapping expectations." + name + " ranges");
				}
			}
		}

		private static boolean endsBefore(VersionRange a, VersionRange b) {
			if (a.maxVersion() == null || b.minVersion() == null)
				return false;
			int result = a.maxVersion().compareTo(b.minVersion());
			return result < 0 || (result == 0 && (!a.maxInclusive() || !b.minInclusive()));
		}

		private static boolean overlaps(VersionRange a, VersionRange b) {
			return compareLower(a, b) <= 0 ? !endsBefore(a, b) : !endsBefore(b, a);
		}

		private static int compareLower(VersionRange a, VersionRange b) {
			if (a.minVersion() == null || b.minVersion() == null)
				return a.minVersion() == b.minVersion() ? 0 : a.minVersion() == null ? -1 : 1;
			int result = a.minVersion().compareTo(b.minVersion());
			return result != 0 ? result : Boolean.compare(b.minInclusive(), a.minInclusive());
		}

		private static int compareUpper(VersionRange a, VersionRange b) {
			if (a.maxVersion() == null || b.maxVersion() == null)
				return a.maxVersion() == b.maxVersion() ? 0 : a.maxVersion() == null ? 1 : -1;
			int result = a.maxVersion().compareTo(b.maxVersion());
			return result != 0 ? result : Boolean.compare(a.maxInclusive(), b.maxInclusive());
		}
	}

	@JsonProperty("expectations")
	public @Nullable Expectations expectations;

	public static class TypeAssertion {
		@JsonProperty("input")
		public String input = "";

		@JsonProperty("output")
		public String output = "";

		public TypeAssertion() {
		}

		public TypeAssertion(String input, String output) {
			this.input = input;
			this.output = output;
		}

		@Override
		public String toString() {
			return String.format("{input: '%s', output: '%s'}", input, output);
		}
	}

	@JsonProperty("types")
	public List<TypeAssertion> types = Collections.emptyList();

	public static class PropertyAssertion {
		@JsonProperty("cardinality")
		public Cardinality cardinality = Cardinality.UNKNOWN;

		@JsonProperty("depends_on_input")
		public boolean dependsOnInput = true;

		@JsonProperty("depends_on_external_state")
		public boolean dependsOnExternalState = true;

		public PropertyAssertion() {
		}

		public PropertyAssertion(Cardinality cardinality, boolean dependsOnInput, boolean dependsOnExternalState) {
			this.cardinality = cardinality;
			this.dependsOnInput = dependsOnInput;
			this.dependsOnExternalState = dependsOnExternalState;
		}

		@Override
		public String toString() {
			return String.format("{cardinality: %s, depends_on_input: %s, depends_on_external_state: %s}",
					cardinality, dependsOnInput, dependsOnExternalState);
		}
	}

	@JsonProperty("properties")
	public @Nullable PropertyAssertion properties;

	@JsonProperty("file")
	public String file = "";

	@JsonProperty("failing")
	public @Nullable Boolean failing;

	@JsonProperty("should_compile")
	public boolean shouldCompile = true;

	@JsonProperty("float_tolerance")
	public @Nullable FloatTolerance floatTolerance;

	/**
	 * jq modules this test case needs on the module search path, keyed by path relative to the
	 * search root (e.g. {@code "a.jq"}, {@code "lib/jq/e/e.jq"}), value is the raw file content.
	 * The test harnesses materialize these files on a module search path before evaluating the test
	 * case.
	 */
	@JsonProperty("modules")
	public Map<String, String> modules = Collections.emptyMap();

	/**
	 * The jq versions the case applies to, or {@code null} for all of them. More than one range says a
	 * version in between behaves differently, which is how a release that is wrong on its own gets left
	 * out without giving up the ones either side of it.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("v")
	@JsonDeserialize(contentUsing = VersionRangeDeserializer.class)
	@JsonSerialize(contentUsing = ToStringSerializer.class)
	public @Nullable List<VersionRange> version;

	/**
	 * Whether this case says anything about {@code jqVersion}.
	 */
	public boolean appliesTo(Version jqVersion) {
		return contains(version, jqVersion);
	}

	public boolean appliesToAssertions(Version jqVersion) {
		return expectations != null ? expectations.hasDefault(jqVersion) : appliesTo(jqVersion);
	}

	public boolean hasAssertionVersionSelection() {
		return expectations != null || version != null;
	}

	private static boolean contains(@Nullable List<VersionRange> ranges, Version jqVersion) {
		if (ranges == null)
			return true;
		for (VersionRange range : ranges) {
			if (range.contains(jqVersion))
				return true;
		}
		return false;
	}

	@JsonProperty("comment")
	public @Nullable String comment;

	@Override
	public String toString() {
		return String.format("jq '%s' <<< '%s' # should be %s, version = %s.", q, in, out, version != null ? version : "any");
	}
}
