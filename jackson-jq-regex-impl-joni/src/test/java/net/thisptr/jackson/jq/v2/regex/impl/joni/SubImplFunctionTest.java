package net.thisptr.jackson.jq.v2.regex.impl.joni;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;

import static org.assertj.core.api.Assertions.assertThat;

public class SubImplFunctionTest {
	private static final JsonProvider<JsonNode> JSON_PROVIDER = Jackson2JsonProviderImpl.getInstance();

	@Test
	public void gsubUsesCaptureObjectsForReplacementExpression() throws Exception {
		List<JsonNode> out = apply("gsub(\"(?<d>\\\\d)\"; \"\\(.d|tonumber+1)\")", "a1b2");

		assertThat(out).extracting(JSON_PROVIDER::getString).containsExactly("a2b3");
	}

	@Test
	public void gsubPreservesMultipleReplacementOutputs() throws Exception {
		List<JsonNode> out = apply("gsub(\"a\"; \"x\", \"y\")", "aa");

		assertThat(out).extracting(JSON_PROVIDER::getString).containsExactly("xx", "yx", "xy", "yy");
	}

	@Test
	public void gsubHandlesManyMatchesWithoutStackOverflow() throws Exception {
		List<JsonNode> out = apply("gsub(\"a\"; \"\")", repeat("a", 10000));

		assertThat(out).extracting(JSON_PROVIDER::getString).containsExactly("");
	}

	@Test
	public void subEmitsSuccessfulReplacementBranchesBeforeReplacementError() throws Exception {
		List<JsonNode> out = apply("try sub(\"a\"; \"1\", \"2\", error(\"bar\"); \"g\") catch .", "abcabc");

		assertThat(out).extracting(JSON_PROVIDER::getString).containsExactly("1bc1bc", "2bc1bc", "bar");
	}

	@Test
	public void subEmitsSuccessfulReplacementBranchesBeforePatternError() throws Exception {
		List<JsonNode> out = apply("try sub(\"a\", \"b\", error(\"foo\"); \"1\", \"2\", error(\"bar\"); \"\", \"g\", error(\"baz\")) catch .", "abcabc");

		assertThat(out).extracting(JSON_PROVIDER::getString).containsExactly("1bcabc", "2bcabc", "bar");
	}

	@Test
	public void subEmitsSuccessfulReplacementBranchesBeforeFlagsError() throws Exception {
		List<JsonNode> out = apply("try sub(\"a\", \"b\", error(\"foo\"); \"1\", \"2\"; \"\", \"g\", error(\"baz\")) catch .", "abcabc");

		assertThat(out).extracting(JSON_PROVIDER::getString).containsExactly("1bcabc", "2bcabc", "1bcabc", "2bcabc", "baz");
	}

	private static List<JsonNode> apply(String queryText, String input) throws Exception {
		Environment<JsonNode> environment = new EnvironmentBuilder<>(JSON_PROVIDER, Versions.JQ_1_8_2).build();
		JsonQuery<JsonNode> query = environment.compile(queryText);
		List<JsonNode> out = new ArrayList<>();
		query.apply(JSON_PROVIDER.createString(input), out::add);
		return out;
	}

	private static String repeat(String text, int count) {
		StringBuilder result = new StringBuilder(text.length() * count);
		for (int i = 0; i < count; ++i) {
			result.append(text);
		}
		return result.toString();
	}
}
