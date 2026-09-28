package net.thisptr.jackson.jq.v2.test.verify;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.test.testcase.TestCase;
import net.thisptr.jackson.jq.v2.test.testcase.TestCaseLoader;

import static org.assertj.core.api.Assertions.assertThat;

class DivergenceTest {
	@Test
	void appliesOnlyOnTheSelectedPlatforms() throws IOException {
		TestCase absent = TestCaseLoader.parseTestCase("{}");
		TestCase always = TestCaseLoader.parseTestCase("{\"diverges_from_jq\":\"ALWAYS\"}");
		TestCase macOS = TestCaseLoader.parseTestCase("{\"diverges_from_jq\":\"ON_MACOS\"}");

		assertThat(VerifyTestCasesTest.expectsDivergence(absent, "Mac OS X")).isFalse();
		assertThat(VerifyTestCasesTest.expectsDivergence(absent, "Linux")).isFalse();
		assertThat(VerifyTestCasesTest.expectsDivergence(always, "Mac OS X")).isTrue();
		assertThat(VerifyTestCasesTest.expectsDivergence(always, "Linux")).isTrue();
		assertThat(VerifyTestCasesTest.expectsDivergence(macOS, "Mac OS X")).isTrue();
		assertThat(VerifyTestCasesTest.expectsDivergence(macOS, "Linux")).isFalse();
	}
}
