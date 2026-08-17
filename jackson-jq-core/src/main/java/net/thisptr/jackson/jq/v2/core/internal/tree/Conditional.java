package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.List;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.misc.Pair;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class Conditional<JsonNode> implements Expression<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	private Expression<JsonNode> otherwise;
	private List<Pair<Expression<JsonNode>, Expression<JsonNode>>> switches;

	public Conditional(JsonProvider<JsonNode> jsonProvider, List<Pair<Expression<JsonNode>, Expression<JsonNode>>> switches, Expression<JsonNode> otherwise) {
		this.jsonProvider = jsonProvider;
		this.switches = switches;
		this.otherwise = otherwise;
	}

	public List<Pair<Expression<JsonNode>, Expression<JsonNode>>> switches() {
		return switches;
	}

	public Expression<JsonNode> otherwise() {
		return otherwise;
	}

	@Override
	public void apply(@Nullable StackFrame frame, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		applyBranch(frame, in, path, output, 0);
	}

	private void applyBranch(@Nullable StackFrame frame, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, int switchIndex) throws JsonQueryException {
		if (switchIndex >= switches.size()) {
			if (otherwise != null) {
				otherwise.apply(frame, in, path, output, false);
			}
			return;
		}
		Pair<Expression<JsonNode>, Expression<JsonNode>> sw = switches.get(switchIndex);
		java.util.List<JsonNode> condValues = new java.util.ArrayList<>();
		sw._1.apply(frame, in, (r) -> condValues.add(r));

		for (JsonNode r : condValues) {
			if (JsonNodeUtils.asBoolean(jsonProvider, r)) {
				sw._2.apply(frame, in, path, output, false);
			} else {
				applyBranch(frame, in, path, output, switchIndex + 1);
			}
		}
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
