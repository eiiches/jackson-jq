package net.thisptr.jackson.jq.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;

import net.thisptr.jackson.jq.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.javacc.ExpressionParser;

public class JsonQueryFunctionTest {
	@Test
	public void test() throws IOException {
		final ObjectMapper mapper = new ObjectMapper();

		final Scope scope = Scope.newEmptyScope();
		BuiltinFunctionLoader.getInstance().loadFunctions(Versions.JQ_1_5, scope);

		scope.addFunction("inc", 1, new JsonQueryFunction("inc", Arrays.asList("x"), new IsolatedScopeQuery(ExpressionParser.compile("x + 1", Versions.JQ_1_5)), scope));
		scope.addFunction("fib", 1, new JsonQueryFunction("fib", Arrays.asList("x"), new IsolatedScopeQuery(ExpressionParser.compile("if x == 0 then 0 elif x == 1 then 1 else fib(x-1) + fib(x-2) end", Versions.JQ_1_5)), scope));
		scope.addFunction("fib", 0, new JsonQueryFunction("fib", Arrays.<String>asList(), new IsolatedScopeQuery(ExpressionParser.compile("fib(.)", Versions.JQ_1_5)), scope));

		assertEquals(Arrays.asList(mapper.readTree("2")), eval(scope, "inc(1)", NullNode.getInstance()));
		assertEquals(Arrays.asList(mapper.readTree("1")), eval(scope, "fib(1)", NullNode.getInstance()));
		assertEquals(Arrays.asList(mapper.readTree("1")), eval(scope, "fib(2)", NullNode.getInstance()));
		assertEquals(Arrays.asList(mapper.readTree("2")), eval(scope, "fib(3)", NullNode.getInstance()));
		assertEquals(Arrays.asList(mapper.readTree("3")), eval(scope, "fib(4)", NullNode.getInstance()));
		assertEquals(Arrays.asList(mapper.readTree("5")), eval(scope, "fib(5)", NullNode.getInstance()));
		assertEquals(Arrays.asList(mapper.readTree("8")), eval(scope, "fib", IntNode.valueOf(6)));
	}

	public static List<JsonNode> eval(final Scope scope, final String q, final JsonNode in) throws JsonQueryException {
		final List<JsonNode> out = new ArrayList<>();
		JsonQuery.compile(q, Versions.JQ_1_5).apply(scope, in, out::add);
		return out;
	}
}
