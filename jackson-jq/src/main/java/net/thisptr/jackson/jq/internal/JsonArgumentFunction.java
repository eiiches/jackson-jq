package net.thisptr.jackson.jq.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Functional;

public abstract class JsonArgumentFunction implements Function {
	protected abstract JsonNode fn(final List<JsonNode> args, final JsonNode in) throws JsonQueryException;

	private void combinations(final Functional.Consumer<JsonNode> out, final Stack<JsonNode> args, final int index, final List<List<JsonNode>> argmat, final JsonNode in) throws JsonQueryException {
		if (index >= argmat.size()) {
			out.accept(fn(args, in));
			return;
		}

		for (final JsonNode arg : argmat.get(index)) {
			args.push(arg);
			combinations(out, args, index + 1, argmat, in);
			args.pop();
		}
	}

	@Override
	public List<JsonNode> apply(final Scope scope, final List<JsonQuery> args, final JsonNode in) throws JsonQueryException {
		final List<List<JsonNode>> _args = new ArrayList<>();
		for (final JsonQuery arg : args)
			_args.add(arg.apply(scope, in));

		final List<JsonNode> out = new ArrayList<>();
		combinations(o -> out.add(o), new Stack<>(), 0, _args, in);
		return out;
	}
}
