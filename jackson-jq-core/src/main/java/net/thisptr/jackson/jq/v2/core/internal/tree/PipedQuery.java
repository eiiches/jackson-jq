package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryBreakException;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.core.internal.utils.PathAndValue;
import net.thisptr.jackson.jq.v2.core.path.UnrepresentablePath;
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
	public void apply(@Nullable StackFrame frame, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output) throws JsonQueryException {
		pathRecursive(frame, in, path, output, components);
	}

	private static <JsonNode> void pathRecursive(@Nullable StackFrame frame, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, List<PipeComponent<JsonNode>> components) throws JsonQueryException {
		if (components.isEmpty()) {
			output.emit(in, path);
			return;
		}

		PipeComponent<JsonNode> head = components.get(0);
		List<PipeComponent<JsonNode>> tail = components.subList(1, components.size());

		if (head instanceof AssignPipeComponent) {
			((AssignPipeComponent<JsonNode>) head).expr.apply(frame, in, (o) -> {
				Deque<PatternMatcher.MatchWithPath<JsonNode>> accumulate = new ArrayDeque<>();
				((AssignPipeComponent<JsonNode>) head).matcher.matchWithPath(frame, o, path, (Deque<PatternMatcher.MatchWithPath<JsonNode>> vars) -> {
					// Set values in reverse order since if there is the variable name crash,
					// jq only uses the first match.
					for (Iterator<PatternMatcher.MatchWithPath<JsonNode>> it = vars.descendingIterator(); it.hasNext();) {
						PatternMatcher.MatchWithPath<JsonNode> var = it.next();
						if (frame != null && var.slot >= 0) {
							frame.set(var.slot, var.path != null ? new PathAndValue<>(var.path, var.value) : var.value);
						}
					}
					pathRecursive(frame, in, path, output, tail);
				}, accumulate);
			});
		} else if (head instanceof TransformPipeComponent) {
			((TransformPipeComponent<JsonNode>) head).expr.apply(frame, in, path, (pobj, ppath) -> {
				pathRecursive(frame, pobj, path != null && ppath == null ? UnrepresentablePath.getInstance() : ppath, output, tail);
			});
		} else if (head instanceof LabelPipeComponent) {
			try {
				pathRecursive(frame, in, path, output, tail);
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
