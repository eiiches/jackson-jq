package net.thisptr.jackson.jq.v2.core.internal.tree.matcher.matchers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.internal.commons.pair.Pair;
import net.thisptr.jackson.jq.v2.core.internal.memory.Memory;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.tree.Comma;
import net.thisptr.jackson.jq.v2.core.internal.tree.literal.ValueLiteral;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.core.version.Versions;
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
				new ObjectMatcher.FieldMatcher<>(false, null, new ValueLiteral<>(JSON_PROVIDER.createString("outer")), new ObjectMatcher<>(JSON_PROVIDER, Arrays.asList(
						new ObjectMatcher.FieldMatcher<>(false, null, new Comma<>(Arrays.<Expression<StackFrame, JsonNode>>asList(new ValueLiteral<>(JSON_PROVIDER.createString("a")), new ValueLiteral<>(JSON_PROVIDER.createString("b")))), new ValueMatcher<>("x"))), Versions.JQ_1_8_2)),
				new ObjectMatcher.FieldMatcher<>(false, null, new ValueLiteral<>(JSON_PROVIDER.createString("c")), new ValueMatcher<>("y"))), Versions.JQ_1_8_2)
				.resolveSlots(slots("x", 3, "y", 5));

		List<List<Pair<Integer, JsonNode>>> matches = new ArrayList<>();

		Deque<PatternMatcher.Match<JsonNode>> accumulator = new ArrayDeque<>();
		matcher.match(new Memory().pushFrame(0), in, (match) -> {
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
				new ObjectMatcher.FieldMatcher<>(false, null, new ValueLiteral<>(JSON_PROVIDER.createString("outer")), new ObjectMatcher<>(JSON_PROVIDER, Arrays.asList(
						new ObjectMatcher.FieldMatcher<>(false, null, new ValueLiteral<>(JSON_PROVIDER.createString("a")), new ValueMatcher<>("x"))), Versions.JQ_1_8_2)),
				new ObjectMatcher.FieldMatcher<>(false, null, new Comma<>(Arrays.<Expression<StackFrame, JsonNode>>asList(new ValueLiteral<>(JSON_PROVIDER.createString("b")), new ValueLiteral<>(JSON_PROVIDER.createString("c")))), new ValueMatcher<>("y"))), Versions.JQ_1_8_2)
				.resolveSlots(slots("x", 7, "y", 11));

		List<List<Pair<Integer, JsonNode>>> matches = new ArrayList<>();

		Deque<PatternMatcher.Match<JsonNode>> accumulator = new ArrayDeque<>();
		matcher.match(new Memory().pushFrame(0), in, (match) -> {
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
