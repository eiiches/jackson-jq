package net.thisptr.jackson.jq.v2.core.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public abstract class JsonArgumentFunction implements Function {
	protected abstract <JsonNode> JsonNode fn(Scope<JsonNode> scope, List<JsonNode> args, JsonNode in) throws JsonQueryException;

	private <JsonNode> void combinations(Scope<JsonNode> scope, PathOutput<JsonNode> output, Stack<JsonNode> args, int index, List<List<JsonNode>> argmat, JsonNode in) throws JsonQueryException {
		if (index >= argmat.size()) {
			output.emit(fn(scope, args, in), null);
			return;
		}

		for (JsonNode arg : argmat.get(index)) {
			args.push(arg);
			combinations(scope, output, args, index + 1, argmat, in);
			args.pop();
		}
	}

	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, List<Expression> args, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, Version version) throws JsonQueryException {
		List<List<JsonNode>> _args = new ArrayList<>(args.size());
		for (Expression arg : args) {
			List<JsonNode> out = new ArrayList<>();
			arg.apply(scope, in, out::add);
			_args.add(out);
		}

		combinations(scope, output, new Stack<>(), 0, _args, in);
	}
}
