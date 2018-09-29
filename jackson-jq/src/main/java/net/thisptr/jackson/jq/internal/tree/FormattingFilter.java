package net.thisptr.jackson.jq.internal.tree;

import java.util.Collections;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public class FormattingFilter implements Expression {
	private final String name;
	private final Version version;

	public FormattingFilter(final String name, final Version version) {
		this.name = name;
		this.version = version;
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Output output) throws JsonQueryException {
		final Function f = scope.getFunction("@" + name, 0);
		if (f == null)
			throw new JsonQueryException("Formatting operator @" + name + " does not exist");
		f.apply(scope, Collections.emptyList(), in, output, version);
	}

	@Override
	public String toString() {
		return "@" + name;
	}
}
