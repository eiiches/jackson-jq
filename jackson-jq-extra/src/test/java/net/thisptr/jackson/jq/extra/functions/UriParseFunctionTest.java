package net.thisptr.jackson.jq.extra.functions;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.TextNode;

import net.thisptr.jackson.jq.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public class UriParseFunctionTest {
	@Test
	public void test() throws JsonQueryException {
		final Scope scope = Scope.newEmptyScope();
		BuiltinFunctionLoader.getInstance().loadFunctions(Versions.JQ_1_5, scope);
		// check this does not throw NPE
		new UriParseFunction().apply(scope, Collections.<Expression>emptyList(), new TextNode("http://google.com"), null, (out, opath) -> {}, Versions.JQ_1_5);
	}
}
