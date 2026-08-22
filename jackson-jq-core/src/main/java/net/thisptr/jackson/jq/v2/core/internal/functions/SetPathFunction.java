package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import com.google.auto.service.AutoService;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.FunctionBody;
import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.core.internal.misc.PathUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.path.Path;

@AutoService(Function.class)
@FunctionRegistration(name = "setpath", nargs = 2)
public class SetPathFunction implements Function {

	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return FunctionBody.builder(args).usesInput(true).cardinality(CardinalityUtils.multiply(args.get(0).getCardinality(), args.get(1).getCardinality())).build((frame, in, ipath, output) -> {
			args.get(1).apply(frame, in, null, (newvalnode, opath) -> {
				args.get(0).apply(frame, in, null, (pathnode, opath2) -> {
					@Nullable Path<JsonNode> path = PathUtils.toPath(jsonProvider, pathnode, version);
					JsonNode out = path.mutate(jsonProvider, in, (dummy) -> newvalnode);
					output.emit(out, path);
				});
			});
		});
	}
}
