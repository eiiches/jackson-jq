package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.List;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.function.utils.FunctionBody;
import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.json.comparator.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.core.internal.path.utils.PathUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.RootPath;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

@FunctionRegistration(name = "path", nargs = 1)
public class PathFunction implements Function {

	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindCtx, List<Expression<Context, JsonNode>> args) {
		JsonProvider<JsonNode> jsonProvider = bindCtx.getJsonProvider();
		return FunctionBody.builder(args).usesInput(true).cardinality(args.get(0).getCardinality()).build((frame, in, ipath, output) -> {
			args.get(0).apply(frame, in, RootPath.getInstance(), (obj, path0) -> {
				@Var Path<JsonNode> path = path0;
				// `VALUE | path(VALUE) => []`
				if (PathUtils.isLost(path) && JsonNodeUtils.isValueNode(jsonProvider, in) && new JsonNodeComparator<>(jsonProvider).compare(in, obj) == 0)
					path = RootPath.getInstance();
				if (PathUtils.isLost(path))
					throw new JsonQueryException(String.format("Invalid path expression with result %s", JsonNodeUtils.toString(jsonProvider, obj)));

				JsonNode out = jsonProvider.createArray(path.toJsonList(jsonProvider));
				output.emit(out, UntrackedPath.getInstance());
			});
		});
	}
}
