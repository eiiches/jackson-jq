package net.thisptr.jackson.jq.internal.tree;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

public class Tuple implements Expression {
	private List<Expression> qs;

	public Tuple(final List<Expression> qs) {
		this.qs = qs;
	}

	public Tuple() {}

	public List<Expression> getQs() {
		return Collections.unmodifiableList(qs);
	}

	public void setQs(List<Expression> qs) {
		this.qs = qs;
	}

	@Override
	public String toString() {
		return qs.toString().replaceAll("^\\[", "(").replaceAll("\\]$", ")");
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Path path, final PathOutput output, final boolean requirePath) throws JsonQueryException {
		for (final Expression q : qs) {
			q.apply(scope, in, path, output, requirePath);
		}
	}
}
