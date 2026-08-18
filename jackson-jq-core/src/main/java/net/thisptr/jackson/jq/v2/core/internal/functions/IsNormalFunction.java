package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import com.google.auto.service.AutoService;
import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

@AutoService(Function.class)
@FunctionRegistration(name = "isnormal", nargs = 0)
public class IsNormalFunction implements Function {
	@Override
	public <JsonNode> Expression<JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<JsonNode>> args, Version version) {
		return (scope, in, ipath, output, ignoredRequirePath) -> {

				@Var boolean result = false;
		if (jsonProvider.getNodeType(in) == JsonNodeType.NUMBER) {
			double v = jsonProvider.asDouble(in);
			result = !Double.isInfinite(v) && (v <= -Double.MIN_NORMAL || Double.MIN_NORMAL <= v);
		}
		output.emit(jsonProvider.createBoolean(result), null);
		};
}
}
