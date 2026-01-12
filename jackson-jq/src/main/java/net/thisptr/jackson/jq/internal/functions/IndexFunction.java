package net.thisptr.jackson.jq.internal.functions;

import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
@BuiltinFunction("index/1")
public class IndexFunction<JsonNode> implements Function<JsonNode> {
	@Override
	public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		if (jsonProvider.getNodeType(in) == JsonNodeType.NULL) {
			output.emit(jsonProvider.createNull(), null);
			return;
		}

		args.get(0).apply(scope, in, (needle) -> {
			final List<Integer> tmp = IndicesFunction.indices(jsonProvider, needle, in);
			if (tmp.isEmpty()) {
				output.emit(jsonProvider.createNull(), null);
			} else {
				output.emit(jsonProvider.createInt(tmp.get(0)), null);
			}
		});
	}
}
