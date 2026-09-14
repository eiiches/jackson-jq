package net.thisptr.jackson.jq.v2.core;

import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.internal.json.comparator.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers {@link JsonQuery#apply(Object)}, the overload that collects the whole output into a list. The
 * streaming overload it delegates to is exercised throughout the rest of the suite.
 */
public class JsonQueryTest {
	/**
	 * Results are compared by jq value, not by JsonNode identity: the node class a literal
	 * compiles to is not what these tests are about.
	 */
	private static final Comparator<JsonNode> BY_JQ_VALUE = new JsonNodeComparator<>(Jackson2JsonProvider.getInstance());

	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final JsonProvider<JsonNode> JSON_PROVIDER = Jackson2JsonProvider.getInstance();
	private static final Environment<JsonNode> ENV = EnvironmentBuilder.withDefaultLoaders(JSON_PROVIDER, Versions.JQ_1_8_2).build();

	@Test
	public void returnsEveryOutputValueInOrder() throws Exception {
		List<JsonNode> out = ENV.compile(".[]").apply(MAPPER.readTree("[1, 2, 3]"));

		assertThat(out).usingElementComparator(BY_JQ_VALUE)
				.containsExactly(MAPPER.readTree("1"), MAPPER.readTree("2"), MAPPER.readTree("3"));
	}

	@Test
	public void returnsAnEmptyListWhenTheQueryProducesNoOutput() {
		List<JsonNode> out = ENV.compile("empty").apply(JSON_PROVIDER.createNull());

		assertThat(out).isEmpty();
	}

	@Test
	public void returnsAFreshListTheCallerMayModify() {
		JsonQuery<JsonNode> query = ENV.compile("1");

		List<JsonNode> first = query.apply(JSON_PROVIDER.createNull());
		first.clear();
		first.add(IntNode.valueOf(99));

		assertThat(query.apply(JSON_PROVIDER.createNull())).usingElementComparator(BY_JQ_VALUE)
				.containsExactly(IntNode.valueOf(1));
	}

	@Test
	public void throwsInsteadOfReturningTheValuesProducedBeforeAFailure() {
		JsonQuery<JsonNode> query = ENV.compile("1, error(\"boom\")");

		assertThatThrownBy(() -> query.apply(JSON_PROVIDER.createNull()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("boom");
	}

	@Test
	public void collectsTheOutputOfAQueryConfiguredWithBindings() {
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(JSON_PROVIDER, Versions.JQ_1_8_2)
				.declareVariable("multiplier")
				.build();
		JsonQuery<JsonNode> query = env.compile("2 * $multiplier");

		RuntimeBindings<JsonNode> bindings = RuntimeBindings.<JsonNode>newBuilder()
				.setVariable("multiplier", IntNode.valueOf(21))
				.build();

		assertThat(query.withRuntimeBindings(bindings).apply(JSON_PROVIDER.createNull()))
				.usingElementComparator(BY_JQ_VALUE)
				.containsExactly(IntNode.valueOf(42));
	}
}
