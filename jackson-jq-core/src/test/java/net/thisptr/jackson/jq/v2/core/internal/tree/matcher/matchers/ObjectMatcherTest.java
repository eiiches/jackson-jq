package net.thisptr.jackson.jq.v2.core.internal.tree.matcher.matchers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.internal.misc.Pair;
import net.thisptr.jackson.jq.v2.core.internal.tree.Tuple;
import net.thisptr.jackson.jq.v2.core.internal.tree.literal.StringLiteral;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Expression;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ObjectMatcherTest {
	private static final JsonProvider<JsonNode> JSON_PROVIDER = Jackson2JsonProviderImpl.getInstance();

	@Test
	void test1() throws Exception {
		JsonNode in = new ObjectMapper().readTree("{\"outer\":{\"a\": 1, \"b\": 2}, \"c\": 3}");
		PatternMatcher<JsonNode> matcher = new ObjectMatcher<>(JSON_PROVIDER, Arrays.asList(
				new ObjectMatcher.FieldMatcher<>(false, new StringLiteral<>(JSON_PROVIDER, "outer"), new ObjectMatcher<>(JSON_PROVIDER, Arrays.asList(
						new ObjectMatcher.FieldMatcher<>(false, new Tuple<>(Arrays.<Expression<JsonNode>>asList(new StringLiteral<>(JSON_PROVIDER, "a"), new StringLiteral<>(JSON_PROVIDER, "b"))), new ValueMatcher<>("x"))))),
				new ObjectMatcher.FieldMatcher<>(false, new StringLiteral<>(JSON_PROVIDER, "c"), new ValueMatcher<>("y"))))
				.resolveSlots(slots("x", 3, "y", 5));

		List<List<Pair<Integer, JsonNode>>> matches = new ArrayList<>();

		Stack<PatternMatcher.Match<JsonNode>> accumulator = new Stack<>();
		matcher.match(null, in, (match) -> {
			List<Pair<Integer, JsonNode>> copy = new ArrayList<>();
			for (PatternMatcher.Match<JsonNode> item : match)
				copy.add(Pair.of(item.slot, item.value));
			matches.add(copy);
		}, accumulator);

		assertEquals(Arrays.asList(
				Arrays.asList(Pair.of(3, IntNode.valueOf(1)), Pair.of(5, IntNode.valueOf(3))),
				Arrays.asList(Pair.of(3, IntNode.valueOf(2)), Pair.of(5, IntNode.valueOf(3)))), matches);
	}

	@Test
	void test2() throws Exception {
		JsonNode in = new ObjectMapper().readTree("{\"outer\":{\"a\": 1}, \"b\": 2, \"c\": 3}");
		PatternMatcher<JsonNode> matcher = new ObjectMatcher<>(JSON_PROVIDER, Arrays.asList(
				new ObjectMatcher.FieldMatcher<>(false, new StringLiteral<>(JSON_PROVIDER, "outer"), new ObjectMatcher<>(JSON_PROVIDER, Arrays.asList(
						new ObjectMatcher.FieldMatcher<>(false, new StringLiteral<>(JSON_PROVIDER, "a"), new ValueMatcher<>("x"))))),
				new ObjectMatcher.FieldMatcher<>(false, new Tuple<>(Arrays.<Expression<JsonNode>>asList(new StringLiteral<>(JSON_PROVIDER, "b"), new StringLiteral<>(JSON_PROVIDER, "c"))), new ValueMatcher<>("y"))))
				.resolveSlots(slots("x", 7, "y", 11));

		List<List<Pair<Integer, JsonNode>>> matches = new ArrayList<>();

		Stack<PatternMatcher.Match<JsonNode>> accumulator = new Stack<>();
		matcher.match(null, in, (match) -> {
			List<Pair<Integer, JsonNode>> copy = new ArrayList<>();
			for (PatternMatcher.Match<JsonNode> item : match)
				copy.add(Pair.of(item.slot, item.value));
			matches.add(copy);
		}, accumulator);

		assertEquals(Arrays.asList(
				Arrays.asList(Pair.of(7, IntNode.valueOf(1)), Pair.of(11, IntNode.valueOf(2))),
				Arrays.asList(Pair.of(7, IntNode.valueOf(1)), Pair.of(11, IntNode.valueOf(3)))), matches);
	}

	private static Map<String, Integer> slots(String firstName, int firstSlot, String secondName, int secondSlot) {
		Map<String, Integer> slots = new HashMap<>();
		slots.put(firstName, firstSlot);
		slots.put(secondName, secondSlot);
		return slots;
	}
}
