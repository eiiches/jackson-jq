package net.thisptr.jackson.jq.internal.functions;

import java.util.Collections;
import java.util.List;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

@BuiltinFunction("tostring/0")
public class ToStringFunction implements Function {
	@Override
	public List<JsonNode> apply(final Scope scope, final List<JsonQuery> args, final JsonNode in) throws JsonQueryException {
		if (in.isTextual()) {
			return Collections.singletonList(in);
		} else {
			return Collections.singletonList((JsonNode) new TextNode(in.toString()));
		}
	}
}
