package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.List;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.misc.Pair;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class Conditional<JsonNode> implements Expression<JsonNode> {
	private Expression<JsonNode> otherwise;
	private List<Pair<Expression<JsonNode>, Expression<JsonNode>>> switches;

	public Conditional(List<Pair<Expression<JsonNode>, Expression<JsonNode>>> switches, Expression<JsonNode> otherwise) {
		this.switches = switches;
		this.otherwise = otherwise;
	}

	private void pathRecursive(PathOutput<JsonNode> output, Scope<JsonNode> scope, List<Pair<Expression<JsonNode>, Expression<JsonNode>>> switches, JsonNode in, Path path) throws JsonQueryException {
		Pair<Expression<JsonNode>, Expression<JsonNode>> sw = switches.get(0);
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
	public void apply(Scope<JsonNode> scope, JsonNode in, Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		pathRecursive(output, scope, switches, in, path);
	}

	@Override
	public String toString() {
		@Var String ifstr = "if";
		StringBuilder builder = new StringBuilder();
		for (Pair<Expression<JsonNode>, Expression<JsonNode>> sw : switches) {
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
