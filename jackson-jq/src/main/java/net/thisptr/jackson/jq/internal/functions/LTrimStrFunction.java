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
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.TextNode;

@BuiltinFunction("ltrimstr/1")
public class LTrimStrFunction implements Function {
	@Override
	public List<JsonNode> apply(final Scope scope, final List<JsonQuery> args, final JsonNode in) throws JsonQueryException {
		Preconditions.checkInputType("ltrimstr", in, JsonNodeType.STRING);

		final String text = in.asText();

		final List<JsonNode> out = new ArrayList<>();
		for (final JsonNode prefixNode : args.get(0).apply(scope, in)) {
			if (!prefixNode.isTextual())
				throw new JsonQueryException("1st argument to ltrimstr() must be string, got " + prefixNode.getNodeType());

			final String prefix = prefixNode.asText();

			if (!text.startsWith(prefix)) {
				out.add(in);
			} else {
				out.add(new TextNode(text.substring(prefix.length(), text.length())));
			}
		}
		return out;
	}
}
