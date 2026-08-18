package net.thisptr.jackson.jq.v2.core.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public abstract class JsonArgumentFunction implements FunctionFactory {
	protected abstract <JsonNode> JsonNode fn(JsonProvider<JsonNode> jsonProvider, List<JsonNode> args, JsonNode in) throws JsonQueryException;

	private <JsonNode> void combinations(JsonProvider<JsonNode> jsonProvider, PathOutput<JsonNode> output, Stack<JsonNode> args, int index, List<List<JsonNode>> argmat, JsonNode in) throws JsonQueryException {
		if (index >= argmat.size()) {
			output.emit(fn(jsonProvider, args, in), null);
			return;
		}

		for (JsonNode arg : argmat.get(index)) {
			args.push(arg);
			combinations(jsonProvider, output, args, index + 1, argmat, in);
			args.pop();
		}
	}

	@Override
	public <JsonNode> Expression<JsonNode> createFunction(JsonProvider<JsonNode> jsonProvider, List<Expression<JsonNode>> args, Version version) {
		return (frame, in, ipath, output, ignoredRequirePath) -> {
			List<List<JsonNode>> _args = new ArrayList<>(args.size());
			for (Expression<JsonNode> arg : args) {
				List<JsonNode> out = new ArrayList<>();
				arg.apply(frame, in, out::add);
				_args.add(out);
			}

			combinations(jsonProvider, output, new Stack<>(), 0, _args, in);
		};
	}
}
