package net.thisptr.jackson.jq.internal.functions;

import java.util.Collections;
import java.util.List;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

import com.fasterxml.jackson.databind.JsonNode;

@BuiltinFunction("tonumber/0")
public class ToNumberFunction implements Function {
	@Override
	public List<JsonNode> apply(final Scope scope, final List<JsonQuery> args, final JsonNode in) throws JsonQueryException {
		if (in.isNumber()) {
			return Collections.singletonList(in);
		} else if (in.isTextual()) {
			final double value;
			try {
				value = Double.parseDouble(in.asText());
			} catch (final NumberFormatException e) {
				throw new JsonQueryException(e);
			}
			return Collections.singletonList(JsonNodeUtils.asNumericNode(value));
		} else {
			throw JsonQueryException.format("%s cannot be parsed as a number", in.getNodeType());
		}
	}
}
