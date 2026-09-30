package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.function.utils.ExpressionPropertiesUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public abstract class AbstractPureJsonArgumentFunction implements Function {
	protected abstract <JsonNode> JsonNode fn(JsonProvider<JsonNode> jsonProvider, List<JsonNode> args) throws JsonQueryException;

	private <Context extends RuntimeContext, JsonNode> void combinations(JsonProvider<JsonNode> jsonProvider, Context frame, JsonNode in, Output<JsonNode> output,
			List<Expression<Context, JsonNode>> expressions, List<JsonNode> args, int index) throws JsonQueryException {
		if (index < 0) {
			output.emit(fn(jsonProvider, args), UntrackedPath.getInstance());
			return;
		}

		expressions.get(index).apply(frame, in, UntrackedPath.getInstance(), (value, path) -> {
			args.set(index, value);
			combinations(jsonProvider, frame, in, output, expressions, args, index - 1);
		});
	}

	@Override
	public ExpressionProperties analyze(Version jqVersion, List<ExpressionProperties> arguments) {
		return ExpressionPropertiesUtils.forwardAll(Cardinality.ONE, false, false, arguments);
	}

	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindCtx, List<Expression<Context, JsonNode>> args) {
		JsonProvider<JsonNode> jsonProvider = bindCtx.getJsonProvider();
		return (frame, in, ipath, output) -> combinations(jsonProvider, frame, in, output, args,
				new ArrayList<>(Collections.nCopies(args.size(), null)), args.size() - 1);
	}
}
