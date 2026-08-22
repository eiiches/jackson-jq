package net.thisptr.jackson.jq.v2.core.internal;

import java.util.ArrayList;
import java.util.List;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public abstract class JsonArgumentFunction implements Function {
	protected abstract <JsonNode> JsonNode fn(JsonProvider<JsonNode> jsonProvider, List<JsonNode> args, JsonNode in) throws JsonQueryException;

	private <JsonNode> void combinations(JsonProvider<JsonNode> jsonProvider, PathOutput<JsonNode> output, List<JsonNode> args, int index, List<List<JsonNode>> argmat, JsonNode in) throws JsonQueryException {
		if (index >= argmat.size()) {
			output.emit(fn(jsonProvider, args, in), null);
			return;
		}

		for (JsonNode arg : argmat.get(index)) {
			args.add(arg);
			combinations(jsonProvider, output, args, index + 1, argmat, in);
			args.remove(args.size() - 1);
		}
	}

	@Override
	public <JsonNode> Expression<JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<JsonNode>> args, Version version) {
		return (frame, in, ipath, output) -> {
			List<List<JsonNode>> _args = new ArrayList<>(args.size());
			for (Expression<JsonNode> arg : args) {
				List<JsonNode> out = new ArrayList<>();
				arg.apply(frame, in, out::add);
				_args.add(out);
			}

			combinations(jsonProvider, output, new ArrayList<>(_args.size()), 0, _args, in);
		};
	}
}
