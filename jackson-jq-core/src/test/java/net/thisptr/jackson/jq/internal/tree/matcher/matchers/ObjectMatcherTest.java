package net.thisptr.jackson.jq.internal.tree.matcher.matchers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;

import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.internal.javacc.ExpressionParser;
import net.thisptr.jackson.jq.internal.misc.Pair;

public class ObjectMatcherTest {

	@Test
	void test1() throws Exception {
		final JsonNode in = new ObjectMapper().readTree("{\"outer\":{\"a\": 1, \"b\": 2}, \"c\": 3}");
		final ObjectMatcher matcher = new ObjectMatcher(Arrays.asList(
				new ObjectMatcher.FieldMatcher(false, ExpressionParser.compile("\"outer\"", Versions.JQ_1_6), new ObjectMatcher(Arrays.asList(
						new ObjectMatcher.FieldMatcher(false, ExpressionParser.compile("(\"a\",\"b\")", Versions.JQ_1_6), new ValueMatcher("x"))))),
				new ObjectMatcher.FieldMatcher(false, ExpressionParser.compile("\"c\"", Versions.JQ_1_6), new ValueMatcher("y"))));

		final List<List<Pair<String, JsonNode>>> matches = new ArrayList<>();

		final Stack<Pair<String, JsonNode>> accumulator = new Stack<>();
		matcher.match(Scope.newEmptyScope(), in, (match) -> {
			matches.add(new ArrayList<>(match));
		}, accumulator);

		assertEquals(Arrays.asList(
				Arrays.asList(Pair.of("x", IntNode.valueOf(1)), Pair.of("y", IntNode.valueOf(3))),
				Arrays.asList(Pair.of("x", IntNode.valueOf(2)), Pair.of("y", IntNode.valueOf(3)))), matches);
	}

	@Test
	void test2() throws Exception {
		final JsonNode in = new ObjectMapper().readTree("{\"outer\":{\"a\": 1}, \"b\": 2, \"c\": 3}");
		final ObjectMatcher matcher = new ObjectMatcher(Arrays.asList(
				new ObjectMatcher.FieldMatcher(false, ExpressionParser.compile("\"outer\"", Versions.JQ_1_6), new ObjectMatcher(Arrays.asList(
						new ObjectMatcher.FieldMatcher(false, ExpressionParser.compile("\"a\"", Versions.JQ_1_6), new ValueMatcher("x"))))),
				new ObjectMatcher.FieldMatcher(false, ExpressionParser.compile("(\"b\",\"c\")", Versions.JQ_1_6), new ValueMatcher("y"))));

		final List<List<Pair<String, JsonNode>>> matches = new ArrayList<>();

		final Stack<Pair<String, JsonNode>> accumulator = new Stack<>();
		matcher.match(Scope.newEmptyScope(), in, (match) -> {
			matches.add(new ArrayList<>(match));
		}, accumulator);

		assertEquals(Arrays.asList(
				Arrays.asList(Pair.of("x", IntNode.valueOf(1)), Pair.of("y", IntNode.valueOf(2))),
				Arrays.asList(Pair.of("x", IntNode.valueOf(1)), Pair.of("y", IntNode.valueOf(3)))), matches);
	}
}
