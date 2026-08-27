package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import com.google.auto.service.AutoService;
import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.FunctionBody;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.path.RootPath;
import net.thisptr.jackson.jq.v2.core.path.UnrepresentablePath;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

@AutoService(Function.class)
@FunctionRegistration(name = "path", nargs = 1)
public class PathFunction implements Function {

	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return FunctionBody.builder(args).usesInput(true).cardinality(args.get(0).getCardinality()).build((frame, in, ipath, output) -> {
			args.get(0).apply(frame, in, RootPath.getInstance(), (obj, path0) -> {
				@Var @Nullable Path<JsonNode> path = path0;
				// `VALUE | path(VALUE) => []`
				if (UnrepresentablePath.isLost(path) && JsonNodeUtils.isValueNode(jsonProvider, in) && new JsonNodeComparator<>(jsonProvider).compare(in, obj) == 0)
					path = RootPath.getInstance();
				if (path == null || path instanceof UnrepresentablePath)
					throw new JsonQueryException(String.format("Invalid path expression with result %s", JsonNodeUtils.toString(jsonProvider, obj)));
				JsonNode out = jsonProvider.createArray();
				path.toJsonNode(jsonProvider, out);
				output.emit(out, null);
			});
		});
	}
}
