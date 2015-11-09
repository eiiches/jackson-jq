package net.thisptr.jackson.jq.internal;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.internal.JsonQueryFunction;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;

public class JsonQueryFunctionTest {
	@Test
	public void test() throws JsonProcessingException, IOException {
		final Scope scope = new Scope();
		final ObjectMapper mapper = new ObjectMapper();

		scope.addFunction("inc", 1, new JsonQueryFunction("inc", Arrays.asList("x"), JsonQuery.compile("x + 1")));
		scope.addFunction("fib", 1, new JsonQueryFunction("fib", Arrays.asList("x"), JsonQuery.compile("if x == 0 then 0 elif x == 1 then 1 else fib(x-1) + fib(x-2) end")));
		scope.addFunction("fib", 0, new JsonQueryFunction("fib", Arrays.<String> asList(), JsonQuery.compile("fib(.)"), scope));

		assertEquals(Arrays.asList(mapper.readTree("2")), JsonQuery.compile("inc(1)").apply(scope, NullNode.getInstance()));
		assertEquals(Arrays.asList(mapper.readTree("1")), JsonQuery.compile("fib(1)").apply(scope, NullNode.getInstance()));
		assertEquals(Arrays.asList(mapper.readTree("1")), JsonQuery.compile("fib(2)").apply(scope, NullNode.getInstance()));
		assertEquals(Arrays.asList(mapper.readTree("2")), JsonQuery.compile("fib(3)").apply(scope, NullNode.getInstance()));
		assertEquals(Arrays.asList(mapper.readTree("3")), JsonQuery.compile("fib(4)").apply(scope, NullNode.getInstance()));
		assertEquals(Arrays.asList(mapper.readTree("5")), JsonQuery.compile("fib(5)").apply(scope, NullNode.getInstance()));
		assertEquals(Arrays.asList(mapper.readTree("8")), JsonQuery.compile("fib").apply(scope, IntNode.valueOf(6)));
	}
}
