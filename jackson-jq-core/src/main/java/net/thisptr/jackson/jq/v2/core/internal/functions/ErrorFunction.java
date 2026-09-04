package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.internal.FunctionBody;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryUserException;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@AutoService(Function.class)
@FunctionRegistration(name = "error", nargs = 0)
@FunctionRegistration(name = "error", nargs = 1)
public class ErrorFunction implements Function {
	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return FunctionBody.builder(args).usesInput(args.isEmpty()).cardinality(Cardinality.ZERO).build((frame, in, ipath, output) -> {
			if (args.isEmpty()) {
				if (jsonProvider.isNull(in))
					return;
				throw new JsonQueryUserException(jsonProvider, in);
			} else {
				args.get(0).apply(frame, in, UntrackedPath.getInstance(), (out, opath) -> {
					if (jsonProvider.isNull(out))
						return;
					throw new JsonQueryUserException(jsonProvider, out);
				});
			}
		});
	}
}
