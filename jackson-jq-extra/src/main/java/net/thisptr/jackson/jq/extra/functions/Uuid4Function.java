package net.thisptr.jackson.jq.extra.functions;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

@BuiltinFunction("uuid4/0")
public class Uuid4Function implements Function {
	@Override
	public List<JsonNode> apply(Scope scope, List<JsonQuery> args, JsonNode in) throws JsonQueryException {
		return Collections.<JsonNode> singletonList(new TextNode(UUID.randomUUID().toString()));
	}
}
