package net.thisptr.jackson.jq.internal.tree;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.misc.Pair;
import net.thisptr.jackson.jq.path.Path;

public class Conditional implements Expression {
	private Expression otherwise;
	private List<Pair<Expression, Expression>> switches;

	public Conditional(final List<Pair<Expression, Expression>> switches, final Expression otherwise) {
		this.switches = switches;
		this.otherwise = otherwise;
	}

	public Conditional() {}

	public void setOtherwise(Expression otherwise) {
		this.otherwise = otherwise;
	}

	public Expression getOtherwise() {
		return otherwise;
	}

	public void setSwitches(List<Pair<Expression, Expression>> switches) {
		this.switches = switches;
	}

	public List<Pair<Expression, Expression>> getSwitches() {
		return Collections.unmodifiableList(switches);
	}

	private void pathRecursive(PathOutput output, Scope scope, List<Pair<Expression, Expression>> switches, JsonNode in, Path path) throws JsonQueryException {
		final Pair<Expression, Expression> sw = switches.get(0);
		sw._1.apply(scope, in, (r) -> {
			if (JsonNodeUtils.asBoolean(r)) {
				sw._2.apply(scope, in, path, output, false);
			} else {
				if (switches.size() > 1) {
					pathRecursive(output, scope, switches.subList(1, switches.size()), in, path);
				} else {
					otherwise.apply(scope, in, path, output, false);
				}
			}
		});
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Path path, final PathOutput output, final boolean requirePath) throws JsonQueryException {
		pathRecursive(output, scope, switches, in, path);
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
