package net.thisptr.jackson.jq.internal.tree;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public class Tuple implements Expression {
	private List<Expression> qs;

	public Tuple(final List<Expression> qs) {
		this.qs = qs;
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Output output) throws JsonQueryException {
		for (final Expression q : qs) {
			q.apply(scope, in, output);
		}
	}

	@Override
	public String toString() {
		return qs.toString().replaceAll("^\\[", "(").replaceAll("\\]$", ")");
	}
}
