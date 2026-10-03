package net.thisptr.jackson.jq.v2.test.testcase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.spi.version.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ExpectationsTest {
	@Test
	void rejectsUnknownTopLevelFieldsInJsonAndYaml() {
		for (String field : List.of("in", "out", "incompat_type", "v")) {
			String json = "{\"q\":\".\",\"" + field + "\":null}";
			String yaml = "- q: '.'\n  " + field + ": null\n";
			assertThatThrownBy(() -> TestCaseLoader.parseTestCase(json))
					.isInstanceOf(IOException.class)
					.hasMessageContaining("\"" + field + "\"");
			assertThatThrownBy(() -> TestCaseLoader.loadTestCases("legacy.yaml", new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))))
					.isInstanceOf(IOException.class)
					.hasMessageContaining("\"" + field + "\"");
		}
	}

	@Test
	void rejectsUnknownExpectationOutputInJsonAndYaml() {
		String json = "{\"q\":\".\",\"expectations\":{\"default\":[{\"v\":\"[1.5, )\",\"out\":[1]}]}}";
		String yaml = "- q: '.'\n  expectations:\n    default:\n    - v: '[1.5, )'\n      out: [1]\n";
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(json))
				.isInstanceOf(IOException.class)
				.hasMessageContaining("\"out\"");
		assertThatThrownBy(() -> TestCaseLoader.loadTestCases("legacy.yaml", new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))))
				.isInstanceOf(IOException.class)
				.hasMessageContaining("\"out\"");
	}

	private static final String PROPERTIES = """
			{"q":".","expectations":{"default":[{"v":"[1.5, )","output":[]}]},"properties":[
			{"v":"[1.5, 1.7)","cardinality":"ONE","depends_on_input":true,"depends_on_external_state":false},
			{"v":"[1.7, )","cardinality":"UNKNOWN","depends_on_input":true,"depends_on_external_state":false}]}
			""";

	@Test
	void readsVersionedProperties() throws IOException {
		TestCase tc = TestCaseLoader.parseTestCase(PROPERTIES);
		assertThat(tc.properties).hasSize(2);
		assertThat(tc.properties.get(0).appliesTo(Version.valueOf("1.6"))).isTrue();
		assertThat(tc.properties.get(0).appliesTo(Version.valueOf("1.7"))).isFalse();
	}

	@Test
	void rejectsIncompleteOrOverlappingProperties() {
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(PROPERTIES.replace("\"depends_on_input\":true,", "")))
				.isInstanceOf(IOException.class);
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(PROPERTIES.replace("\"cardinality\":\"ONE\"", "\"cardinality\":null")))
				.isInstanceOf(IOException.class);
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(PROPERTIES.replace("\"depends_on_external_state\":false", "\"depends_on_external_state\":null")))
				.isInstanceOf(IOException.class);
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(PROPERTIES.replace("[1.7, )", "[1.6, )")))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsIdenticalConsecutiveProperties() {
		String identical = PROPERTIES.replace("\"cardinality\":\"UNKNOWN\"", "\"cardinality\":\"ONE\"");
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(identical))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("identical consecutive properties");
		assertThatCode(() -> TestCaseLoader.parseTestCase(identical.replace("[1.7, )", "[1.8.0, )")))
				.doesNotThrowAnyException();
	}

	@Test
	void rejectsIdenticalConsecutiveTypesForTheSameInput() {
		String rows = """
				{"q":".","expectations":{"default":[{"v":"[1.5, )","output":[]}]},"types":[
				{"v":"[1.7, )","input":"ANY","output":"NUMBER"},
				{"v":"[1.5, 1.7)","input":"ANY","output":"NUMBER"}]}
				""";
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(rows))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("identical consecutive types");
		assertThatCode(() -> TestCaseLoader.parseTestCase(rows.replace("\"input\":\"ANY\",\"output\":\"NUMBER\"}]", "\"input\":\"NULL\",\"output\":\"NUMBER\"}]")))
				.doesNotThrowAnyException();
		assertThatCode(() -> TestCaseLoader.parseTestCase(rows.replace("[1.7, )", "[1.8.0, )")))
				.doesNotThrowAnyException();
		String oneRow = """
				{"q":".","expectations":{"default":[{"v":"[1.5, )","output":[]}]},"types":[{"v":["[1.5, 1.7)","[1.7, )"],"input":"ANY","output":"NUMBER"}]}
				""";
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(oneRow))
				.isInstanceOf(IOException.class);
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(oneRow.replace("\"v\":[\"[1.5, 1.7)\",\"[1.7, )\"],", "")))
				.isInstanceOf(IOException.class);
	}

	private static final String CASE = """
			{"q":".","expectations":{"default":[
			{"v":"[1.5, 1.7)","output":[1]},
			{"v":"[1.7, )","output":[2]}],
			"overrides":[{"v":"[1.8.0, )","os":"MACOS","output":[3]},
			{"v":"[1.7, 1.8.0)","os":"MACOS","arch":"AARCH64","timeout":true}],
			"jjq":[{"v":"[1.7, )","output":[],"runtime_error":"example error","incompat_type":"INTENTIONAL"}]}}
			""";

	@Test
	void resolvesEachTarget() throws IOException {
		TestCase.Expectations expectations = TestCaseLoader.parseTestCase(CASE).expectations;
		assertThat(expectations.resolve(Version.valueOf("1.6"), true, "Linux", "amd64").values().get(0).toString()).isEqualTo("1");
		assertThat(expectations.resolve(Version.valueOf("1.7"), true, "Linux", "amd64").values().get(0).toString()).isEqualTo("2");
		assertThat(expectations.resolve(Version.valueOf("1.7"), true, "Mac OS X", "x86_64").values().get(0).toString()).isEqualTo("2");
		assertThat(expectations.resolve(Version.valueOf("1.7"), true, "Mac OS X", "aarch64").timedOut()).isTrue();
		assertThat(expectations.resolve(Version.valueOf("1.7.1"), true, "Mac OS X", "arm64").timedOut()).isTrue();
		assertThat(expectations.resolve(Version.valueOf("1.8.2"), true, "Mac OS X", "aarch64").values().get(0).toString()).isEqualTo("3");
		assertThat(expectations.resolve(Version.valueOf("1.8.2"), false, "Mac OS X", "aarch64").runtimeError).isNotNull();
		assertThat(expectations.resolve(Version.valueOf("1.6"), false, "Mac OS X", "aarch64").runtimeError).isNull();
		assertThat(expectations.resolve(Version.valueOf("1.4"), true, "Linux", "amd64").compileError).isNotNull();
		assertThat(expectations.resolve(Version.valueOf("1.4"), false, "Mac OS X", "aarch64").compileError).isNotNull();
	}

	@Test
	void acceptsVersionedJjqClassificationAndDefaultTimeout() throws IOException {
		String testCase = """
				{"q":".","expectations":{"default":[
				{"v":"[1.5, 1.6)","timeout":true},
				{"v":"[1.6, )","output":[1]}],
				"jjq":[{"v":"[1.6, )","output":[2],"incompat_type":"JACKSON_JQ_BUG"}]}}
				""";
		TestCase.Expectations expectations = TestCaseLoader.parseTestCase(testCase).expectations;
		assertThat(expectations.resolve(Version.valueOf("1.5"), true, "Linux", "amd64").timedOut()).isTrue();
		assertThat(expectations.resolve(Version.valueOf("1.6"), false, "Linux", "amd64"))
				.isInstanceOfSatisfying(TestCase.JacksonJqExpectation.class,
						row -> assertThat(row.incompatType).isEqualTo(TestCase.IncompatibilityType.JACKSON_JQ_BUG));
		assertThat(expectations.resolve(Version.valueOf("1.6"), true, "Linux", "amd64").values().get(0).asInt()).isEqualTo(1);
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(testCase.replace("\"output\":[2],", "")))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(testCase.replace("\"timeout\":true", "\"timeout\":true,\"output\":[]")))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void recordsPartialOutputWhenJacksonJqExceedsALimit() throws IOException {
		String testCase = """
				{"q":".","expectations":{"default":[{"v":"[1.5, )","output":[1]}],
				"jjq":[{"v":"[1.5, )","output":[1,2],"limit_exceeded":true,"incompat_type":"UNCLASSIFIED"}]}}
				""";
		TestCase.JacksonJqExpectation row = TestCaseLoader.parseTestCase(testCase).expectations.jjq.get(0);
		assertThat(row.limitExceeded).isTrue();
		assertThat(row.values()).hasSize(2);
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(testCase.replace("\"output\":[1,2],", "")))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsOverridesEquivalentToTheDefaultResult() throws IOException {
		String testCase = """
				{"q":".","expectations":{"default":[{"v":"[1.5, )","output":[1],"runtime_error":"jq failed"}],
				"jjq":[{"v":"[1.5, )","compile_error":"jackson-jq failed","incompat_type":"INTENTIONAL"}]}}
				""";
		TestCase tc = TestCaseLoader.parseTestCase(testCase);
		assertThatThrownBy(() -> tc.expectations.validateNoRedundantOverrides(List.of(Version.valueOf("1.5")), null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("unnecessary expectation override");
		String successfulJjq = testCase.replace("\"compile_error\":\"jackson-jq failed\"", "\"output\":[1]");
		TestCase different = TestCaseLoader.parseTestCase(successfulJjq);
		assertThatCode(() -> different.expectations.validateNoRedundantOverrides(List.of(Version.valueOf("1.5")), null))
				.doesNotThrowAnyException();
	}

	@Test
	void skipsAnUnstableJqOutcomeForOneVersion() throws IOException {
		String testCase = """
				{"q":".","expectations":{"default":[
				{"v":"[1.5, 1.6)","unstable":true},
				{"v":"[1.6, )","output":[1]}]}}
				""";
		TestCase.Expectations expectations = TestCaseLoader.parseTestCase(testCase).expectations;
		expectations.validateCoverage(List.of(Version.valueOf("1.5"), Version.valueOf("1.6")));
		assertThat(expectations.resolve(Version.valueOf("1.5"), true, "Linux", "amd64").unstable()).isTrue();
		assertThat(expectations.resolve(Version.valueOf("1.5"), false, "Linux", "amd64").unstable()).isTrue();
		assertThat(expectations.resolve(Version.valueOf("1.6"), true, "Linux", "amd64").values().get(0).asInt()).isEqualTo(1);
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(testCase.replace("\"unstable\":true", "\"unstable\":true,\"output\":[]")))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(testCase.replace("\"unstable\":true", "\"unstable\":true,\"timeout\":true")))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void requiresExplicitCoverageForEveryVersionAndKnownPlatform() throws IOException {
		TestCase.Expectations covered = TestCaseLoader.parseTestCase(CASE).expectations;
		covered.validateCoverage(List.of(Version.valueOf("1.5"), Version.valueOf("1.6"), Version.valueOf("1.8.2")));

		String missingJq15 = CASE.replace("[1.5, 1.7)", "[1.6, 1.7)");
		TestCase.Expectations incomplete = TestCaseLoader.parseTestCase(missingJq15).expectations;
		assertThatThrownBy(() -> incomplete.validateCoverage(List.of(Version.valueOf("1.5"))))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("jq 1.5")
				.hasMessageContaining("MACOS/AARCH64");
	}

	@Test
	void resolvesArchOnlyOverride() throws IOException {
		String archOnly = CASE.replace("\"overrides\":[{\"v\":\"[1.8.0, )\",\"os\":\"MACOS\",\"output\":[3]},", "\"overrides\":[")
				.replace("\"os\":\"MACOS\",\"arch\":\"AARCH64\"", "\"arch\":\"AARCH64\"");
		TestCase.Expectations expectations = TestCaseLoader.parseTestCase(archOnly).expectations;
		assertThat(expectations.resolve(Version.valueOf("1.7"), true, "Linux", "aarch64").timedOut()).isTrue();
		assertThat(expectations.resolve(Version.valueOf("1.7"), true, "Windows 11", "aarch64").timedOut()).isTrue();
		assertThat(expectations.resolve(Version.valueOf("1.7"), true, "Mac OS X", "x86_64").values().get(0).toString()).isEqualTo("2");
	}

	@Test
	void fallsBackToDefaultOnUnsupportedPlatforms() throws IOException {
		assertThatThrownBy(() -> TestCase.OperatingSystem.fromSystemProperty("Windows 11"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("unsupported operating system");
		assertThatThrownBy(() -> TestCase.Architecture.fromSystemProperty("riscv64"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("unsupported architecture");
		TestCase.Expectations expectations = TestCaseLoader.parseTestCase(CASE).expectations;
		assertThat(expectations.resolve(Version.valueOf("1.8.2"), true, "Windows 11", "aarch64").values().get(0).toString()).isEqualTo("2");
		assertThat(expectations.resolve(Version.valueOf("1.8.2"), true, "Mac OS X", "riscv64").values().get(0).toString()).isEqualTo("3");
		assertThat(expectations.resolve(Version.valueOf("1.8.2"), true, "Windows 11", "riscv64").values().get(0).toString()).isEqualTo("2");
	}

	@Test
	void rejectsOverlappingRows() {
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(CASE.replace("[1.7, )\",\"output\":[2]", "[1.6, )\",\"output\":[2]"))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(CASE.replace("\"v\":\"[1.8.0, )\",\"os\":\"MACOS\"", "\"v\":\"[1.7, )\",\"os\":\"MACOS\"")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("overlapping expectations.overrides");
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(overlappingOverrides("\"os\":\"MACOS\"", "\"arch\":\"AARCH64\"")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("overlapping expectations.overrides");
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(overlappingOverrides("\"arch\":\"AARCH64\"", "\"os\":\"MACOS\",\"arch\":\"AARCH64\"")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("overlapping expectations.overrides");
	}

	private static String overlappingOverrides(String firstSelectors, String secondSelectors) {
		return "{\"q\":\".\",\"expectations\":{\"default\":[{\"v\":\"[1.5, )\",\"output\":[1]}],\"overrides\":["
				+ "{\"v\":\"[1.7, )\"," + firstSelectors + ",\"output\":[2]},"
				+ "{\"v\":\"[1.8.0, )\"," + secondSelectors + ",\"output\":[3]}]}}";
	}

	@Test
	void rejectsIdenticalConsecutiveExpectationResults() {
		String base = """
				{"q":".","expectations":{"default":[
				{"v":"[1.7, )","output":[1],"runtime_error":"example error"},
				{"v":"[1.5, 1.7)","output":[1],"runtime_error":"example error"}]}}
				""";
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(base))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("identical consecutive expectations.default");
		assertThatCode(() -> TestCaseLoader.parseTestCase(base.replace("[1.7, )", "[1.8.0, )")))
				.doesNotThrowAnyException();
		assertThatCode(() -> TestCaseLoader.parseTestCase(base.replace("[1],\"runtime_error\":\"example error\"}]}", "[1]}]}")))
				.doesNotThrowAnyException();

		String jjq = """
				{"q":".","expectations":{"default":[{"v":"[1.5, )","output":[0]}],"jjq":[
				{"v":"[1.7, )","output":[1],"incompat_type":"INTENTIONAL"},
				{"v":"[1.5, 1.7)","output":[1],"incompat_type":"INTENTIONAL"}]}}
				""";
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(jjq))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("identical consecutive expectations.jjq");
	}

	@Test
	void rejectsIdenticalConsecutivePlatformOverrides() {
		String rows = """
				{"q":".","expectations":{"default":[{"v":"[1.5, )","output":[0]}],"overrides":[
				{"v":"[1.7, )","os":"MACOS","arch":"AARCH64","output":[1]},
				{"v":"[1.5, 1.7)","os":"MACOS","arch":"AARCH64","output":[1]}]}}
				""";
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(rows))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("identical consecutive expectations.overrides");
		assertThatCode(() -> TestCaseLoader.parseTestCase(rows.replace("[1.7, )", "[1.8.0, )")))
				.doesNotThrowAnyException();
		assertThatCode(() -> TestCaseLoader.parseTestCase(rows.replace("\"arch\":\"AARCH64\",\"output\":[1]}]}", "\"arch\":\"AMD64\",\"output\":[1]}]}")))
				.doesNotThrowAnyException();
		assertThatCode(() -> TestCaseLoader.parseTestCase(rows.replace("\"output\":[1]}]}", "\"output\":[1],\"runtime_error\":\"example error\"}]}")))
				.doesNotThrowAnyException();
	}

	@Test
	void allowsOverlappingRangesOnDisjointPlatforms() throws IOException {
		TestCaseLoader.parseTestCase(overlappingOverrides("\"os\":\"MACOS\"", "\"os\":\"LINUX\""));
		TestCaseLoader.parseTestCase(overlappingOverrides("\"arch\":\"AARCH64\"", "\"arch\":\"AMD64\""));
	}

	@Test
	void rejectsOverridesOutsideTheDefaultRanges() {
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(CASE.replace("\"overrides\":[{\"v\":\"[1.8.0, )\"", "\"overrides\":[{\"v\":\"[1.4, 1.5)\""))).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void requiresOutputAndRejectsUnknownFields() {
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(CASE.replace("\"output\":[3]", "\"runtime_error\":\"example error\""))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(CASE.replace("\"v\":\"[1.8.0, )\",\"os\":\"MACOS\",\"output\":[3]", "\"os\":\"MACOS\",\"output\":[3]"))).isInstanceOf(IOException.class);
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(CASE.replace("\"v\":\"[1.5, 1.7)\",\"output\":[1]", "\"output\":[1]"))).isInstanceOf(IOException.class);
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(CASE.replace("\"q\":\".\"", "\"q\":\".\",\"v\":\"[1.5, )\""))).isInstanceOf(IOException.class);
	}

	@Test
	void rejectsFieldsFromAnotherExpectationContext() {
		String base = """
				{"q":".","expectations":{"default":[{"v":"[1.5, )","output":[]}]}}
				""";
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(base.replace("\"output\":[]", "\"output\":[],\"os\":\"LINUX\"")))
				.isInstanceOf(IOException.class);
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(base.replace("\"output\":[]", "\"output\":[],\"incompat_type\":\"INTENTIONAL\"")))
				.isInstanceOf(IOException.class);
		String jjq = base.replace("\"output\":[]}]", "\"output\":[]}],\"jjq\":[{\"v\":\"[1.5, )\",\"output\":[],\"incompat_type\":\"INTENTIONAL\",\"timeout\":true}]");
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(jjq))
				.isInstanceOf(IOException.class);
	}

	@Test
	void rejectsTimeoutRowsWithAResultAndOverridesWithoutAPlatform() {
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(CASE.replace("\"timeout\":true", "\"timeout\":true,\"output\":[]"))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(CASE.replace("\"timeout\":true", "\"timeout\":true,\"runtime_error\":\"example error\""))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(CASE.replace("\"output\":[],\"runtime_error\":\"example error\"", "\"timeout\":true"))).isInstanceOf(IOException.class);
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(CASE.replace("\"os\":\"MACOS\",\"output\":[3]", "\"output\":[3]"))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(CASE.replace("\"arch\":\"AARCH64\"", "\"arch\":\"unknown\""))).isInstanceOf(IOException.class);
	}
}
