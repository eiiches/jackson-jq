package net.thisptr.jackson.jq.v2.core.internal;

import java.util.ArrayList;
import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public abstract class PureJsonArgumentFunction implements Function {
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
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return FunctionBody.builder(args).cardinality(CardinalityUtils.multiply(args, Expression::getCardinality)).build((frame, in, ipath, output) -> {
			List<List<JsonNode>> _args = new ArrayList<>(args.size());
			for (Expression<Context, JsonNode> arg : args) {
				List<JsonNode> out = new ArrayList<>();
				arg.apply(frame, in, UntrackedPath.getInstance(), (v, opath) -> out.add(v));
				_args.add(out);
			}

			combinations(jsonProvider, output, new ArrayList<>(_args.size()), 0, _args);
		});
	}
}
