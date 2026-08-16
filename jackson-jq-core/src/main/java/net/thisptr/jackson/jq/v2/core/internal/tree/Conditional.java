package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.List;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.misc.Pair;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
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

	@Override
	public <JsonNode> void apply(JsonProvider<JsonNode> jsonProvider, ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		applyBranch(jsonProvider, frame, in, path, output, 0);
	}

	private <JsonNode> void applyBranch(JsonProvider<JsonNode> jsonProvider, ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, int switchIndex) throws JsonQueryException {
		if (switchIndex >= switches.size()) {
			if (otherwise != null) {
				otherwise.apply(jsonProvider, frame, in, path, output, false);
			}
			return;
		}
		Pair<Expression, Expression> sw = switches.get(switchIndex);
		java.util.List<JsonNode> condValues = new java.util.ArrayList<>();
		sw._1.apply(jsonProvider, frame, in, (r) -> condValues.add(r));

		for (JsonNode r : condValues) {
			if (JsonNodeUtils.asBoolean(jsonProvider, r)) {
				sw._2.apply(jsonProvider, frame, in, path, output, false);
			} else {
				applyBranch(jsonProvider, frame, in, path, output, switchIndex + 1);
			}
		}
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
