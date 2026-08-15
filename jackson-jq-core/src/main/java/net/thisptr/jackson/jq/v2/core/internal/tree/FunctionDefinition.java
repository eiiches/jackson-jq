package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.List;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.JsonQueryFunction;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class FunctionDefinition implements Expression {
	private Expression body;
	private String fname;
	private List<String> args;

	public FunctionDefinition(String fname, List<String> args, Expression body) {
		this.fname = fname;
		this.args = args;
		this.body = body;
	}

	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		scope.addFunctionFactory(fname, args.size(), new JsonQueryFunction<>(fname, args, body, scope));
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("def ");
		builder.append(fname);
		if (!args.isEmpty()) {
			builder.append("(");
			@Var String sep = "";
			for (String arg : args) {
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
