package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.internal.FunctionBody;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@AutoService(Function.class)
@FunctionRegistration(name = "rindex", nargs = 1)
public class RIndexFunction implements Function {
	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return FunctionBody.builder(args).usesInput(true).build((frame, in, ipath, output) -> {
			if (jsonProvider.isNull(in)) {
				output.emit(jsonProvider.createNull(), UntrackedPath.getInstance());
				return;
			}

			args.get(0).apply(frame, in, UntrackedPath.getInstance(), (needle, opath) -> {
				List<Integer> tmp = IndicesFunction.indices(jsonProvider, needle, in);
				if (tmp.isEmpty()) {
					output.emit(jsonProvider.createNull(), UntrackedPath.getInstance());
				} else {
					output.emit(jsonProvider.createNumber(tmp.get(tmp.size() - 1)), UntrackedPath.getInstance());
				}
			});
		});
	}
}
