package net.thisptr.jackson.jq.internal.functions;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.misc.UnicodeUtils;

@BuiltinFunction("length/0")
public class LengthFunction implements Function {
	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Output output, final Version version) throws JsonQueryException {
		output.emit(length(in));
	}

	public JsonNode length(final JsonNode in) throws JsonQueryException {
		if (in.isTextual()) {
			return IntNode.valueOf(UnicodeUtils.lengthUtf32(in.asText()));
		} else if (in.isArray() || in.isObject()) {
			return new IntNode(in.size());
		} else if (in.isNull()) {
			return new IntNode(0);
		} else if (in.isNumber()) {
			return JsonNodeUtils.asNumericNode(Math.abs(in.asDouble()));
		} else {
			throw new JsonQueryTypeException("%s has no length", in);
		}
	}
}
