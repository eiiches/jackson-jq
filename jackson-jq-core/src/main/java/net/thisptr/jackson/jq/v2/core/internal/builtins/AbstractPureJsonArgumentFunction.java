package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.ArrayList;
import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.function.utils.ExpressionPropertiesUtils;
import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
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

	private <JsonNode> void combinations(JsonProvider<JsonNode> jsonProvider, Output<JsonNode> output, List<JsonNode> args, int index, List<List<JsonNode>> argmat) throws JsonQueryException {
		if (index >= argmat.size()) {
			output.emit(fn(jsonProvider, args), UntrackedPath.getInstance());
			return;
		}

		for (JsonNode arg : argmat.get(index)) {
			args.add(arg);
			combinations(jsonProvider, output, args, index + 1, argmat);
			args.remove(args.size() - 1);
		}
	}

	@Override
	public ExpressionProperties analyze(Version jqVersion, List<ExpressionProperties> arguments) {
		return ExpressionPropertiesUtils.forwardAll(CardinalityUtils.multiply(arguments, ExpressionProperties::cardinality), false, false, arguments);
	}

	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindCtx, List<Expression<Context, JsonNode>> args) {
		JsonProvider<JsonNode> jsonProvider = bindCtx.getJsonProvider();
		return (frame, in, ipath, output) -> {
			List<List<JsonNode>> _args = new ArrayList<>(args.size());
			for (Expression<Context, JsonNode> arg : args) {
				List<JsonNode> out = new ArrayList<>();
				arg.apply(frame, in, UntrackedPath.getInstance(), (v, opath) -> out.add(v));
				_args.add(out);
			}

			combinations(jsonProvider, output, new ArrayList<>(_args.size()), 0, _args);
		};
	}
}
