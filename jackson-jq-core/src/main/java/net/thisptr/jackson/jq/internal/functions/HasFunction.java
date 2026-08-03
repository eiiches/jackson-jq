package net.thisptr.jackson.jq.internal.functions;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
@BuiltinFunction("has/1")
public class HasFunction implements Function {
	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path ipath, final PathOutput output, final Version version) throws JsonQueryException {
		if (in.isNull()) {
			output.emit(BooleanNode.FALSE, null);
			return;
		}
		args.get(0).apply(scope, in, (keyName) -> {
			if (in.isObject()) {
				if (!keyName.isTextual())
					throw new JsonQueryException("argument 1 of has() must be string for object input");
				output.emit(BooleanNode.valueOf(in.has(keyName.asText())), null);
			} else if (in.isArray()) {
				if (!keyName.isIntegralNumber())
					throw new JsonQueryException("argument 1 of has() must be int for array input");
				output.emit(BooleanNode.valueOf(in.has(keyName.asInt())), null);
			} else {
				throw new JsonQueryException("has() is not applicable to " + in.getNodeType());
			}
		});
	}
}
