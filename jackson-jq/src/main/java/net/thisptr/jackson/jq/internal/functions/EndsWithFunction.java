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

@BuiltinFunction("endswith/1")
public class EndsWithFunction implements Function {
	@Override
	public List<JsonNode> apply(final Scope scope, final List<JsonQuery> args, final JsonNode in) throws JsonQueryException {
		Preconditions.checkInputType("endswith", in, JsonNodeType.STRING);

		final String text = in.asText();

		final List<JsonNode> out = new ArrayList<>();
		for (final JsonNode suffix : args.get(0).apply(scope, in)) {
			if (!suffix.isTextual())
				throw new JsonQueryException("1st argument of endswith() must evaluate to string");
			out.add(BooleanNode.valueOf(text.endsWith(suffix.asText())));
		}
		return out;
	}
}
