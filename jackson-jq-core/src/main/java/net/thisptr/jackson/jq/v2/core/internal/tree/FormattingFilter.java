package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Collections;

import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class FormattingFilter<JsonNode> implements Expression<JsonNode> {
	private final String name;
	private final Version version;

	public FormattingFilter(String name, Version version) {
		this.name = name;
		this.version = version;
	}

	@Override
	public void apply(Scope<JsonNode> scope, JsonNode in, Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		Function f = scope.getFunction("@" + name, 0);
		if (f == null)
			throw new JsonQueryException("Formatting operator @" + name + " does not exist");
		f.apply(scope, Collections.emptyList(), in, ipath, output, version);
	}

	@Override
	public String toString() {
		return "@" + name;
	}
}
