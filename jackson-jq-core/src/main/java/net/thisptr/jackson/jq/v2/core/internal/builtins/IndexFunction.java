package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.internal.function.utils.FunctionBody;
import net.thisptr.jackson.jq.v2.core.internal.path.utils.PathOperations;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@AutoService(Function.class)
@FunctionRegistration(name = "index", nargs = 1)
public class IndexFunction implements Function {
	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return FunctionBody.builder(args).usesInput(true).build((frame, in, ipath, output) -> {
			args.get(0).apply(frame, in, UntrackedPath.getInstance(), (needle, opath) -> {
				IndicesFunction.emitIndices(jsonProvider, needle, in, version,
						(indices, path) -> PathOperations.resolveArrayIndex(jsonProvider, indices, UntrackedPath.getInstance(), output, 0, false, version));
			});
		});
	}
}
