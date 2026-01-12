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
import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.path.Path;
import net.thisptr.jackson.jq.path.RootPath;

@AutoService(Function.class)
@BuiltinFunction("path/1")
public class PathFunction<JsonNode> implements Function<JsonNode> {

	@Override
	public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		args.get(0).apply(scope, in, RootPath.getInstance(), (obj, path) -> {
			// `VALUE | path(VALUE) => []`
			if (path == null && JsonNodeUtils.isValueNode(jsonProvider, in) && new JsonNodeComparator<>(jsonProvider).compare(in, obj) == 0)
				path = RootPath.getInstance();
			if (path == null)
				throw new JsonQueryException("Invalid path expression with result %s", JsonNodeUtils.toString(jsonProvider, obj));
			final JsonNode out = jsonProvider.createArray();
			path.toJsonNode(jsonProvider, out);
			output.emit(out, null);
		}, true);
	}
}
