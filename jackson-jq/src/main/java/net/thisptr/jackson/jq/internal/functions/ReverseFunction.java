package net.thisptr.jackson.jq.internal.functions;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;

@BuiltinFunction("reverse/0")
public class ReverseFunction implements Function {
	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Output output, final Version version) throws JsonQueryException {
		final ArrayNode out = scope.getObjectMapper().createArrayNode();

		if (in.isNull()) {
			output.emit(out);
			return;
		}
		if (in.isArray()) {
			for (int i = in.size() - 1; i >= 0; --i)
				out.add(in.get(i));
			output.emit(out);
			return;
		}

		// below are to emulate jq behavior

		if (in.isTextual()) {
			if (in.asText().isEmpty()) {
				output.emit(out);
				return;
			}
			throw JsonQueryException.format("Cannot index %s with number", in.getNodeType());
		}
		if (in.isNumber()) {
			if (in.asDouble() == 0.0) {
				output.emit(out);
				return;
			}
			throw JsonQueryException.format("Cannot index %s with number", in.getNodeType());
		}
		if (in.isObject()) {
			if (in.size() == 0) {
				output.emit(out);
				return;
			}
			throw JsonQueryException.format("Cannot index %s with number", in.getNodeType());
		}
		if (in.isBoolean()) {
			throw new JsonQueryTypeException("%s has no length", in);
		}
		throw new JsonQueryTypeException("%s cannot be reversed", in);
	}
}
