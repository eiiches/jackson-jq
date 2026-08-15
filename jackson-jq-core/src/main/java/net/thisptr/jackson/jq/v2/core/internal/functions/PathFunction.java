package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import com.google.auto.service.AutoService;
import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.path.RootPath;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

@AutoService(FunctionFactory.class)
@FunctionRegistration(name = "path", nargs = 1)
public class PathFunction implements FunctionFactory {

	@Override
	public <JsonNode> Function<JsonNode> createFunction(JsonProvider<JsonNode> jsonProvider, List<Expression> args, Version version) {
		return (scope, in, ipath, output) -> {
			args.get(0).apply(scope, in, RootPath.getInstance(), (obj, path0) -> {
				@Var @Nullable Path<JsonNode> path = path0;
				// `VALUE | path(VALUE) => []`
				if (path == null && JsonNodeUtils.isValueNode(jsonProvider, in) && new JsonNodeComparator<>(jsonProvider).compare(in, obj) == 0)
					path = RootPath.getInstance();
				if (path == null)
					throw new JsonQueryException("Invalid path expression with result %s", JsonNodeUtils.toString(jsonProvider, obj));
				JsonNode out = jsonProvider.createArray();
				path.toJsonNode(jsonProvider, out);
				output.emit(out, null);
			}, true);
		};
	}
}
