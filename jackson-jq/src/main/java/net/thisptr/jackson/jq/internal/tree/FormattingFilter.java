package net.thisptr.jackson.jq.internal.tree;

import java.util.Collections;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

public class FormattingFilter<JsonNode> implements Expression<JsonNode> {
	private final String name;
	private final Version version;

	public FormattingFilter(final String name, final Version version) {
		this.name = name;
		this.version = version;
	}

	@Override
	public void apply(final Scope<JsonNode> scope, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final boolean requirePath) throws JsonQueryException {
		final Function<JsonNode> f = scope.getFunction("@" + name, 0);
		if (f == null)
			throw new JsonQueryException("Formatting operator @" + name + " does not exist");
		f.apply(scope, Collections.emptyList(), in, ipath, output, version);
	}

	@Override
	public String toString() {
		return "@" + name;
	}
}
