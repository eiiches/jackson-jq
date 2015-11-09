package net.thisptr.jackson.jq.internal.tree;

import java.util.Collections;
import java.util.List;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.JsonQueryFunction;

import com.fasterxml.jackson.databind.JsonNode;

public class FunctionDefinition extends JsonQuery {
	private JsonQuery body;
	private String fname;
	private List<String> args;

	public FunctionDefinition(final String fname, final List<String> args, final JsonQuery body) {
		this.fname = fname;
		this.args = args;
		this.body = body;
	}

	@Override
	public List<JsonNode> apply(final Scope scope, final JsonNode in) throws JsonQueryException {
		scope.addFunction(fname, args.size(), new JsonQueryFunction(fname, args, body, scope));
		return Collections.emptyList();
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
