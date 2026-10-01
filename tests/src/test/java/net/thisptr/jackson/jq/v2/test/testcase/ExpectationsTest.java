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
			"overrides":[{"v":"[1.8.0, )","os":"MACOS","out":[3]},
			{"v":"[1.7, 1.8.0)","os":"MACOS","arch":"AARCH64","timeout":true}],
			"jjq":[{"v":"[1.7, )","out":[],"error":true}]}}
			""";

	@Test
	void resolvesEachTarget() throws IOException {
		TestCase.Expectations expectations = Objects.requireNonNull(TestCaseLoader.parseTestCase(CASE).expectations);
		assertThat(expectations.resolve(Version.valueOf("1.6"), true, "Linux", "amd64").values().get(0).toString()).isEqualTo("1");
		assertThat(expectations.resolve(Version.valueOf("1.7"), true, "Linux", "amd64").values().get(0).toString()).isEqualTo("2");
		assertThat(expectations.resolve(Version.valueOf("1.7"), true, "Mac OS X", "x86_64").values().get(0).toString()).isEqualTo("2");
		assertThat(expectations.resolve(Version.valueOf("1.7"), true, "Mac OS X", "aarch64").timeout).isTrue();
		assertThat(expectations.resolve(Version.valueOf("1.7.1"), true, "Mac OS X", "arm64").timeout).isTrue();
		assertThat(expectations.resolve(Version.valueOf("1.8.2"), true, "Mac OS X", "aarch64").values().get(0).toString()).isEqualTo("3");
		assertThat(expectations.resolve(Version.valueOf("1.8.2"), false, "Mac OS X", "aarch64").error).isTrue();
		assertThat(expectations.resolve(Version.valueOf("1.6"), false, "Mac OS X", "aarch64").error).isFalse();
		assertThat(expectations.resolve(Version.valueOf("1.4"), true, "Linux", "amd64").error).isTrue();
		assertThat(expectations.resolve(Version.valueOf("1.4"), false, "Mac OS X", "aarch64").values()).isEmpty();
	}

	@Test
	void resolvesArchOnlyOverride() throws IOException {
		String archOnly = CASE.replace("\"overrides\":[{\"v\":\"[1.8.0, )\",\"os\":\"MACOS\",\"out\":[3]},", "\"overrides\":[")
				.replace("\"os\":\"MACOS\",\"arch\":\"AARCH64\"", "\"arch\":\"AARCH64\"");
		TestCase.Expectations expectations = Objects.requireNonNull(TestCaseLoader.parseTestCase(archOnly).expectations);
		assertThat(expectations.resolve(Version.valueOf("1.7"), true, "Linux", "aarch64").timeout).isTrue();
		assertThat(expectations.resolve(Version.valueOf("1.7"), true, "Windows 11", "aarch64").timeout).isTrue();
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
		TestCase.Expectations expectations = Objects.requireNonNull(TestCaseLoader.parseTestCase(CASE).expectations);
		assertThat(expectations.resolve(Version.valueOf("1.8.2"), true, "Windows 11", "aarch64").values().get(0).toString()).isEqualTo("2");
		assertThat(expectations.resolve(Version.valueOf("1.8.2"), true, "Mac OS X", "riscv64").values().get(0).toString()).isEqualTo("3");
		assertThat(expectations.resolve(Version.valueOf("1.8.2"), true, "Windows 11", "riscv64").values().get(0).toString()).isEqualTo("2");
	}

	@Test
	void rejectsOverlappingRows() {
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(CASE.replace("[1.7, )\",\"out\":[2]", "[1.6, )\",\"out\":[2]"))).isInstanceOf(IllegalArgumentException.class);
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
		return "{\"q\":\".\",\"expectations\":{\"default\":[{\"v\":\"[1.5, )\",\"out\":[1]}],\"overrides\":["
				+ "{\"v\":\"[1.7, )\"," + firstSelectors + ",\"out\":[2]},"
				+ "{\"v\":\"[1.8.0, )\"," + secondSelectors + ",\"out\":[3]}]}}";
	}

	@Test
	void allowsOverlappingRangesOnDisjointPlatforms() throws IOException {
		TestCaseLoader.parseTestCase(overlappingOverrides("\"os\":\"MACOS\"", "\"os\":\"LINUX\""));
		TestCaseLoader.parseTestCase(overlappingOverrides("\"arch\":\"AARCH64\"", "\"arch\":\"AMD64\""));
	}

	@Test
	void rejectsRedundantAndUncoveredOverrides() {
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(CASE.replace("\"out\":[3]", "\"out\":[2]"))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(CASE.replace("\"overrides\":[{\"v\":\"[1.8.0, )\"", "\"overrides\":[{\"v\":\"(, 1.5)\""))).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void requiresOutputAndRejectsLegacyControls() {
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(CASE.replace("\"out\":[3]", "\"error\":true"))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(CASE.replace("\"v\":\"[1.8.0, )\",\"os\":\"MACOS\",\"out\":[3]", "\"os\":\"MACOS\",\"out\":[3]"))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(CASE.replace("\"v\":\"[1.5, 1.7)\",\"out\":[1]", "\"out\":[1]"))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(CASE.replace("\"q\":\".\"", "\"q\":\".\",\"v\":\"[1.5, )\""))).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void timeoutRequiresPlatformOverrideWithoutOutputOrError() {
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(CASE.replace("\"timeout\":true", "\"timeout\":true,\"out\":[]"))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(CASE.replace("\"timeout\":true", "\"timeout\":true,\"error\":true"))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(CASE.replace("\"v\":\"[1.7, )\",\"out\":[2]", "\"v\":\"[1.7, )\",\"timeout\":true"))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(CASE.replace("\"out\":[],\"error\":true", "\"timeout\":true"))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(CASE.replace("\"os\":\"MACOS\",\"out\":[3]", "\"out\":[3]"))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> TestCaseLoader.parseTestCase(CASE.replace("\"arch\":\"AARCH64\"", "\"arch\":\"unknown\""))).isInstanceOf(IOException.class);
	}
}
