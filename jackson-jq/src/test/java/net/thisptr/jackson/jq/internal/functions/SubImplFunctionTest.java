package net.thisptr.jackson.jq.internal.functions;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import net.thisptr.jackson.jq.DefaultRootScope;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Version;
import org.junit.jupiter.api.Test;

public class SubImplFunctionTest {
	@Test
	public void gsubUsesCaptureObjectsForReplacementExpression() throws Exception {
		final List<JsonNode> out = apply("gsub(\"(?<d>\\\\d)\"; \"\\(.d|tonumber+1)\")", "a1b2");

		assertThat(out).containsExactly(TextNode.valueOf("a2b3"));
	}

	@Test
	public void gsubPreservesMultipleReplacementOutputs() throws Exception {
		final List<JsonNode> out = apply("gsub(\"a\"; \"x\", \"y\")", "aa");

		assertThat(out).containsExactly(
				TextNode.valueOf("xx"),
				TextNode.valueOf("xy"),
				TextNode.valueOf("yx"),
				TextNode.valueOf("yy"));
	}

	@Test
	public void gsubHandlesManyMatchesWithoutStackOverflow() throws Exception {
		final List<JsonNode> out = apply("gsub(\"a\"; \"\")", repeat("a", 10000));

		assertThat(out).containsExactly(TextNode.valueOf(""));
	}

	private static List<JsonNode> apply(final String queryText, final String input) throws Exception {
		final JsonQuery query = JsonQuery.compile(queryText, Version.LATEST);
		final List<JsonNode> out = new ArrayList<>();
		query.apply(DefaultRootScope.getInstance(Version.LATEST), TextNode.valueOf(input), out::add);
		return out;
	}

	private static String repeat(final String text, final int count) {
		final StringBuilder result = new StringBuilder(text.length() * count);
		for (int i = 0; i < count; ++i) {
			result.append(text);
		}
		return result.toString();
	}
}
