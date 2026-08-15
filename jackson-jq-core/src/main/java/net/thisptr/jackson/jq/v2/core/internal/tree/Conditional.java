package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.List;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.misc.Pair;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class Conditional implements Expression {
	private Expression otherwise;
	private List<Pair<Expression, Expression>> switches;

	public Conditional(List<Pair<Expression, Expression>> switches, Expression otherwise) {
		this.switches = switches;
		this.otherwise = otherwise;
	}

	public List<Pair<Expression, Expression>> switches() {
		return switches;
	}

	public Expression otherwise() {
		return otherwise;
	}

	private <JsonNode> void pathRecursive(PathOutput<JsonNode> output, Scope<JsonNode> scope, List<Pair<Expression, Expression>> switches, JsonNode in, @Nullable Path path) throws JsonQueryException {
		Pair<Expression, Expression> sw = switches.get(0);
		sw._1.apply(scope, in, (r) -> {
			if (JsonNodeUtils.asBoolean(scope.jsonProvider(), r)) {
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
	public <JsonNode> void apply(Scope<JsonNode> scope, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		pathRecursive(output, scope, switches, in, path);
	}

	@Override
	public String toString() {
		@Var String ifstr = "if";
		StringBuilder builder = new StringBuilder();
		for (Pair<Expression, Expression> sw : switches) {
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
