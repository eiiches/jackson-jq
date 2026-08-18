package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.List;
import java.util.Stack;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryBreakException;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.core.internal.utils.PathAndValue;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class PipedQuery<JsonNode> implements Expression<JsonNode> {
	private List<PipeComponent<JsonNode>> components;

	public PipedQuery(List<PipeComponent<JsonNode>> components) {
		this.components = components;
	}

	public List<PipeComponent<JsonNode>> components() {
		return components;
	}

	@Override
	public void apply(@Nullable StackFrame frame, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		pathRecursive(frame, in, path, output, components, requirePath);
	}

	private static <JsonNode> void pathRecursive(@Nullable StackFrame frame, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, List<PipeComponent<JsonNode>> components, boolean requirePath) throws JsonQueryException {
		if (components.isEmpty()) {
			output.emit(in, path);
			return;
		}

		PipeComponent<JsonNode> head = components.get(0);
		List<PipeComponent<JsonNode>> tail = components.subList(1, components.size());

		if (head instanceof AssignPipeComponent) {
			((AssignPipeComponent<JsonNode>) head).expr.apply(frame, in, (o) -> {
				Stack<PatternMatcher.MatchWithPath<JsonNode>> accumulate = new Stack<>();
				((AssignPipeComponent<JsonNode>) head).matcher.matchWithPath(frame, o, path, (List<PatternMatcher.MatchWithPath<JsonNode>> vars) -> {
					// Set values in reverse order since if there is the variable name crash,
					// jq only uses the first match.
					for (int i = vars.size() - 1; i >= 0; --i) {
						PatternMatcher.MatchWithPath<JsonNode> var = vars.get(i);
						if (frame != null && var.slot >= 0) {
							frame.set(var.slot, var.path != null ? new PathAndValue<>(var.path, var.value) : var.value);
						}
					}
					pathRecursive(frame, in, path, output, tail, requirePath);
				}, accumulate);
			});
		} else if (head instanceof TransformPipeComponent) {
			((TransformPipeComponent<JsonNode>) head).expr.apply(frame, in, path, (pobj, ppath) -> {
				pathRecursive(frame, pobj, ppath, output, tail, requirePath);
			}, requirePath);
		} else if (head instanceof LabelPipeComponent) {
			try {
				pathRecursive(frame, in, path, output, tail, requirePath);
			} catch (JsonQueryBreakException e) {
				if (((LabelPipeComponent<JsonNode>) head).name.equals(e.name()))
					return;
				throw e;
			}
		} else {
			throw new IllegalStateException();
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("(");
		@Var String sep = "";
		for (PipeComponent<JsonNode> component : components) {
			builder.append(sep);
			builder.append(component.toString());
			sep = " | ";
		}
		builder.append(")");
		return builder.toString();
	}
}
