package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Collections;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class FormattingFilter implements Expression {
	private final String name;
	private final Version version;

	public FormattingFilter(String name, Version version) {
		this.name = name;
		this.version = version;
	}

	public String name() {
		return name;
	}

	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		FunctionFactory factory = scope.getFunctionFactory("@" + name, 0);
		if (factory == null)
			throw new JsonQueryException("Formatting operator @" + name + " does not exist");
		Function<JsonNode> f = factory.createFunction(scope.jsonProvider(), Collections.emptyList(), version);
		f.apply(scope, in, ipath, output);
	}

	@Override
	public <JsonNode> void apply(JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		throw new UnsupportedOperationException("FormattingFilter requires symbol resolution");
	}

	@Override
	public String toString() {
		return "@" + name;
	}
}
