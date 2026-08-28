package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.internal.FunctionBody;
import net.thisptr.jackson.jq.v2.core.internal.misc.PathUtils;
import net.thisptr.jackson.jq.v2.core.internal.path.PathOperations;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

@AutoService(Function.class)
@FunctionRegistration(name = "getpath", nargs = 1)
public class GetPathFunction implements Function {

	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return FunctionBody.builder(args).usesInput(true).cardinality(args.get(0).getCardinality()).build((frame, in, ipath, output) -> {
			args.get(0).apply(frame, in, UntrackedPath.getInstance(), (argpath, opath) -> {
				Path<JsonNode> subpath = PathUtils.toPath(jsonProvider, argpath);
				PathOperations.resolve(jsonProvider, subpath, in, ipath, output, false, version);
			});
		});
	}
}
