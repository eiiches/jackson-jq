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
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Expression;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ObjectMatcherTest {

	@Test
	void test1() throws Exception {
		JsonNode in = new ObjectMapper().readTree("{\"outer\":{\"a\": 1, \"b\": 2}, \"c\": 3}");
		ObjectMatcher<JsonNode> matcher = new ObjectMatcher<>(Arrays.asList(
				new ObjectMatcher.FieldMatcher<>(false, new StringLiteral("outer"), new ObjectMatcher<>(Arrays.asList(
						new ObjectMatcher.FieldMatcher<>(false, new Tuple(Arrays.<Expression>asList(new StringLiteral("a"), new StringLiteral("b"))), new ValueMatcher<>("x"))))),
				new ObjectMatcher.FieldMatcher<>(false, new StringLiteral("c"), new ValueMatcher<>("y"))));

		List<List<Pair<String, JsonNode>>> matches = new ArrayList<>();

		Stack<Pair<String, JsonNode>> accumulator = new Stack<>();
		matcher.match(Jackson2JsonProviderImpl.getInstance(), null, in, (match) -> {
			matches.add(new ArrayList<>(match));
		}, accumulator);

		assertEquals(Arrays.asList(
				Arrays.asList(Pair.of("x", IntNode.valueOf(1)), Pair.of("y", IntNode.valueOf(3))),
				Arrays.asList(Pair.of("x", IntNode.valueOf(2)), Pair.of("y", IntNode.valueOf(3)))), matches);
	}

	@Test
	void test2() throws Exception {
		JsonNode in = new ObjectMapper().readTree("{\"outer\":{\"a\": 1}, \"b\": 2, \"c\": 3}");
		ObjectMatcher<JsonNode> matcher = new ObjectMatcher<>(Arrays.asList(
				new ObjectMatcher.FieldMatcher<>(false, new StringLiteral("outer"), new ObjectMatcher<>(Arrays.asList(
						new ObjectMatcher.FieldMatcher<>(false, new StringLiteral("a"), new ValueMatcher<>("x"))))),
				new ObjectMatcher.FieldMatcher<>(false, new Tuple(Arrays.<Expression>asList(new StringLiteral("b"), new StringLiteral("c"))), new ValueMatcher<>("y"))));

		List<List<Pair<String, JsonNode>>> matches = new ArrayList<>();

		Stack<Pair<String, JsonNode>> accumulator = new Stack<>();
		matcher.match(Jackson2JsonProviderImpl.getInstance(), null, in, (match) -> {
			matches.add(new ArrayList<>(match));
		}, accumulator);

		assertEquals(Arrays.asList(
				Arrays.asList(Pair.of("x", IntNode.valueOf(1)), Pair.of("y", IntNode.valueOf(2))),
				Arrays.asList(Pair.of("x", IntNode.valueOf(1)), Pair.of("y", IntNode.valueOf(3)))), matches);
	}
}
