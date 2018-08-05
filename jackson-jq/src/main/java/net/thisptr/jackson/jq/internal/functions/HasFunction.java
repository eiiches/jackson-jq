package net.thisptr.jackson.jq.internal.functions;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;

@BuiltinFunction("has/1")
public class HasFunction implements Function {
	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Output output) throws JsonQueryException {
		args.get(0).apply(scope, in, (keyName) -> {
			if (in.isObject()) {
				if (!keyName.isTextual())
					throw new JsonQueryException("argument 1 of has() must be string for object input");
				output.emit(BooleanNode.valueOf(in.has(keyName.asText())));
			} else if (in.isArray()) {
				if (!keyName.isIntegralNumber())
					throw new JsonQueryException("argument 1 of has() must be int for array input");
				output.emit(BooleanNode.valueOf(in.has(keyName.asInt())));
			} else {
				throw new JsonQueryException("has() is not applicable to " + in.getNodeType());
			}
		});
	}
}
