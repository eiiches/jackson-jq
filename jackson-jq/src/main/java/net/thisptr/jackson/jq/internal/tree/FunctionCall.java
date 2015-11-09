package net.thisptr.jackson.jq.internal.tree;

import java.util.List;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

import com.fasterxml.jackson.databind.JsonNode;

public class FunctionCall extends JsonQuery {
	private String name;
	private List<JsonQuery> args;

	public FunctionCall(final String name, final List<JsonQuery> args) {
		this.name = name;
		this.args = args;
	}

	@Override
	public List<JsonNode> apply(final Scope scope, final JsonNode in) throws JsonQueryException {
		final Function f = scope.getFunction(name, args.size());
		if (f == null)
			throw new JsonQueryException(String.format("Function %s/%s does not exist", name, args.size()));
		return f.apply(scope, args, in);
	}

	@Override
	public String toString() {
		if (args.isEmpty()) {
			return String.format("%s", name);
		} else {
			final StringBuilder builder = new StringBuilder(name);
			builder.append("(");
			String sep = "";
			for (final JsonQuery arg : args) {
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
