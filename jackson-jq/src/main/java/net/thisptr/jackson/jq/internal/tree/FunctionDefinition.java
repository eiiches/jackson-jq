package net.thisptr.jackson.jq.internal.tree;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.JsonQueryFunction;

public class FunctionDefinition implements Expression {
	private Expression body;
	private String fname;
	private List<String> args;

	public FunctionDefinition(final String fname, final List<String> args, final Expression body) {
		this.fname = fname;
		this.args = args;
		this.body = body;
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Output output) throws JsonQueryException {
		scope.addFunction(fname, args.size(), new JsonQueryFunction(fname, args, body, scope));
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
