package net.thisptr.jackson.jq.internal.functions;

import java.util.ArrayList;
import java.util.List;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.functions.MatchFunction.MatchObject;
import net.thisptr.jackson.jq.internal.misc.Preconditions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

@BuiltinFunction("test/2")
public class TestFunction implements Function {
	@Override
	public List<JsonNode> apply(final Scope scope, final List<JsonQuery> args, final JsonNode in) throws JsonQueryException {
		Preconditions.checkInputType("test", in, JsonNodeType.STRING);

		final List<JsonNode> out = new ArrayList<>();
		for (final JsonNode regex : args.get(0).apply(scope, in))
			for (final JsonNode modifiers : args.get(1).apply(scope, in))
				out.add(hasMatch(MatchFunction.match(in, regex, modifiers)));
		return out;
	}

	private static BooleanNode hasMatch(final List<MatchObject> r) {
		return BooleanNode.valueOf(!r.isEmpty());
	}
}
