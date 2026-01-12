package net.thisptr.jackson.jq.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.thisptr.jackson.jq.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.jackson2.Jackson2JsonProviderImpl;

public class NullLHSFunctionTest {
	@Test
	public void test() throws IOException {
		final ObjectMapper mapper = new ObjectMapper();
		final Scope<JsonNode> scope = Scope.newEmptyScope(Jackson2JsonProviderImpl.getInstance());
		BuiltinFunctionLoader.getInstance().loadFunctions(Versions.JQ_1_5, scope);
		ObjectNode input = mapper.createObjectNode().set("input", mapper.createArrayNode().add(1));
		assertEquals(Arrays.asList(input.deepCopy().set("output", mapper.createArrayNode().add(2))), JsonQueryFunctionTest.eval(scope, ".output+=[.input[0]+1]", input));
	}
}
