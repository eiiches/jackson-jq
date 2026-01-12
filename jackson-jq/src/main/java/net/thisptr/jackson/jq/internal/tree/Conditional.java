package net.thisptr.jackson.jq.internal.tree;

import java.util.List;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.misc.Pair;
import net.thisptr.jackson.jq.path.Path;

public class Conditional<JsonNode> implements Expression<JsonNode> {
	private Expression<JsonNode> otherwise;
	private List<Pair<Expression<JsonNode>, Expression<JsonNode>>> switches;

	public Conditional(final List<Pair<Expression<JsonNode>, Expression<JsonNode>>> switches, final Expression<JsonNode> otherwise) {
		this.switches = switches;
		this.otherwise = otherwise;
	}

	private void pathRecursive(PathOutput<JsonNode> output, Scope<JsonNode> scope, List<Pair<Expression<JsonNode>, Expression<JsonNode>>> switches, JsonNode in, Path path) throws JsonQueryException {
		final Pair<Expression<JsonNode>, Expression<JsonNode>> sw = switches.get(0);
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
	public void apply(final Scope<JsonNode> scope, final JsonNode in, final Path<JsonNode> path, final PathOutput<JsonNode> output, final boolean requirePath) throws JsonQueryException {
		pathRecursive(output, scope, switches, in, path);
	}

	@Override
	public String toString() {
		String ifstr = "if";
		final StringBuilder builder = new StringBuilder();
		for (final Pair<Expression<JsonNode>, Expression<JsonNode>> sw : switches) {
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
