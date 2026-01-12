package net.thisptr.jackson.jq.extra.functions;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.jackson2.Jackson2JsonProviderImpl;

public class UriParseFunctionTest {
	@Test
	public void test() throws JsonQueryException {
		final Scope<JsonNode> scope = Scope.newEmptyScope(Jackson2JsonProviderImpl.getInstance());
		BuiltinFunctionLoader.getInstance().loadFunctions(Versions.JQ_1_5, scope);
		// check this does not throw NPE
		new UriParseFunction<JsonNode>().apply(scope, Collections.<Expression<JsonNode>>emptyList(), scope.jsonProvider().createString("http://google.com"), null, (out, opath) -> {}, Versions.JQ_1_5);
	}
}
