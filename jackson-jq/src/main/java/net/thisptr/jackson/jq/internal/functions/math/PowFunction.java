package net.thisptr.jackson.jq.internal.functions.math;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.JsonArgumentFunction;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.misc.Preconditions;

@AutoService(Function.class)
@BuiltinFunction("pow/2")
public class PowFunction extends JsonArgumentFunction {
	@Override
	protected JsonNode fn(final List<JsonNode> args, final JsonNode in) throws JsonQueryException {
		Preconditions.checkArgumentType("pow/2", 0, args.get(0), JsonNodeType.NUMBER);
		Preconditions.checkArgumentType("pow/2", 1, args.get(1), JsonNodeType.NUMBER);
		return JsonNodeUtils.asNumericNode(Math.pow(args.get(0).asDouble(), args.get(1).asDouble()));
	}
}