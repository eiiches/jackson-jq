package net.thisptr.jackson.jq.extra.functions;

import java.util.Collections;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.TextNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public class UriParseFunctionTest {
	@Test
	public void test() throws JsonQueryException {
		final Scope scope = Scope.newEmptyScope();
		scope.loadFunctions(Scope.class.getClassLoader());
		// check this does not throw NPE
		new UriParseFunction().apply(scope, Collections.<Expression>emptyList(), new TextNode("http://google.com"), (out) -> {});
	}
}
