package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.List;
import java.util.Stack;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryBreakException;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher.MatchWithPath;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
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
	@SuppressWarnings({"unchecked", "rawtypes"})
	public <N> void apply(Scope<N> scope, N in, @Nullable Path<N> path, PathOutput<N> output, boolean requirePath) throws JsonQueryException {
		applyInternal((Scope) scope, (JsonNode) in, (Path) path, (PathOutput) output, requirePath);
	}

	private void applyInternal(Scope<JsonNode> scope, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		pathRecursive(scope, in, path, output, components, requirePath);
	}

	private static <JsonNode> void pathRecursive(Scope<JsonNode> scope, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, List<PipeComponent<JsonNode>> components, boolean requirePath) throws JsonQueryException {
		if (components.isEmpty()) {
			output.emit(in, path);
			return;
		}

		PipeComponent<JsonNode> head = components.get(0);
		List<PipeComponent<JsonNode>> tail = components.subList(1, components.size());

		if (head instanceof AssignPipeComponent) {
			Scope<JsonNode> childScope = Scope.newChildScope(scope);
			((AssignPipeComponent<JsonNode>) head).expr.apply(scope, in, (o) -> {
				Stack<MatchWithPath<JsonNode>> accumulate = new Stack<>();
				((AssignPipeComponent<JsonNode>) head).matcher.matchWithPath(scope, o, path, (List<MatchWithPath<JsonNode>> vars) -> {
					// Set values in reverse order since if there is the variable name crash,
					// jq only uses the first match.
					for (int i = vars.size() - 1; i >= 0; --i) {
						MatchWithPath<JsonNode> var = vars.get(i);
						int slot = ((AssignPipeComponent<JsonNode>) head).getSlot(var.name);
						childScope.setValueWithPath(slot, var.value, var.path);
					}
					pathRecursive(childScope, in, path, output, tail, requirePath);
				}, accumulate);
			});
		} else if (head instanceof TransformPipeComponent) {
			((TransformPipeComponent<JsonNode>) head).expr.apply(scope, in, path, (pobj, ppath) -> {
				pathRecursive(scope, pobj, ppath, output, tail, requirePath);
			}, requirePath);
		} else if (head instanceof LabelPipeComponent) {
			try {
				pathRecursive(scope, in, path, output, tail, requirePath);
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
