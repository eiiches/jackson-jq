package net.thisptr.jackson.jq.v2.ext.debug.functions;

import java.util.List;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

// TODO: make this useful or remove
public class DebugScopeFunction implements Function {

	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return new Expression<Context, JsonNode>() {
			@Override
			public Cardinality getCardinality() {
				return Cardinality.ONE;
			}

			@Override
			public boolean dependsOnExternalState() {
				return false;
			}

			@Override
			public void apply(Context context, JsonNode in, @Nullable Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
				JsonNode functions = jsonProvider.createObject();

				@Var JsonNode scopeNode = jsonProvider.createObject();
				scopeNode = jsonProvider.set(scopeNode, "functions", functions);

				@Var JsonNode info = jsonProvider.createObject();
				info = jsonProvider.set(info, "scope", scopeNode);
				info = jsonProvider.set(info, "input", in);
				output.emit(info, null);
			}
		};
	}
}
