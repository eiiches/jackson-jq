package net.thisptr.jackson.jq.internal.functions;

import java.util.ArrayList;
import java.util.List;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.Preconditions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

@BuiltinFunction("startswith/1")
public class StartsWithFunction implements Function {
	@Override
	public List<JsonNode> apply(Scope scope, List<JsonQuery> args, JsonNode in) throws JsonQueryException {
		Preconditions.checkInputType("startswith", in, JsonNodeType.STRING);

		final String text = in.asText();

		final List<JsonNode> out = new ArrayList<>();
		for (final JsonNode prefix : args.get(0).apply(scope, in)) {
			if (!prefix.isTextual())
				throw new JsonQueryException("1st argument of startswith() must evaluate to string");
			out.add(BooleanNode.valueOf(text.startsWith(prefix.asText())));
		}
		return out;
	}
}
