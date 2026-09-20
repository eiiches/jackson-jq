package net.thisptr.jackson.jq.v2.regex.impl.joni;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.RuntimeOptions;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.RuntimeLimitExceededException;
import net.thisptr.jackson.jq.v2.spi.version.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SubImplFunctionTest {
	private static final JsonProvider<JsonNode> JSON_PROVIDER = Jackson2JsonProvider.getInstance();

	@Test
	public void gsubUsesCaptureObjectsForReplacementExpression() {
		List<JsonNode> out = apply("gsub(\"(?<d>\\\\d)\"; \"\\(.d|tonumber+1)\")", "a1b2");

		assertThat(out).extracting(JSON_PROVIDER::getString).containsExactly("a2b3");
	}

	@Test
	public void gsubPreservesMultipleReplacementOutputs() {
		List<JsonNode> out = apply("gsub(\"a\"; \"x\", \"y\")", "aa");

		assertThat(out).extracting(JSON_PROVIDER::getString).containsExactly("xx", "yx", "xy", "yy");
	}

	@Test
	public void gsubHandlesManyMatchesWithoutStackOverflow() {
		List<JsonNode> out = apply("gsub(\"a\"; \"\")", repeat("a", 10000));

		assertThat(out).extracting(JSON_PROVIDER::getString).containsExactly("");
	}

	@Test
	public void gsubAdvancesPastZeroWidthMatchesByCodePointInEveryVersion() {
		for (Version version : Versions.versions()) {
			assertThat(apply("gsub(\"\"; \"X\")", "a😀b", version)).extracting(JSON_PROVIDER::getString).containsExactly("XaX😀XbX");
		}
	}

	@Test
	public void subEmitsSuccessfulReplacementBranchesBeforeReplacementError() {
		List<JsonNode> out = apply("try sub(\"a\"; \"1\", \"2\", error(\"bar\"); \"g\") catch .", "abcabc");

		assertThat(out).extracting(JSON_PROVIDER::getString).containsExactly("1bc1bc", "2bc1bc", "bar");
	}

	@Test
	public void subEmitsSuccessfulReplacementBranchesBeforePatternError() {
		List<JsonNode> out = apply("try sub(\"a\", \"b\", error(\"foo\"); \"1\", \"2\", error(\"bar\"); \"\", \"g\", error(\"baz\")) catch .", "abcabc");

		assertThat(out).extracting(JSON_PROVIDER::getString).containsExactly("1bcabc", "2bcabc", "bar");
	}

	@Test
	public void subEmitsSuccessfulReplacementBranchesBeforeFlagsError() {
		List<JsonNode> out = apply("try sub(\"a\", \"b\", error(\"foo\"); \"1\", \"2\"; \"\", \"g\", error(\"baz\")) catch .", "abcabc");

		assertThat(out).extracting(JSON_PROVIDER::getString).containsExactly("1bcabc", "2bcabc", "1bcabc", "2bcabc", "baz");
	}

	@Test
	public void gsubIsBoundedByTheMaxStringLength() {
		// Every input here is tiny; it is the replacement that multiplies them out -- four matches of
		// one character each, replaced by ten, make forty.
		assertThatThrownBy(() -> apply("gsub(\"a\"; \"xxxxxxxxxx\")", "aaaa", RuntimeOptions.newBuilder().setMaxStringLength(39).build()))
				.isInstanceOf(RuntimeLimitExceededException.class)
				.hasMessageContaining("maximum string length of 39");
		assertThatCode(() -> apply("gsub(\"a\"; \"xxxxxxxxxx\")", "aaaa", RuntimeOptions.newBuilder().setMaxStringLength(40).build())).doesNotThrowAnyException();
	}

	private static List<JsonNode> apply(String queryText, String input) {
		return apply(queryText, input, RuntimeOptions.newBuilder().build());
	}

	private static List<JsonNode> apply(String queryText, String input, RuntimeOptions options) {
		return apply(queryText, input, options, Versions.JQ_1_8_2);
	}

	private static List<JsonNode> apply(String queryText, String input, Version version) {
		return apply(queryText, input, RuntimeOptions.newBuilder().build(), version);
	}

	private static List<JsonNode> apply(String queryText, String input, RuntimeOptions options, Version version) {
		Environment<JsonNode> environment = EnvironmentBuilder.withDefaultLoaders(JSON_PROVIDER, version).build();
		JsonQuery<JsonNode> query = environment.compile(queryText).withRuntimeOptions(options);
		List<JsonNode> out = new ArrayList<>();
		query.apply(JSON_PROVIDER.createString(input), out::add);
		return out;
	}

	private static String repeat(String text, int count) {
		return text.repeat(Math.max(0, count));
	}
}
