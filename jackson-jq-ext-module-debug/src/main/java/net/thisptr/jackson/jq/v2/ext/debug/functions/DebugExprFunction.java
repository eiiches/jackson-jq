package net.thisptr.jackson.jq.v2.ext.debug.functions;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public class DebugExprFunction implements Function {
	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindCtx, List<Expression<Context, JsonNode>> args) {
		JsonProvider<JsonNode> jsonProvider = bindCtx.getJsonProvider();
		Expression<Context, JsonNode> filter = args.get(0);
		boolean dependsOnInput = filter.dependsOnInput();
		boolean dependsOnExternalState = filter.dependsOnExternalState();
		return new Expression<Context, JsonNode>() {
			@Override
			public Cardinality getCardinality() {
				return Cardinality.ONE;
			}

			@Override
			public boolean dependsOnInput() {
				return false;
			}

			@Override
			public boolean dependsOnExternalState() {
				return false;
			}

			@Override
			public void apply(Context context, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
				Map<String, JsonNode> info = new LinkedHashMap<>();
				info.put("depends_on_input", jsonProvider.createBoolean(dependsOnInput));
				info.put("depends_on_external_state", jsonProvider.createBoolean(dependsOnExternalState));
				output.emit(jsonProvider.createObject(info), UntrackedPath.getInstance());
			}
		};
	}
}
