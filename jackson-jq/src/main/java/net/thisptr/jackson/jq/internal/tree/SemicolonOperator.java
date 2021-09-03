package net.thisptr.jackson.jq.internal.tree;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

public class SemicolonOperator implements Expression {
	private List<Expression> qs;

	public SemicolonOperator(final List<Expression> qs) {
		this.qs = qs;
	}

	public SemicolonOperator() {}

	public List<Expression> getQs() {
		return Collections.unmodifiableList(qs);
	}

	public void setQs(List<Expression> qs) {
		this.qs = qs;
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Path path, final PathOutput output, final boolean requirePath) throws JsonQueryException {
		if (qs.isEmpty())
			return;
		for (final Expression q : qs.subList(0, qs.size() - 1))
			q.apply(scope, in, (out) -> {});
		qs.get(qs.size() - 1).apply(scope, in, path, output, requirePath);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		String sep = "";
		for (final Expression q : qs) {
			builder.append(sep);
			builder.append(q);
			sep = "; ";
		}
		return builder.toString();
	}
}
