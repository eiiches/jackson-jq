package net.thisptr.jackson.jq.internal.functions;

import java.util.ArrayList;
import java.util.List;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;

import com.fasterxml.jackson.databind.JsonNode;

@BuiltinFunction("recurse/1")
public class RecurseFunction implements Function {
	@Override
	public List<JsonNode> apply(final Scope scope, final List<JsonQuery> args, final JsonNode in) throws JsonQueryException {
		final List<JsonNode> out = new ArrayList<>();
		recurse(scope, in, args.get(0), out);
		return out;
	}

	private static void recurse(final Scope scope, final JsonNode in, final JsonQuery path, final List<JsonNode> out) throws JsonQueryException {
		out.add(in);
		for (final JsonNode child : path.apply(scope, in))
			recurse(scope, child, path, out);
	}
}
