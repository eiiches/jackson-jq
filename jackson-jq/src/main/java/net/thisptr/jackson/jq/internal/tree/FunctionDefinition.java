package net.thisptr.jackson.jq.internal.tree;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.JsonQueryFunction;
import net.thisptr.jackson.jq.path.Path;

public class FunctionDefinition implements Expression {
	private Expression body;
	private String fname;
	private List<String> args;

	public FunctionDefinition(final String fname, final List<String> args, final Expression body) {
		this.fname = fname;
		this.args = args;
		this.body = body;
	}

	public FunctionDefinition() {}

	public Expression getBody() {
		return body;
	}

	public void setBody(Expression body) {
		this.body = body;
	}

	public List<String> getArgs() {
		return Collections.unmodifiableList(args);
	}

	public void setArgs(List<String> args) {
		this.args = args;
	}

	public String getFname() {
		return fname;
	}

	public void setFname(String fname) {
		this.fname = fname;
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Path ipath, final PathOutput output, final boolean requirePath) throws JsonQueryException {
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
