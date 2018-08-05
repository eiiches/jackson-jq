package net.thisptr.jackson.jq.internal.tree;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.misc.Pair;

public class Conditional implements Expression {
	private Expression otherwise;
	private List<Pair<Expression, Expression>> switches;

	public Conditional(final List<Pair<Expression, Expression>> switches, final Expression otherwise) {
		this.switches = switches;
		this.otherwise = otherwise;
	}

	private void applyRecursive(final Output output, final Scope scope, final List<Pair<Expression, Expression>> switches, final JsonNode in) throws JsonQueryException {
		final Pair<Expression, Expression> sw = switches.get(0);
		sw._1.apply(scope, in, (r) -> {
			if (JsonNodeUtils.asBoolean(r)) {
				sw._2.apply(scope, in, output);
			} else {
				if (switches.size() > 1) {
					applyRecursive(output, scope, switches.subList(1, switches.size()), in);
				} else {
					otherwise.apply(scope, in, output);
				}
			}
		});
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Output output) throws JsonQueryException {
		applyRecursive(output, scope, switches, in);
	}

	@Override
	public String toString() {
		String ifstr = "if";
		final StringBuilder builder = new StringBuilder();
		for (final Pair<Expression, Expression> sw : switches) {
			builder.append(ifstr);
			builder.append(" ");
			builder.append(sw._1 != null ? sw._1 : "null");
			builder.append(" ");
			builder.append("then");
			builder.append(" ");
			builder.append(sw._2 != null ? sw._2 : "null");
			builder.append(" ");
			ifstr = "elif";
		}
		builder.append("else ");
		builder.append(otherwise != null ? otherwise : "null");
		builder.append(" ");
		builder.append("end");
		return builder.toString();
	}
}
