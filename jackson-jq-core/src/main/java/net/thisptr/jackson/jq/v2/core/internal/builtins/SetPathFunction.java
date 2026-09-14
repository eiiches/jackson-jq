package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.function.utils.FunctionBody;
import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.core.internal.path.utils.PathOperations;
import net.thisptr.jackson.jq.v2.core.internal.path.utils.PathUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@FunctionRegistration(name = "setpath", nargs = 2)
public class SetPathFunction implements Function {

	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindCtx, List<Expression<Context, JsonNode>> args) {
		JsonProvider<JsonNode> jsonProvider = bindCtx.getJsonProvider();
		Version version = bindCtx.getJqVersion();
		return FunctionBody.builder(args).usesInput(true).cardinality(CardinalityUtils.multiply(args.get(0).getCardinality(), args.get(1).getCardinality())).build((frame, in, ipath, output) -> {
			args.get(1).apply(frame, in, UntrackedPath.getInstance(), (newvalnode, opath) -> {
				args.get(0).apply(frame, in, UntrackedPath.getInstance(), (pathnode, opath2) -> {
					Path<JsonNode> path = PathUtils.toPath(jsonProvider, pathnode);
					JsonNode out = PathOperations.mutate(jsonProvider, frame.getRuntimeLimits(), path, in, (dummy) -> newvalnode, version);
					output.emit(out, path);
				});
			});
		});
	}
}
