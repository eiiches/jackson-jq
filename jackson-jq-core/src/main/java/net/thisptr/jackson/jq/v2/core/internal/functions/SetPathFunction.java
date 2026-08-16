package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import com.google.auto.service.AutoService;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.PathUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.path.Path;

@AutoService(FunctionFactory.class)
@FunctionRegistration(name = "setpath", nargs = 2)
public class SetPathFunction implements FunctionFactory {

	@Override
	public <JsonNode> Function<JsonNode> createFunction(JsonProvider<JsonNode> jsonProvider, List<Expression<JsonNode>> args, Version version) {
		return (frame, in, ipath, output) -> {
			args.get(1).apply(frame, in, (newvalnode) -> {
				args.get(0).apply(frame, in, (pathnode) -> {
					@Nullable Path<JsonNode> path = PathUtils.toPath(jsonProvider, pathnode);
					JsonNode out = path.mutate(jsonProvider, in, (dummy) -> newvalnode);
					output.emit(out, path);
				});
			});
		};
	}
}
