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

@BuiltinFunction("rtrimstr/1")
public class RTrimStrFunction implements Function {
	@Override
	public List<JsonNode> apply(final Scope scope, final List<JsonQuery> args, final JsonNode in) throws JsonQueryException {
		Preconditions.checkInputType("rtrimstr", in, JsonNodeType.STRING);

		final String text = in.asText();

		final List<JsonNode> out = new ArrayList<>();
		for (final JsonNode suffixNode : args.get(0).apply(scope, in)) {
			if (!suffixNode.isTextual())
				throw new JsonQueryException("1st argument to rtrimstr() must be string, got " + suffixNode.getNodeType());

			final String suffix = suffixNode.asText();

			if (!text.endsWith(suffix)) {
				out.add(in);
			} else {
				out.add(new TextNode(text.substring(0, text.length() - suffix.length())));
			}
		}
		return out;
	}
}
