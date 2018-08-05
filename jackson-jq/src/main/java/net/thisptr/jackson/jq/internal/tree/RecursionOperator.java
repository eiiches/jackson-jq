package net.thisptr.jackson.jq.internal.tree;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public class RecursionOperator implements Expression {
	@Override
	public void apply(final Scope scope, final JsonNode in, final Output output) throws JsonQueryException {
		applyRecursive(scope, in, output);
	}

	private static void applyRecursive(final Scope scope, final JsonNode in, final Output output) throws JsonQueryException {
		output.emit(in);
		if (in.isObject() || in.isArray()) {
			for (final JsonNode child : in)
				applyRecursive(scope, child, output);
		}
	}

	@Override
	public String toString() {
		return "(..)";
	}
}
