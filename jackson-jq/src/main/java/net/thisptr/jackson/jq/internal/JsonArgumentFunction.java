package net.thisptr.jackson.jq.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

public abstract class JsonArgumentFunction<JsonNode> implements Function<JsonNode> {
	protected abstract JsonNode fn(final Scope<JsonNode> scope, final List<JsonNode> args, final JsonNode in) throws JsonQueryException;

	private void combinations(final Scope<JsonNode> scope, final PathOutput<JsonNode> output, final Stack<JsonNode> args, final int index, final List<List<JsonNode>> argmat, final JsonNode in) throws JsonQueryException {
		if (index >= argmat.size()) {
			output.emit(fn(scope, args, in), null);
			return;
		}

		for (final JsonNode arg : argmat.get(index)) {
			args.push(arg);
			combinations(scope, output, args, index + 1, argmat, in);
			args.pop();
		}
	}

	@Override
	public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		final List<List<JsonNode>> _args = new ArrayList<>(args.size());
		for (final Expression<JsonNode> arg : args) {
			final List<JsonNode> out = new ArrayList<>();
			arg.apply(scope, in, out::add);
			_args.add(out);
		}

		combinations(scope, output, new Stack<>(), 0, _args, in);
	}
}
