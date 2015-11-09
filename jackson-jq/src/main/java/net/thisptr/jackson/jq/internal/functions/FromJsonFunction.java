package net.thisptr.jackson.jq.internal.functions;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.Preconditions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

@BuiltinFunction("fromjson/0")
public class FromJsonFunction implements Function {
	@Override
	public List<JsonNode> apply(Scope scope, List<JsonQuery> args, JsonNode in) throws JsonQueryException {
		Preconditions.checkInputType("fromjson", in, JsonNodeType.STRING);

		try {
			return Collections.<JsonNode> singletonList(scope.getObjectMapper().readTree(in.asText()));
		} catch (IOException e) {
			throw new JsonQueryException(e);
		}
	}
}
