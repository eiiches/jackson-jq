package net.thisptr.jackson.jq.internal.functions;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.Preconditions;

@BuiltinFunction("fromjson/0")
public class FromJsonFunction implements Function {
	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Output output) throws JsonQueryException {
		Preconditions.checkInputType("fromjson", in, JsonNodeType.STRING);

		try {
			output.emit(scope.getObjectMapper().readTree(in.asText()));
		} catch (IOException e) {
			throw new JsonQueryException(e);
		}
	}
}
