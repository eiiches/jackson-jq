package net.thisptr.jackson.jq.internal.tree;

import java.util.Collections;
import java.util.List;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

import com.fasterxml.jackson.databind.JsonNode;

public class FormattingFilter extends JsonQuery {
	private String name;

	public FormattingFilter(final String name) {
		this.name = name;
	}

	@Override
	public List<JsonNode> apply(final Scope scope, final JsonNode in) throws JsonQueryException {
		final Function f = scope.getFunction("@" + name, 0);
		if (f == null)
			throw new JsonQueryException("Formatting operator @" + name + " does not exist");
		return f.apply(scope, Collections.<JsonQuery> emptyList(), in);
	}

	@Override
	public String toString() {
		return "@" + name;
	}
}
