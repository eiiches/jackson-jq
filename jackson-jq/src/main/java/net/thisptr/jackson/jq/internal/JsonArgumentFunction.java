package net.thisptr.jackson.jq.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public abstract class JsonArgumentFunction implements Function {
	protected abstract JsonNode fn(final List<JsonNode> args, final JsonNode in) throws JsonQueryException;

	private void combinations(final Output output, final Stack<JsonNode> args, final int index, final List<List<JsonNode>> argmat, final JsonNode in) throws JsonQueryException {
		if (index >= argmat.size()) {
			output.emit(fn(args, in));
			return;
		}

		for (final JsonNode arg : argmat.get(index)) {
			args.push(arg);
			combinations(output, args, index + 1, argmat, in);
			args.pop();
		}
	}

	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Output output) throws JsonQueryException {
		final List<List<JsonNode>> _args = new ArrayList<>();
		for (final Expression arg : args) {
			final List<JsonNode> out = new ArrayList<>();
			arg.apply(scope, in, out::add);
			_args.add(out);
		}

		combinations(output, new Stack<>(), 0, _args, in);
	}
}
