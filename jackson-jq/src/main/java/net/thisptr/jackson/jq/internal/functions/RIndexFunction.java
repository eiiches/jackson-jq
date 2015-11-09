package net.thisptr.jackson.jq.internal.functions;

import java.util.ArrayList;
import java.util.List;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;

@BuiltinFunction("rindex/1")
public class RIndexFunction implements Function {
	@Override
	public List<JsonNode> apply(final Scope scope, final List<JsonQuery> args, final JsonNode in) throws JsonQueryException {
		final List<JsonNode> out = new ArrayList<>();
		for (final JsonNode needle : args.get(0).apply(scope, in)) {
			final List<Integer> tmp = IndicesFunction.indices(needle, in);
			if (tmp.isEmpty()) {
				out.add(NullNode.getInstance());
			} else {
				out.add(new IntNode(tmp.get(tmp.size() - 1)));
			}
		}
		return out;
	}
}
