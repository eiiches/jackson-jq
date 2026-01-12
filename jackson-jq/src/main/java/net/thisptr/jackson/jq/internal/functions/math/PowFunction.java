package net.thisptr.jackson.jq.internal.functions.math;

import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.JsonArgumentFunction;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.misc.Preconditions;

@AutoService(Function.class)
@BuiltinFunction("pow/2")
public class PowFunction<JsonNode> extends JsonArgumentFunction<JsonNode> {
	@Override
	protected JsonNode fn(final Scope<JsonNode> scope, final List<JsonNode> args, final JsonNode in) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		Preconditions.checkArgumentType(jsonProvider, "pow/2", 0, args.get(0), JsonNodeType.NUMBER);
		Preconditions.checkArgumentType(jsonProvider, "pow/2", 1, args.get(1), JsonNodeType.NUMBER);
		return JsonNodeUtils.asNumericNode(jsonProvider, Math.pow(jsonProvider.asDouble(args.get(0)), jsonProvider.asDouble(args.get(1))));
	}
}
