package net.thisptr.jackson.jq.internal.functions;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;

@BuiltinFunction("utf8bytelength/0")
public class Utf8ByteLengthFunction implements Function {
	@Override
	public List<JsonNode> apply(final Scope scope, final List<JsonQuery> args, final JsonNode in) throws JsonQueryException {
		if (!in.isTextual())
			throw JsonQueryException.format("%s (%s) only strings have UTF-8 byte length", in.getNodeType(), in);
		return Collections.<JsonNode> singletonList(IntNode.valueOf(length(in.asText())));
	}

	private static int length(final String in) throws JsonQueryException {
		// TODO: implement without creating an array
		return in.getBytes(StandardCharsets.UTF_8).length;
	}
}
