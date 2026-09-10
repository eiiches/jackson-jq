package net.thisptr.jackson.jq.v2.core.internal.builtins.math;

import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.internal.builtins.AbstractPureJsonArgumentFunction;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.Preconditions;
import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

@AutoService(Function.class)
@FunctionRegistration(name = "atan2", nargs = 2)
public class Atan2Function extends AbstractPureJsonArgumentFunction {
	@Override
	protected <JsonNode> JsonNode fn(JsonProvider<JsonNode> jsonProvider, List<JsonNode> args) throws JsonQueryException {
		Preconditions.checkArgumentType(jsonProvider, "atan2/2", 0, args.get(0), JsonNodeType.NUMBER);
		Preconditions.checkArgumentType(jsonProvider, "atan2/2", 1, args.get(1), JsonNodeType.NUMBER);
		return JsonNodeUtils.asNumericNode(jsonProvider, Math.atan2(jsonProvider.getNumberAsDoubleRounded(args.get(0)), jsonProvider.getNumberAsDoubleRounded(args.get(1))));
	}
}
