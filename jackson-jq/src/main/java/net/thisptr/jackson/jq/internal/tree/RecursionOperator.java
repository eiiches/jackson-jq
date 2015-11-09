package net.thisptr.jackson.jq.internal.tree;

import java.util.ArrayList;
import java.util.List;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

import com.fasterxml.jackson.databind.JsonNode;

public class RecursionOperator extends JsonQuery {
	@Override
	public List<JsonNode> apply(final Scope scope, final JsonNode in) throws JsonQueryException {
		final List<JsonNode> out = new ArrayList<>();
		applyRecursive(scope, in, out);
		return out;
	}

	private static void applyRecursive(final Scope scope, final JsonNode in, final List<JsonNode> out) throws JsonQueryException {
		out.add(in);
		if (in.isObject() || in.isArray()) {
			for (final JsonNode child : in)
				applyRecursive(scope, child, out);
		}
	}

	@Override
	public String toString() {
		return "(..)";
	}
}
