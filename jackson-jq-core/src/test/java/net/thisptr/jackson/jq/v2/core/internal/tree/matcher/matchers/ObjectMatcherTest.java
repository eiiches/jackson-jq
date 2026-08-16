package net.thisptr.jackson.jq.v2.core.internal.tree.matcher.matchers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.internal.misc.Pair;
import net.thisptr.jackson.jq.v2.core.internal.tree.Tuple;
import net.thisptr.jackson.jq.v2.core.internal.tree.literal.StringLiteral;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Expression;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ObjectMatcherTest {
	private static final JsonProvider<JsonNode> JSON_PROVIDER = Jackson2JsonProviderImpl.getInstance();

	@Test
	void test1() throws Exception {
		JsonNode in = new ObjectMapper().readTree("{\"outer\":{\"a\": 1, \"b\": 2}, \"c\": 3}");
		ObjectMatcher<JsonNode> matcher = new ObjectMatcher<>(JSON_PROVIDER, Arrays.asList(
				new ObjectMatcher.FieldMatcher<>(false, new StringLiteral<>(JSON_PROVIDER, "outer"), new ObjectMatcher<>(JSON_PROVIDER, Arrays.asList(
						new ObjectMatcher.FieldMatcher<>(false, new Tuple<>(Arrays.<Expression<JsonNode>>asList(new StringLiteral<>(JSON_PROVIDER, "a"), new StringLiteral<>(JSON_PROVIDER, "b"))), new ValueMatcher<>("x"))))),
				new ObjectMatcher.FieldMatcher<>(false, new StringLiteral<>(JSON_PROVIDER, "c"), new ValueMatcher<>("y"))));

		List<List<Pair<String, JsonNode>>> matches = new ArrayList<>();

		Stack<Pair<String, JsonNode>> accumulator = new Stack<>();
		matcher.match(null, in, (match) -> {
			matches.add(new ArrayList<>(match));
		}, accumulator);

		assertEquals(Arrays.asList(
				Arrays.asList(Pair.of("x", IntNode.valueOf(1)), Pair.of("y", IntNode.valueOf(3))),
				Arrays.asList(Pair.of("x", IntNode.valueOf(2)), Pair.of("y", IntNode.valueOf(3)))), matches);
	}

	@Test
	void test2() throws Exception {
		JsonNode in = new ObjectMapper().readTree("{\"outer\":{\"a\": 1}, \"b\": 2, \"c\": 3}");
		ObjectMatcher<JsonNode> matcher = new ObjectMatcher<>(JSON_PROVIDER, Arrays.asList(
				new ObjectMatcher.FieldMatcher<>(false, new StringLiteral<>(JSON_PROVIDER, "outer"), new ObjectMatcher<>(JSON_PROVIDER, Arrays.asList(
						new ObjectMatcher.FieldMatcher<>(false, new StringLiteral<>(JSON_PROVIDER, "a"), new ValueMatcher<>("x"))))),
				new ObjectMatcher.FieldMatcher<>(false, new Tuple<>(Arrays.<Expression<JsonNode>>asList(new StringLiteral<>(JSON_PROVIDER, "b"), new StringLiteral<>(JSON_PROVIDER, "c"))), new ValueMatcher<>("y"))));

		List<List<Pair<String, JsonNode>>> matches = new ArrayList<>();

		Stack<Pair<String, JsonNode>> accumulator = new Stack<>();
		matcher.match(null, in, (match) -> {
			matches.add(new ArrayList<>(match));
		}, accumulator);

		assertEquals(Arrays.asList(
				Arrays.asList(Pair.of("x", IntNode.valueOf(1)), Pair.of("y", IntNode.valueOf(2))),
				Arrays.asList(Pair.of("x", IntNode.valueOf(1)), Pair.of("y", IntNode.valueOf(3)))), matches);
	}
}
