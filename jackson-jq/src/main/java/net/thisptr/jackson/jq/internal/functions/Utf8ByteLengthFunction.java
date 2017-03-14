package net.thisptr.jackson.jq.internal.functions;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.UnicodeUtils;

@BuiltinFunction("utf8bytelength/0")
public class Utf8ByteLengthFunction implements Function {
	@Override
	public List<JsonNode> apply(final Scope scope, final List<JsonQuery> args, final JsonNode in) throws JsonQueryException {
		if (!in.isTextual())
			throw JsonQueryException.format("%s (%s) only strings have UTF-8 byte length", in.getNodeType(), in);
		return Collections.<JsonNode> singletonList(IntNode.valueOf(UnicodeUtils.lengthUtf8(in.asText())));
	}
}
