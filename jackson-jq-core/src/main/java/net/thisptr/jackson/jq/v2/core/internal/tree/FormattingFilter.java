package net.thisptr.jackson.jq.v2.core.internal.tree;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
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
	public <JsonNode> void apply(JsonProvider<JsonNode> jsonProvider, ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		throw new UnsupportedOperationException("FormattingFilter requires symbol resolution");
	}

	@Override
	public String toString() {
		return "@" + name;
	}
}
