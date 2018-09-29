package net.thisptr.jackson.jq.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.internal.javacc.ExpressionParser;

public class JsonQueryFunctionTest {
	@Test
	public void test() throws IOException {
		final ObjectMapper mapper = new ObjectMapper();

		final Scope scope = Scope.newEmptyScope();
		scope.loadFunctions(Scope.class.getClassLoader(), Versions.JQ_1_5);

		scope.addFunction("inc", 1, new JsonQueryFunction("inc", Arrays.asList("x"), new IsolatedScopeQuery(ExpressionParser.compile("x + 1", Versions.JQ_1_5)), scope));
		scope.addFunction("fib", 1, new JsonQueryFunction("fib", Arrays.asList("x"), new IsolatedScopeQuery(ExpressionParser.compile("if x == 0 then 0 elif x == 1 then 1 else fib(x-1) + fib(x-2) end", Versions.JQ_1_5)), scope));
		scope.addFunction("fib", 0, new JsonQueryFunction("fib", Arrays.<String>asList(), new IsolatedScopeQuery(ExpressionParser.compile("fib(.)", Versions.JQ_1_5)), scope));

		assertEquals(Arrays.asList(mapper.readTree("2")), JsonQuery.compile("inc(1)").apply(scope, NullNode.getInstance()));
		assertEquals(Arrays.asList(mapper.readTree("1")), JsonQuery.compile("fib(1)").apply(scope, NullNode.getInstance()));
		assertEquals(Arrays.asList(mapper.readTree("1")), JsonQuery.compile("fib(2)").apply(scope, NullNode.getInstance()));
		assertEquals(Arrays.asList(mapper.readTree("2")), JsonQuery.compile("fib(3)").apply(scope, NullNode.getInstance()));
		assertEquals(Arrays.asList(mapper.readTree("3")), JsonQuery.compile("fib(4)").apply(scope, NullNode.getInstance()));
		assertEquals(Arrays.asList(mapper.readTree("5")), JsonQuery.compile("fib(5)").apply(scope, NullNode.getInstance()));
		assertEquals(Arrays.asList(mapper.readTree("8")), JsonQuery.compile("fib").apply(scope, IntNode.valueOf(6)));
	}
}
