package net.thisptr.jackson.jq.internal.tree;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public class FunctionCall implements Expression {
	private String name;
	private List<Expression> args;

	public FunctionCall(final String name, final List<Expression> args) {
		this.name = name;
		this.args = args;
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Output output) throws JsonQueryException {
		final Function f = scope.getFunction(name, args.size());
		if (f == null)
			throw new JsonQueryException(String.format("Function %s/%s does not exist", name, args.size()));
		f.apply(scope, args, in, output);
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
