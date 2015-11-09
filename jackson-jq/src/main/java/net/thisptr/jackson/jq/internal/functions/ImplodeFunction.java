package net.thisptr.jackson.jq.internal.functions;

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
import com.fasterxml.jackson.databind.node.TextNode;

@BuiltinFunction("implode/0")
public class ImplodeFunction implements Function {
	@Override
	public List<JsonNode> apply(final Scope scope, final List<JsonQuery> args, final JsonNode in) throws JsonQueryException {
		Preconditions.checkInputArrayType("implode", in, JsonNodeType.NUMBER);

		final StringBuilder builder = new StringBuilder();
		for (final JsonNode ch : in) {
			if (ch.canConvertToInt()) {
				builder.append((char) ch.asInt());
			} else {
				throw new JsonQueryException("input to implode() must be a list of codepoints; " + ch.getNodeType() + " found");
			}
		}
		return Collections.<JsonNode> singletonList(new TextNode(builder.toString()));
	}
}
