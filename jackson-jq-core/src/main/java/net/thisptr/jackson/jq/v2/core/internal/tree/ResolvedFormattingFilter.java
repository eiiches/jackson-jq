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

public class ResolvedFormattingFilter implements Expression {
	private final String name;
	private final FunctionFactory factory;
	private final Version version;

	public ResolvedFormattingFilter(String name, FunctionFactory factory, Version version) {
		this.name = name;
		this.factory = factory;
		this.version = version;
	}

	public String name() {
		return name;
	}

	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		Function<JsonNode> f = factory.createFunction(scope.jsonProvider(), Collections.emptyList(), version);
		f.apply(scope, in, ipath, output);
	}

	@Override
	public String toString() {
		return "@" + name;
	}
}
