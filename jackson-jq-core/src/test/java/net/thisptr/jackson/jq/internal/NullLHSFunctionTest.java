package net.thisptr.jackson.jq.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.management.ObjectName;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.thisptr.jackson.jq.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.javacc.ExpressionParser;

public class NullLHSFunctionTest {
	@Test
	public void test() throws IOException {
		final ObjectMapper mapper = new ObjectMapper();
		final Scope scope = Scope.newEmptyScope();
		BuiltinFunctionLoader.getInstance().loadFunctions(Versions.JQ_1_5, scope);
		ObjectNode input = mapper.createObjectNode().set("input", mapper.createArrayNode().add(1));
		assertEquals(Arrays.asList(input.deepCopy().set("output", mapper.createArrayNode().add(2))), JsonQueryFunctionTest.eval(scope, ".output+=[.input[0]+1]", input));
	}
}
