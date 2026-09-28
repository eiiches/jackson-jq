package net.thisptr.jackson.jq.v2.ext.joni;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.json.internal.io.JsonCodec;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The primitive's argument handling, which the jq-language surface cannot reach: {@code _match_impl}
 * takes its test mode as a third argument, so a call can vary all three and observe the order the
 * combinations are produced in -- and the order a failing argument is reported in relative to the
 * combinations that succeeded.
 */
public class MatchImplFunctionTest {
	private static final JsonProvider<JsonNode> JSON_PROVIDER = Jackson2JsonProvider.getInstance();
	private static final String A_AT_0 = "[{\"offset\":0,\"length\":1,\"string\":\"a\",\"captures\":[]}]";
	private static final String B_AT_1 = "[{\"offset\":1,\"length\":1,\"string\":\"b\",\"captures\":[]}]";
	private static final String A_AT_0_AND_3 = "[{\"offset\":0,\"length\":1,\"string\":\"a\",\"captures\":[]},"
			+ "{\"offset\":3,\"length\":1,\"string\":\"a\",\"captures\":[]}]";
	private static final String B_AT_1_AND_4 = "[{\"offset\":1,\"length\":1,\"string\":\"b\",\"captures\":[]},"
			+ "{\"offset\":4,\"length\":1,\"string\":\"b\",\"captures\":[]}]";

	/**
	 * The test mode is the outermost argument, the flags the next, and the regex the innermost.
	 */
	@Test
	public void everyArgumentCombinationIsProducedInOrder() {
		assertThat(run("\"abcabc\" | impl::_match_impl(\"a\", \"b\"; \"\", \"g\"; false, true)"))
				.containsExactly(A_AT_0, B_AT_1, A_AT_0_AND_3, B_AT_1_AND_4, "true", "true", "true", "true");
	}

	@Test
	public void aFailingRegexIsReportedAfterTheCombinationsThatSucceeded() {
		assertThat(run("try (\"abcabc\" | impl::_match_impl(\"a\", \"b\", error(\"foo\"); \"\", \"g\", error(\"bar\"); false, true, error(\"baz\"))) catch ."))
				.containsExactly(A_AT_0, B_AT_1, "\"foo\"");
	}

	@Test
	public void aFailingFlagsArgumentIsReportedAfterTheCombinationsThatSucceeded() {
		assertThat(run("try (\"abcabc\" | impl::_match_impl(\"a\", \"b\"; \"\", \"g\", error(\"bar\"); false, true, error(\"baz\"))) catch ."))
				.containsExactly(A_AT_0, B_AT_1, A_AT_0_AND_3, B_AT_1_AND_4, "\"bar\"");
	}

	@Test
	public void aFailingTestModeIsReportedAfterTheCombinationsThatSucceeded() {
		assertThat(run("try (\"abcabc\" | impl::_match_impl(\"a\", \"b\"; \"\", \"g\"; false, true, error(\"baz\"))) catch ."))
				.containsExactly(A_AT_0, B_AT_1, A_AT_0_AND_3, B_AT_1_AND_4, "true", "true", "true", "true", "\"baz\"");
	}

	private static List<String> run(String expression) throws JsonQueryException {
		Environment<JsonNode> environment = EnvironmentBuilder.withDefaultLoaders(JSON_PROVIDER, Versions.JQ_1_8_2)
				.importModule(new JoniRegexPrimitives(), "impl")
				.build();
		return environment.compile(expression).apply(JSON_PROVIDER.createNull())
				.stream().map(value -> JsonCodec.format(JSON_PROVIDER, value)).toList();
	}
}
