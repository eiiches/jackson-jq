package net.thisptr.jackson.jq.internal.functions;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

@BuiltinFunction("tonumber/0")
public class ToNumberFunction implements Function {
	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Output output) throws JsonQueryException {
		if (in.isNumber()) {
			output.emit(in);
		} else if (in.isTextual()) {
			final double value = Double.parseDouble(in.asText());
			output.emit(JsonNodeUtils.asNumericNode(value));
		} else {
			throw JsonQueryException.format("%s cannot be parsed as a number", in.getNodeType());
		}
	}
}
