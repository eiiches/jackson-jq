package net.thisptr.jackson.jq.v2.test.testcase;

import java.io.IOException;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.spi.version.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ExpectationsTest {
	private static final String CASE = """
			{"q":".","expectations":{"default":[
			{"v":"[1.5, 1.7)","out":[1]},
			{"v":"[1.7, )","out":[2]}],
			"macos":[{"v":"[1.7, )","out":[3]}],
			"jjq":[{"v":"[1.7, )","out":[],"error":true}]}}
			""";

	@Test
	void resolvesEachTarget() throws IOException {
		TestCase.Expectations expectations = Objects.requireNonNull(TestCaseLoader.parseTestCase(CASE).expectations);
		assertThat(expectations.resolve(Version.valueOf("1.6"), true, "Linux").values().get(0).toString()).isEqualTo("1");
		assertThat(expectations.resolve(Version.valueOf("1.8.2"), true, "Linux").values().get(0).toString()).isEqualTo("2");
		assertThat(expectations.resolve(Version.valueOf("1.8.2"), true, "Mac OS X").values().get(0).toString()).isEqualTo("3");
		assertThat(expectations.resolve(Version.valueOf("1.8.2"), false, "Mac OS X").error).isTrue();
		assertThat(expectations.resolve(Version.valueOf("1.6"), false, "Mac OS X").error).isFalse();
		assertThat(expectations.resolve(Version.valueOf("1.4"), true, "Linux").error).isTrue();
		assertThat(expectations.resolve(Version.valueOf("1.4"), false, "Mac OS X").values()).isEmpty();
	}

	@Test
	void rejectsOverlappingRows() {
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(CASE.replace("[1.7, )\",\"out\":[2]", "[1.6, )\",\"out\":[2]"))).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsRedundantAndUncoveredOverrides() {
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(CASE.replace("\"out\":[3]", "\"out\":[2]"))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(CASE.replace("\"macos\":[{\"v\":\"[1.7, )\"", "\"macos\":[{\"v\":\"(, 1.7)\""))).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void requiresOutputAndRejectsLegacyControls() {
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(CASE.replace("\"out\":[3]", "\"error\":true"))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(CASE.replace("\"v\":\"[1.7, )\",\"out\":[3]", "\"out\":[3]"))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(CASE.replace("\"v\":\"[1.5, 1.7)\",\"out\":[1]", "\"out\":[1]"))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(CASE.replace("\"q\":\".\"", "\"q\":\".\",\"v\":\"[1.5, )\""))).isInstanceOf(IllegalArgumentException.class);
	}
}
