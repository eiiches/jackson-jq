package net.thisptr.jackson.jq.v2.core.internal.functions.math;

import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.internal.JsonArgumentFunction;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.misc.Preconditions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

@AutoService(Function.class)
@FunctionRegistration(name = "pow", nargs = 2)
public class PowFunction extends JsonArgumentFunction {
	@Override
	protected <JsonNode> JsonNode fn(Scope<JsonNode> scope, List<JsonNode> args, JsonNode in) throws JsonQueryException {
		JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		Preconditions.checkArgumentType(jsonProvider, "pow/2", 0, args.get(0), JsonNodeType.NUMBER);
		Preconditions.checkArgumentType(jsonProvider, "pow/2", 1, args.get(1), JsonNodeType.NUMBER);
		return JsonNodeUtils.asNumericNode(jsonProvider, Math.pow(jsonProvider.asDouble(args.get(0)), jsonProvider.asDouble(args.get(1))));
	}
}
