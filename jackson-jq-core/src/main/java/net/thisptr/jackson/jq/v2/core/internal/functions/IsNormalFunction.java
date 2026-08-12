package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import com.google.auto.service.AutoService;
import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

@AutoService(Function.class)
@FunctionRegistration("isnormal/0")
public class IsNormalFunction implements Function {
	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, List<Expression<JsonNode>> args, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, Version version) throws JsonQueryException {
		JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		@Var boolean result = false;
		if (jsonProvider.getNodeType(in) == JsonNodeType.NUMBER) {
			double v = jsonProvider.asDouble(in);
			result = !Double.isInfinite(v) && (v <= -Double.MIN_NORMAL || Double.MIN_NORMAL <= v);
		}
		output.emit(jsonProvider.createBoolean(result), null);
	}
}
