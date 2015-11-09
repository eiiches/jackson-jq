package net.thisptr.jackson.jq.internal.functions;

import java.util.ArrayList;
import java.util.List;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;

@BuiltinFunction("has/1")
public class HasFunction implements Function {
	@Override
	public List<JsonNode> apply(final Scope scope, final List<JsonQuery> args, final JsonNode in) throws JsonQueryException {
		final List<JsonNode> out = new ArrayList<>();
		for (final JsonNode keyName : args.get(0).apply(scope, in)) {
			if (in.isObject()) {
				if (!keyName.isTextual())
					throw new JsonQueryException("argument 1 of has() must be string for object input");
				out.add((JsonNode) BooleanNode.valueOf(in.has(keyName.asText())));
			} else if (in.isArray()) {
				if (!keyName.isIntegralNumber())
					throw new JsonQueryException("argument 1 of has() must be int for array input");
				out.add((JsonNode) BooleanNode.valueOf(in.has(keyName.asInt())));
			} else {
				throw new JsonQueryException("has() is not applicable to " + in.getNodeType());
			}
		}

		return out;
	}
}
