package net.thisptr.jackson.jq.v2.core.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JsonQueryFunctionTest {
	@Test
	public void test() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		Environment<JsonNode> env = new Environment<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_5);
		BuiltinFunctionLoader.getInstance().listFunctionFactories(Versions.JQ_1_5).forEach(env::addFunctionFactory);

		assertEquals(Arrays.asList(mapper.readTree("2")), eval(env, "def inc(x): x + 1; inc(1)", NullNode.getInstance()));
		assertEquals(Arrays.asList(mapper.readTree("5")), eval(env, "def fib(x): if x == 0 then 0 elif x == 1 then 1 else fib(x-1) + fib(x-2) end; fib(5)", NullNode.getInstance()));
		assertEquals(Arrays.asList(mapper.readTree("[1,100,2100,100,2100]")), eval(env, "def id(x):x; 2000 as $x | def f(x):1 as $x | id([$x, x, x]); def g(x): 100 as $x | f($x,$x+x); g($x)", mapper.readTree("\"more testing\"")));
	}

	@Test
	public void twoHopNestedVariableCapture() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		Environment<JsonNode> env = new Environment<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_5);
		BuiltinFunctionLoader.getInstance().listFunctionFactories(Versions.JQ_1_5).forEach(env::addFunctionFactory);

		assertEquals(Arrays.asList(mapper.readTree("1")), eval(env, "def a($x): def b: def c: $x; c; b; a(1)", NullNode.getInstance()));
	}

	@Test
	public void twoHopNestedFunctionCapture() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		Environment<JsonNode> env = new Environment<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_5);
		BuiltinFunctionLoader.getInstance().listFunctionFactories(Versions.JQ_1_5).forEach(env::addFunctionFactory);

		assertEquals(Arrays.asList(mapper.readTree("2")), eval(env, "def a(f): def b: def c: f; c; b; a(1+1)", NullNode.getInstance()));
	}

	@Test
	public void selfRecursiveFunctionNestedInsideAnotherDef() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		Environment<JsonNode> env = new Environment<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_5);
		BuiltinFunctionLoader.getInstance().listFunctionFactories(Versions.JQ_1_5).forEach(env::addFunctionFactory);

		assertEquals(Arrays.asList(mapper.readTree("120")),
				eval(env, "def outer: def fact: if . <= 1 then 1 else . * ((. - 1) | fact) end; 5 | fact; outer", NullNode.getInstance()));
	}

	public static List<JsonNode> eval(Environment<JsonNode> env, String q, JsonNode in) throws JsonQueryException {
		List<JsonNode> out = new ArrayList<>();
		env.compile(q).apply(in, (outNode, path) -> out.add(outNode));
		return out;
	}
}
