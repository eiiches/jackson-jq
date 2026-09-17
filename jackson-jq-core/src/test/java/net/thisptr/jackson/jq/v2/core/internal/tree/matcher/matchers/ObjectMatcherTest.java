package net.thisptr.jackson.jq.v2.core.internal.tree.matcher.matchers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.SlotResolver;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ObjectMatcherTest {
	private static final JsonProvider<JsonNode> JSON_PROVIDER = Jackson2JsonProvider.getInstance();

	@Test
	void test1() throws Exception {
		JsonNode in = new ObjectMapper().readTree("{\"outer\":{\"a\": 1, \"b\": 2}, \"c\": 3}");
		PatternMatcher<JsonNode> matcher = new ObjectMatcher<>(JSON_PROVIDER, Arrays.asList(
				new ObjectMatcher.FieldMatcher<>(false, null, new ValueLiteral<>(JSON_PROVIDER.createString("outer")), new ObjectMatcher<>(JSON_PROVIDER, Arrays.asList(
						new ObjectMatcher.FieldMatcher<>(false, null, new Comma<>(Arrays.<Expression<StackFrame, JsonNode>>asList(new ValueLiteral<>(JSON_PROVIDER.createString("a")), new ValueLiteral<>(JSON_PROVIDER.createString("b")))), new ValueMatcher<>("x"), Memory.NO_OUTPUT_COUNTER)), Versions.JQ_1_8_2), Memory.NO_OUTPUT_COUNTER),
				new ObjectMatcher.FieldMatcher<>(false, null, new ValueLiteral<>(JSON_PROVIDER.createString("c")), new ValueMatcher<>("y"), Memory.NO_OUTPUT_COUNTER)), Versions.JQ_1_8_2)
				.resolveSlots(new SlotResolver(slots("x", 3, "y", 5)));

		assertEquals(Arrays.asList(
				Arrays.<Pair<Integer, Object>>asList(Pair.of(3, IntNode.valueOf(1)), Pair.of(5, IntNode.valueOf(3))),
				Arrays.<Pair<Integer, Object>>asList(Pair.of(3, IntNode.valueOf(2)), Pair.of(5, IntNode.valueOf(3)))), bindingsPerMatch(matcher, in, 3, 5));
	}

	@Test
	void test2() throws Exception {
		JsonNode in = new ObjectMapper().readTree("{\"outer\":{\"a\": 1}, \"b\": 2, \"c\": 3}");
		PatternMatcher<JsonNode> matcher = new ObjectMatcher<>(JSON_PROVIDER, Arrays.asList(
				new ObjectMatcher.FieldMatcher<>(false, null, new ValueLiteral<>(JSON_PROVIDER.createString("outer")), new ObjectMatcher<>(JSON_PROVIDER, Arrays.asList(
						new ObjectMatcher.FieldMatcher<>(false, null, new ValueLiteral<>(JSON_PROVIDER.createString("a")), new ValueMatcher<>("x"), Memory.NO_OUTPUT_COUNTER)), Versions.JQ_1_8_2), Memory.NO_OUTPUT_COUNTER),
				new ObjectMatcher.FieldMatcher<>(false, null, new Comma<>(Arrays.<Expression<StackFrame, JsonNode>>asList(new ValueLiteral<>(JSON_PROVIDER.createString("b")), new ValueLiteral<>(JSON_PROVIDER.createString("c")))), new ValueMatcher<>("y"), Memory.NO_OUTPUT_COUNTER)), Versions.JQ_1_8_2)
				.resolveSlots(new SlotResolver(slots("x", 7, "y", 11)));

		assertEquals(Arrays.asList(
				Arrays.<Pair<Integer, Object>>asList(Pair.of(7, IntNode.valueOf(1)), Pair.of(11, IntNode.valueOf(2))),
				Arrays.<Pair<Integer, Object>>asList(Pair.of(7, IntNode.valueOf(1)), Pair.of(11, IntNode.valueOf(3)))), bindingsPerMatch(matcher, in, 7, 11));
	}

	/**
	 * Runs {@code matcher} and, for each complete match, snapshots what it wrote into the frame at
	 * {@code slots}.
	 */
	private static List<List<Pair<Integer, Object>>> bindingsPerMatch(PatternMatcher<JsonNode> matcher, JsonNode in, int... slots) throws Exception {
		StackFrame frame = new Memory().pushFrame(Arrays.stream(slots).max().getAsInt() + 1);
		List<List<Pair<Integer, Object>>> matches = new ArrayList<>();
		matcher.match(frame, in, () -> {
			List<Pair<Integer, Object>> snapshot = new ArrayList<>();
			for (int slot : slots)
				snapshot.add(Pair.of(slot, Objects.requireNonNull(frame.get(slot), "a complete match must have written every slot")));
			matches.add(snapshot);
		});
		return matches;
	}

	private static Map<String, Integer> slots(String firstName, int firstSlot, String secondName, int secondSlot) {
		Map<String, Integer> slots = new HashMap<>();
		slots.put(firstName, firstSlot);
		slots.put(secondName, secondSlot);
		return slots;
	}
}
