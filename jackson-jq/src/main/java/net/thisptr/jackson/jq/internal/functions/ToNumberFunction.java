package net.thisptr.jackson.jq.internal.functions;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

@BuiltinFunction("tonumber/0")
public class ToNumberFunction implements Function {
	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Output output, final Version version) throws JsonQueryException {
		if (in.isNumber()) {
			output.emit(in);
		} else if (in.isTextual()) {
			try {
				final double value = Double.parseDouble(in.asText());
				output.emit(JsonNodeUtils.asNumericNode(value));
			} catch (final NumberFormatException e) {
				throw new JsonQueryException(e);
			}
		} else {
			throw new JsonQueryTypeException("%s cannot be parsed as a number", in);
		}
	}
}
