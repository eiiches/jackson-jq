package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.List;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class SemicolonOperator<JsonNode> implements Expression<JsonNode> {
	private List<Expression<JsonNode>> qs;

	public SemicolonOperator(List<Expression<JsonNode>> qs) {
		this.qs = qs;
	}

	public List<Expression<JsonNode>> expressions() {
		return qs;
	}

	@Override
	public void apply(ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		if (qs.isEmpty())
			return;
		for (Expression<JsonNode> q : qs.subList(0, qs.size() - 1))
			q.apply(frame, in, (out) -> {});
		qs.get(qs.size() - 1).apply(frame, in, path, output, requirePath);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		@Var String sep = "";
		for (Expression<JsonNode> q : qs) {
			builder.append(sep);
			builder.append(q);
			sep = "; ";
		}
		return builder.toString();
	}
}
