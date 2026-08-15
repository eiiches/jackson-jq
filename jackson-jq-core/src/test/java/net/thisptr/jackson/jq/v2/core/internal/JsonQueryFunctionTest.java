package net.thisptr.jackson.jq.v2.core.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.internal.javacc.ExpressionParser;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.FunctionNameAndArity;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JsonQueryFunctionTest {
	@Test
	@SuppressWarnings("deprecation")
	public void test() throws IOException {
		ObjectMapper mapper = new ObjectMapper();

		Environment<JsonNode> env = new Environment<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_5);
		Scope<JsonNode> scope = Scope.newEmptyScope(Jackson2JsonProviderImpl.getInstance());
		BuiltinFunctionLoader.getInstance().loadFunctions(Versions.JQ_1_5, scope);

		JsonQueryFunction<JsonNode> inc1 = new JsonQueryFunction<>("inc", Arrays.asList("x"), new IsolatedScopeQuery(ExpressionParser.compile("x + 1", Versions.JQ_1_5)), scope);
		JsonQueryFunction<JsonNode> fib1 = new JsonQueryFunction<>("fib", Arrays.asList("x"), new IsolatedScopeQuery(ExpressionParser.compile("if x == 0 then 0 elif x == 1 then 1 else fib(x-1) + fib(x-2) end", Versions.JQ_1_5)), scope);
		JsonQueryFunction<JsonNode> fib0 = new JsonQueryFunction<>("fib", Arrays.<String>asList(), new IsolatedScopeQuery(ExpressionParser.compile("fib(.)", Versions.JQ_1_5)), scope);

		scope.addFunctionFactory("inc", 1, inc1);
		scope.addFunctionFactory("fib", 1, fib1);
		scope.addFunctionFactory("fib", 0, fib0);

		env.addFunctionFactory(FunctionNameAndArity.of("inc", 1), inc1);
		env.addFunctionFactory(FunctionNameAndArity.of("fib", 1), fib1);
		env.addFunctionFactory(FunctionNameAndArity.of("fib", 0), fib0);

		assertEquals(Arrays.asList(mapper.readTree("2")), eval(env, "inc(1)", NullNode.getInstance()));
		assertEquals(Arrays.asList(mapper.readTree("1")), eval(env, "fib(1)", NullNode.getInstance()));
		assertEquals(Arrays.asList(mapper.readTree("1")), eval(env, "fib(2)", NullNode.getInstance()));
		assertEquals(Arrays.asList(mapper.readTree("2")), eval(env, "fib(3)", NullNode.getInstance()));
		assertEquals(Arrays.asList(mapper.readTree("3")), eval(env, "fib(4)", NullNode.getInstance()));
		assertEquals(Arrays.asList(mapper.readTree("5")), eval(env, "fib(5)", NullNode.getInstance()));
		assertEquals(Arrays.asList(mapper.readTree("8")), eval(env, "fib", IntNode.valueOf(6)));
	}

	public static List<JsonNode> eval(Environment<JsonNode> env, String q, JsonNode in) throws JsonQueryException {
		List<JsonNode> out = new ArrayList<>();
		env.compile(q).apply(in, (outNode, path) -> out.add(outNode));
		return out;
	}
}
