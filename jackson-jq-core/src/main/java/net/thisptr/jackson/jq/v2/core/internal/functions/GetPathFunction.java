package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.internal.misc.PathUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.path.Path;

@AutoService(FunctionFactory.class)
@FunctionRegistration(name = "getpath", nargs = 1)
public class GetPathFunction implements FunctionFactory {

	@Override
	public <JsonNode> Function<JsonNode> createFunction(JsonProvider<JsonNode> jsonProvider, List<Expression> args, Version version) {
		return (frame, in, ipath, output) -> {
			args.get(0).apply(jsonProvider, frame, in, (argpath) -> {
				Path<JsonNode> subpath = PathUtils.toPath(jsonProvider, argpath);
				subpath.get(jsonProvider, in, ipath, output, false);
			});
		};
	}
}
