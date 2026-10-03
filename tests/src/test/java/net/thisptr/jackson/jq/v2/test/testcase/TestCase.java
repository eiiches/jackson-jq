package net.thisptr.jackson.jq.v2.test.testcase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
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
@JsonIgnoreProperties(ignoreUnknown = false)
public class TestCase {
	public enum IncompatibilityType {
		INTENTIONAL,
		JACKSON_JQ_BUG,
		UNCLASSIFIED
	}

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

	@JsonProperty("input")
	public JsonNode input = NullNode.getInstance();

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = false)
	public abstract static class AbstractExpectation {
		@JsonProperty("v")
		@JsonDeserialize(using = VersionRangeDeserializer.class)
		@JsonSerialize(using = ToStringSerializer.class)
		public final VersionRange version;

		protected AbstractExpectation(VersionRange version) {
			this.version = Objects.requireNonNull(version, "expectation row requires v");
		}

		@JsonProperty("output")
		public @Nullable List<JsonNode> output;

		@JsonIgnore
		public List<JsonNode> values() {
			return Objects.requireNonNull(output, "expectation row requires output");
		}

		@JsonProperty("runtime_error")
		public @Nullable String runtimeError;

		@JsonProperty("compile_error")
		public @Nullable String compileError;

		public boolean contains(Version version) {
			return this.version.contains(version);
		}

		public boolean timedOut() {
			return false;
		}

		public boolean unstable() {
			return false;
		}

		public boolean limitExceeded() {
			return false;
		}
	}

	public abstract static class AbstractJqExpectation extends AbstractExpectation {
		@JsonProperty("timeout")
		@JsonInclude(JsonInclude.Include.NON_DEFAULT)
		public boolean timeout;

		protected AbstractJqExpectation(VersionRange version) {
			super(version);
		}

		@Override
		public boolean timedOut() {
			return timeout;
		}
	}

	public static class DefaultExpectation extends AbstractJqExpectation {
		@JsonProperty("unstable")
		@JsonInclude(JsonInclude.Include.NON_DEFAULT)
		public boolean skip;

		@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
		public DefaultExpectation(@JsonProperty(value = "v", required = true) @JsonDeserialize(using = VersionRangeDeserializer.class) VersionRange version) {
			super(version);
		}

		@Override
		public boolean unstable() {
			return skip;
		}
	}

	public static class OverrideExpectation extends AbstractJqExpectation {
		@JsonProperty("os")
		public @Nullable OperatingSystem os;

		@JsonProperty("arch")
		public @Nullable Architecture arch;

		@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
		public OverrideExpectation(@JsonProperty(value = "v", required = true) @JsonDeserialize(using = VersionRangeDeserializer.class) VersionRange version) {
			super(version);
		}

		public boolean matchesPlatform(@Nullable OperatingSystem os, @Nullable Architecture arch) {
			return (this.os == null || this.os.equals(os)) && (this.arch == null || this.arch.equals(arch));
		}
	}

	public static class JacksonJqExpectation extends AbstractExpectation {
		@JsonProperty("incompat_type")
		public @Nullable IncompatibilityType incompatType;

		@JsonProperty("limit_exceeded")
		@JsonInclude(JsonInclude.Include.NON_DEFAULT)
		public boolean limitExceeded;

		@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
		public JacksonJqExpectation(@JsonProperty(value = "v", required = true) @JsonDeserialize(using = VersionRangeDeserializer.class) VersionRange version) {
			super(version);
		}

		@Override
		public boolean limitExceeded() {
			return limitExceeded;
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = false)
	public static class Expectations {
		@JsonProperty("default")
		public List<DefaultExpectation> defaultRows = Collections.emptyList();

		@JsonProperty("overrides")
		public List<OverrideExpectation> overrides = Collections.emptyList();

		@JsonProperty("jjq")
		public List<JacksonJqExpectation> jjq = Collections.emptyList();

		public AbstractExpectation resolve(Version version, boolean realJq, String osName, String osArch) {
			DefaultExpectation base = find(defaultRows, version);
			if (base == null) {
				DefaultExpectation unsupported = new DefaultExpectation(VersionRange.of(null, false, null, false));
				unsupported.compileError = "Unsupported jq version";
				return unsupported;
			}
			if (base.skip)
				return base;
			if (!realJq) {
				JacksonJqExpectation jjqRow = find(jjq, version);
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
			OverrideExpectation override = findOverride(version, os, arch);
			return override != null ? override : base;
		}

		private @Nullable OverrideExpectation findOverride(Version version, @Nullable OperatingSystem os, @Nullable Architecture arch) {
			for (OverrideExpectation row : overrides) {
				if (row.contains(version) && row.matchesPlatform(os, arch))
					return row;
			}
			return null;
		}

		private static <T extends AbstractExpectation> @Nullable T find(List<T> rows, Version version) {
			for (T row : rows) {
				if (row.contains(version))
					return row;
			}
			return null;
		}

		public boolean hasDefault(Version version) {
			return find(defaultRows, version) != null;
		}

		public boolean hasJjq(Version version) {
			return find(jjq, version) != null;
		}

		public void validateCoverage(List<Version> versions) {
			for (Version version : versions) {
				for (OperatingSystem os : OperatingSystem.values()) {
					for (Architecture arch : Architecture.values()) {
						if (find(defaultRows, version) == null && findOverride(version, os, arch) == null)
							throw new IllegalArgumentException("missing expectation for jq " + version + " on " + os + "/" + arch);
					}
				}
			}
		}

		public void validateNoRedundantOverrides(List<Version> versions, @Nullable FloatTolerance tolerance) {
			for (AbstractExpectation row : concat(overrides, jjq)) {
				for (Version version : versions) {
					DefaultExpectation base = find(defaultRows, version);
					if (row.contains(version) && base != null && ExpectationComparison.equivalent(base, row, tolerance))
						throw new IllegalArgumentException("unnecessary expectation override for jq " + version + ": " + row.version);
				}
			}
		}

		public void validate() {
			validateDefaultExpectations();
			validateOverrideExpectations();
			validateJjqExpectations();
			for (AbstractExpectation row : concat(overrides, jjq)) {
				VersionRange range = row.version;
				if (!coveredByDefault(range))
					throw new IllegalArgumentException("expectation override range is not covered by default: " + row.version);
				for (DefaultExpectation base : defaultRows) {
					if (base.skip && overlaps(range, base.version))
						throw new IllegalArgumentException("expectation override overlaps unstable default: " + base.version);
				}
			}
		}

		private boolean coveredByDefault(VersionRange range) {
			// Check boundaries as well as configured versions, so a gap between releases is rejected.
			List<VersionRange> ranges = new ArrayList<>();
			for (DefaultExpectation row : defaultRows)
				ranges.add(row.version);
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

		private static List<AbstractExpectation> concat(List<? extends AbstractExpectation> first, List<? extends AbstractExpectation> second) {
			List<AbstractExpectation> result = new ArrayList<>(first);
			result.addAll(second);
			return result;
		}

		private void validateDefaultExpectations() {
			if (defaultRows.isEmpty())
				throw new IllegalArgumentException("expectations.default must contain at least one row");
			for (DefaultExpectation row : defaultRows) {
				if (row.skip) {
					if (row.output != null || row.runtimeError != null || row.compileError != null || row.timeout)
						throw new IllegalArgumentException("expectations.default unstable row requires only v");
				} else if (row.timeout) {
					if (row.output != null || row.runtimeError != null || row.compileError != null)
						throw new IllegalArgumentException("expectations.default timeout row requires only v");
				} else {
					validateOutcome(row, "default");
				}
			}
			validateDisjointRanges(defaultRows, "default");
			validateDistinctConsecutiveResults(defaultRows, "default");
		}

		private void validateOverrideExpectations() {
			for (OverrideExpectation row : overrides) {
				if (row.os == null && row.arch == null)
					throw new IllegalArgumentException("expectations.overrides row requires os or arch");
				if (row.timeout) {
					if (row.output != null || row.runtimeError != null || row.compileError != null)
						throw new IllegalArgumentException("expectations.overrides timeout row cannot specify output or an error");
				} else {
					validateOutcome(row, "overrides");
				}
			}
			for (OperatingSystem os : OperatingSystem.values()) {
				for (Architecture arch : Architecture.values()) {
					for (int i = 0; i < overrides.size(); i++) {
						OverrideExpectation row = overrides.get(i);
						if (!row.matchesPlatform(os, arch))
							continue;
						for (int j = 0; j < i; j++) {
							OverrideExpectation previous = overrides.get(j);
							if (previous.matchesPlatform(os, arch) && overlaps(row.version, previous.version))
								throw new IllegalArgumentException("overlapping expectations.overrides ranges for " + os + "/" + arch);
						}
					}
				}
			}
			for (int i = 0; i < overrides.size(); i++) {
				OverrideExpectation row = overrides.get(i);
				for (int j = 0; j < i; j++) {
					OverrideExpectation previous = overrides.get(j);
					if (row.os == previous.os && row.arch == previous.arch
							&& consecutive(row.version, previous.version)
							&& sameResult(row, previous))
						throw new IllegalArgumentException("identical consecutive expectations.overrides ranges");
				}
			}
		}

		private void validateJjqExpectations() {
			for (JacksonJqExpectation row : jjq) {
				if (row.incompatType == null)
					throw new IllegalArgumentException("expectations.jjq row requires incompat_type");
				if (row.limitExceeded) {
					if (row.output == null || row.compileError != null || row.runtimeError != null)
						throw new IllegalArgumentException("expectations.jjq limit_exceeded row requires output and no other result");
				} else {
					validateOutcome(row, "jjq");
				}
			}
			validateDisjointRanges(jjq, "jjq");
			validateDistinctConsecutiveResults(jjq, "jjq");
		}

		private static void validateDistinctConsecutiveResults(List<? extends AbstractExpectation> rows, String name) {
			for (int i = 0; i < rows.size(); i++) {
				AbstractExpectation row = rows.get(i);
				for (int j = 0; j < i; j++) {
					AbstractExpectation previous = rows.get(j);
					if (consecutive(row.version, previous.version) && sameResult(row, previous))
						throw new IllegalArgumentException("identical consecutive expectations." + name + " ranges");
				}
			}
		}

		private static boolean sameResult(AbstractExpectation a, AbstractExpectation b) {
			// Outputs are compared by value, the way every assertion reads them, so two rows that
			// differ only in how a number is written are the one result they look like.
			return ExpectationComparison.sameValues(a.output, b.output, null)
					&& Objects.equals(a.runtimeError, b.runtimeError) && Objects.equals(a.compileError, b.compileError)
					&& a.timedOut() == b.timedOut() && a.unstable() == b.unstable()
					&& a.limitExceeded() == b.limitExceeded() && incompatType(a) == incompatType(b);
		}

		private static @Nullable IncompatibilityType incompatType(AbstractExpectation row) {
			return row instanceof JacksonJqExpectation jjqRow ? jjqRow.incompatType : null;
		}

		private static boolean consecutive(VersionRange a, VersionRange b) {
			return adjacent(a, b) || adjacent(b, a);
		}

		private static boolean adjacent(VersionRange a, VersionRange b) {
			return a.maxVersion() != null && a.maxVersion().equals(b.minVersion()) && !a.maxInclusive() && b.minInclusive();
		}

		private static void validateOutcome(AbstractExpectation row, String name) {
			if (row.compileError != null) {
				if (row.compileError.isBlank() || row.output != null || row.runtimeError != null)
					throw new IllegalArgumentException("expectations." + name + " compile_error row cannot specify output or runtime_error");
			} else if (row.output == null) {
				throw new IllegalArgumentException("expectations." + name + " row requires output");
			}
			if (row.runtimeError != null && row.runtimeError.isBlank())
				throw new IllegalArgumentException("expectations." + name + " runtime_error must not be blank");
		}

		private static void validateDisjointRanges(List<? extends AbstractExpectation> rows, String name) {
			for (int i = 0; i < rows.size(); i++) {
				VersionRange range = rows.get(i).version;
				for (int j = 0; j < i; j++) {
					if (overlaps(range, rows.get(j).version))
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
	public Expectations expectations = new Expectations();

	@JsonIgnoreProperties(ignoreUnknown = false)
	public static class TypeAssertion {
		@JsonProperty("input")
		public String input = "";

		@JsonProperty("output")
		public String output = "";

		@JsonProperty("v")
		@JsonDeserialize(using = VersionRangeDeserializer.class)
		@JsonSerialize(using = ToStringSerializer.class)
		public final VersionRange version;

		@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
		public TypeAssertion(@JsonProperty(value = "v", required = true) @JsonDeserialize(using = VersionRangeDeserializer.class) VersionRange version) {
			this.version = Objects.requireNonNull(version, "type assertion requires v");
		}

		public boolean appliesTo(Version jqVersion) {
			return version.contains(jqVersion);
		}

		public TypeAssertion(String input, String output) {
			this(VersionRange.valueOf("[1.5, )"));
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

	public void validateTypes() {
		for (int i = 0; i < types.size(); i++) {
			TypeAssertion row = types.get(i);
			for (int j = 0; j < i; j++) {
				TypeAssertion previous = types.get(j);
				if (row.input.equals(previous.input) && row.output.equals(previous.output)
						&& Expectations.consecutive(row.version, previous.version))
					throw new IllegalArgumentException("identical consecutive types ranges: " + q);
			}
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = false)
	public static class PropertyAssertion {
		@JsonProperty("v")
		@JsonDeserialize(using = VersionRangeDeserializer.class)
		@JsonSerialize(using = ToStringSerializer.class)
		public final VersionRange version;

		@JsonProperty("cardinality")
		public final Cardinality cardinality;

		@JsonProperty("depends_on_input")
		public final boolean dependsOnInput;

		@JsonProperty("depends_on_external_state")
		public final boolean dependsOnExternalState;

		@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
		public PropertyAssertion(
				@JsonProperty(value = "v", required = true) @JsonDeserialize(using = VersionRangeDeserializer.class) VersionRange version,
				@JsonProperty(value = "cardinality", required = true) Cardinality cardinality,
				@JsonProperty(value = "depends_on_input", required = true) Boolean dependsOnInput,
				@JsonProperty(value = "depends_on_external_state", required = true) Boolean dependsOnExternalState) {
			this.version = Objects.requireNonNull(version, "property assertion requires v");
			this.cardinality = Objects.requireNonNull(cardinality, "property assertion requires cardinality");
			this.dependsOnInput = Objects.requireNonNull(dependsOnInput, "property assertion requires depends_on_input");
			this.dependsOnExternalState = Objects.requireNonNull(dependsOnExternalState, "property assertion requires depends_on_external_state");
		}

		public boolean appliesTo(Version jqVersion) {
			return version.contains(jqVersion);
		}

		@Override
		public String toString() {
			return String.format("{cardinality: %s, depends_on_input: %s, depends_on_external_state: %s}",
					cardinality, dependsOnInput, dependsOnExternalState);
		}
	}

	@JsonProperty("properties")
	public List<PropertyAssertion> properties = Collections.emptyList();

	public void validateProperties() {
		if (properties == null)
			throw new IllegalArgumentException("properties must be a list: " + q);
		for (int i = 0; i < properties.size(); i++) {
			PropertyAssertion row = properties.get(i);
			if (row.version.minVersion() == null || !row.version.minInclusive() || (row.version.maxVersion() != null && row.version.maxInclusive()))
				throw new IllegalArgumentException("properties range must be half-open: " + row.version);
			for (int j = 0; j < i; j++) {
				PropertyAssertion previous = properties.get(j);
				if (Expectations.overlaps(row.version, previous.version))
					throw new IllegalArgumentException("overlapping properties ranges: " + q);
				if (Expectations.consecutive(row.version, previous.version)
						&& row.cardinality == previous.cardinality
						&& row.dependsOnInput == previous.dependsOnInput
						&& row.dependsOnExternalState == previous.dependsOnExternalState)
					throw new IllegalArgumentException("identical consecutive properties ranges: " + q);
			}
		}
	}

	@JsonProperty("file")
	public String file = "";

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

	public boolean appliesToAssertions(Version jqVersion) {
		return expectations.hasDefault(jqVersion);
	}

	@JsonProperty("comment")
	public @Nullable String comment;

	/**
	 * Why a case asserts what it does, when that is not obvious -- an upstream issue, say. No
	 * harness reads it; it is documentation that travels with the case.
	 */
	@JsonProperty("justification")
	public @Nullable String justification;

	/**
	 * Names the case the way an error message should: the file it is written in, and the command it
	 * stands for. Falls back to the command alone for a case parsed without a file.
	 *
	 * @return the case's identity, for an error message to prefix its reason with
	 */
	public String describe() {
		return file.isEmpty() ? toString() : file + ": " + this;
	}

	@Override
	public String toString() {
		return String.format("jq '%s' <<< '%s'", q, input);
	}
}
