package net.thisptr.jackson.jq.internal.tree;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

public class FunctionCall implements Expression {
	private String name;
	private List<Expression> args;
	private Version version;

	public FunctionCall(final String name, final List<Expression> args, final Version version) {
		this.name = name;
		this.args = args;
		this.version = version;
	}

	@Override
	public void apply(Scope scope, JsonNode in, Path path, PathOutput output, final boolean requirePath) throws JsonQueryException {
		final Function f = scope.getFunction(name, args.size());
		if (f == null)
			throw new JsonQueryException(String.format("Function %s/%s does not exist", name, args.size()));
		f.apply(scope, args, in, path, output, version);
	}

	@Override
	public String toString() {
		if (args.isEmpty()) {
			return String.format("%s", name);
		} else {
			final StringBuilder builder = new StringBuilder(name);
			builder.append("(");
			String sep = "";
			for (final Expression arg : args) {
				builder.append(sep);
				if (arg == null) {
					builder.append("null");
				} else {
					builder.append(arg.toString());
				}
				sep = "; ";
			}
			builder.append(")");
			return builder.toString();
		}
	}
}
