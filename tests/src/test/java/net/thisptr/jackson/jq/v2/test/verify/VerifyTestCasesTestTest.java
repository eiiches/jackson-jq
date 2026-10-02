package net.thisptr.jackson.jq.v2.test.verify;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.version.VersionRange;
import net.thisptr.jackson.jq.v2.test.testcase.TestCase;
import net.thisptr.jackson.jq.v2.test.testcase.TestCaseLoader;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class VerifyTestCasesTestTest {
	private static final List<String> GROUPS = List.of("expectations.default", "expectations.overrides", "expectations.jjq", "types", "properties");

	private static TestCase caseWithRange(String group, String text) {
		VersionRange range = VersionRange.valueOf(text);
		TestCase tc = new TestCase();
		tc.expectations.defaultRows = List.of(new TestCase.DefaultExpectation(VersionRange.valueOf("[1.5, )")));
		switch (group) {
			case "expectations.default" ->
					tc.expectations.defaultRows = List.of(new TestCase.DefaultExpectation(range));
			case "expectations.overrides" ->
					tc.expectations.overrides = List.of(new TestCase.OverrideExpectation(range));
			case "expectations.jjq" -> tc.expectations.jjq = List.of(new TestCase.JacksonJqExpectation(range));
			case "types" -> tc.types = List.of(new TestCase.TypeAssertion(range));
			case "properties" ->
					tc.properties = List.of(new TestCase.PropertyAssertion(range, Cardinality.ONE, false, false));
			default -> throw new IllegalArgumentException(group);
		}
		return tc;
	}

	@Test
	void acceptsOpenEndsAndHistoricalRanges() {
		for (String group : GROUPS) {
			assertThatCode(() -> VerifyTestCasesTest.validateVersionRanges(caseWithRange(group, "[1.5, )"), Versions.JQ_1_8_2))
					.doesNotThrowAnyException();
			assertThatCode(() -> VerifyTestCasesTest.validateVersionRanges(caseWithRange(group, "[1.5, 1.8.2)"), Versions.JQ_1_8_2))
					.doesNotThrowAnyException();
		}
	}

	@Test
	void rejectsExclusiveStartsAndInclusiveEndsInEveryGroup() {
		for (String group : GROUPS) {
			assertThatThrownBy(() -> VerifyTestCasesTest.validateVersionRanges(caseWithRange(group, "(1.5, )"), Versions.JQ_1_8_2))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining(group + " range must have an inclusive start");
			assertThatThrownBy(() -> VerifyTestCasesTest.validateVersionRanges(caseWithRange(group, "[1.5, 1.7]"), Versions.JQ_1_8_2))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining(group + " range must have an exclusive end");
		}
	}

	@Test
	void requiresOpenEndWhenRangeIncludesLatestVersion() {
		for (String group : GROUPS) {
			assertThatThrownBy(() -> VerifyTestCasesTest.validateVersionRanges(caseWithRange(group, "[1.5, 1.8.2]"), Versions.JQ_1_8_2))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining(group + " range covering jq 1.8.2");
			assertThatThrownBy(() -> VerifyTestCasesTest.validateVersionRanges(caseWithRange(group, "[1.5, 1.8.3)"), Versions.JQ_1_8_2))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining(group + " range covering jq 1.8.2");
		}
	}

	@Test
	void rejectsInclusiveSpellingForUnboundedEnd() {
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase("""
				{"q":".","expectations":{"default":[{"v":"[1.5, ]","output":[]}]}}
				"""))
				.isInstanceOf(IOException.class)
				.hasMessageContaining("test case range must use [inclusive, exclusive)");
	}
}
