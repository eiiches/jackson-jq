package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.List;
import java.util.Stack;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryBreakException;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher.MatchWithPath;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class PipedQuery<JsonNode> implements Expression {
	private List<PipeComponent<JsonNode>> components;

	public PipedQuery(List<PipeComponent<JsonNode>> components) {
		this.components = components;
	}

	public List<PipeComponent<JsonNode>> components() {
		return components;
	}

	@Override
	public <N> void apply(JsonProvider<N> jsonProvider, ExecutionStack<N>.@Nullable Frame frame, N in, @Nullable Path<N> path, PathOutput<N> output, boolean requirePath) throws JsonQueryException {
		applyInternal((JsonProvider) jsonProvider, (ExecutionStack.Frame) frame, (JsonNode) in, (Path) path, (PathOutput) output, requirePath);
	}

	private void applyInternal(JsonProvider<JsonNode> jsonProvider, ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		pathRecursive(jsonProvider, frame, in, path, output, components, requirePath);
	}

	private static <JsonNode> void pathRecursive(JsonProvider<JsonNode> jsonProvider, ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, List<PipeComponent<JsonNode>> components, boolean requirePath) throws JsonQueryException {
		if (components.isEmpty()) {
			output.emit(in, path);
			return;
		}

		PipeComponent<JsonNode> head = components.get(0);
		List<PipeComponent<JsonNode>> tail = components.subList(1, components.size());

		if (head instanceof AssignPipeComponent) {
			((AssignPipeComponent<JsonNode>) head).expr.apply(jsonProvider, frame, in, (o) -> {
				Stack<MatchWithPath<JsonNode>> accumulate = new Stack<>();
				((AssignPipeComponent<JsonNode>) head).matcher.matchWithPath(jsonProvider, frame, o, path, (List<MatchWithPath<JsonNode>> vars) -> {
					// Set values in reverse order since if there is the variable name crash,
					// jq only uses the first match.
					for (int i = vars.size() - 1; i >= 0; --i) {
						MatchWithPath<JsonNode> var = vars.get(i);
						int slot = ((AssignPipeComponent<JsonNode>) head).getSlot(var.name);
						if (frame != null && slot >= 0) {
							frame.set(slot, var.path, var.value);
						}
					}
					pathRecursive(jsonProvider, frame, in, path, output, tail, requirePath);
				}, accumulate);
			});
		} else if (head instanceof TransformPipeComponent) {
			((TransformPipeComponent<JsonNode>) head).expr.apply(jsonProvider, frame, in, path, (pobj, ppath) -> {
				pathRecursive(jsonProvider, frame, pobj, ppath, output, tail, requirePath);
			}, requirePath);
		} else if (head instanceof LabelPipeComponent) {
			try {
				pathRecursive(jsonProvider, frame, in, path, output, tail, requirePath);
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
