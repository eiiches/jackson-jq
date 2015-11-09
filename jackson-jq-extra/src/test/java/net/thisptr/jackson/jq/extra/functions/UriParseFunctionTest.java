package net.thisptr.jackson.jq.extra.functions;

import java.util.Collections;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.extra.functions.UriParseFunction;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.TextNode;

public class UriParseFunctionTest {
	@Test
	public void test() throws JsonQueryException {
		// check this does not throw NPE
		new UriParseFunction().apply(new Scope(), Collections.<JsonQuery> emptyList(), new TextNode("http://google.com"));
	}
}
