package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.JsonQueryFunction;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class FunctionDefinition<JsonNode> implements Expression<JsonNode> {
	private Expression<JsonNode> body;
	private String fname;
	private List<String> args;

	public FunctionDefinition(final String fname, final List<String> args, final Expression<JsonNode> body) {
		this.fname = fname;
		this.args = args;
		this.body = body;
	}

	@Override
	public void apply(final Scope<JsonNode> scope, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final boolean requirePath) throws JsonQueryException {
		scope.addFunction(fname, args.size(), new JsonQueryFunction<>(fname, args, body, scope));
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder("def ");
		builder.append(fname);
		if (!args.isEmpty()) {
			builder.append("(");
			String sep = "";
			for (final String arg : args) {
				builder.append(sep);
				builder.append(arg);
				sep = "; ";
			}
			builder.append(")");
		}
		builder.append(": ");
		builder.append(body);
		return builder.toString();
	}
}
