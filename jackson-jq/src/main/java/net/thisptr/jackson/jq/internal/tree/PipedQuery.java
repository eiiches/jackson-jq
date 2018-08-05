package net.thisptr.jackson.jq.internal.tree;

import java.util.List;
import java.util.Stack;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryBreakException;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Pair;

public class PipedQuery implements Expression {
	private List<PipeComponent> components;

	public PipedQuery(final List<PipeComponent> components) {
		this.components = components;
	}

	@Override
	public void apply(Scope scope, JsonNode in, final Output output) throws JsonQueryException {
		applyRecursive(scope, in, output, components);
	}

	private static void applyRecursive(final Scope scope, final JsonNode in, final Output output, final List<PipeComponent> components) throws JsonQueryException {
		if (components.isEmpty()) {
			output.emit(in);
			return;
		}

		final PipeComponent head = components.get(0);
		final List<PipeComponent> tail = components.subList(1, components.size());

		if (head instanceof AssignPipeComponent) {
			final Scope childScope = Scope.newChildScope(scope);
			((AssignPipeComponent) head).expr.apply(scope, in, (o) -> {
				final Stack<Pair<String, JsonNode>> accumulate = new Stack<>();
				((AssignPipeComponent) head).matcher.match(scope, o, (final List<Pair<String, JsonNode>> vars) -> {
					// Set values in reverse order since if there is the variable name crash,
					// jq only uses the first match.
					for (int i = vars.size() - 1; i >= 0; --i) {
						final Pair<String, JsonNode> var = vars.get(i);
						childScope.setValue(var._1, var._2);
					}
					applyRecursive(childScope, in, output, tail);
				}, accumulate, true);
			});
		} else if (head instanceof TransformPipeComponent) {
			((TransformPipeComponent) head).expr.apply(scope, in, (o) -> {
				applyRecursive(scope, o, output, tail);
			});
		} else if (head instanceof LabelPipeComponent) {
			try {
				applyRecursive(scope, in, output, tail);
			} catch (JsonQueryBreakException e) {
				if (((LabelPipeComponent) head).name.equals(e.name()))
					return;
				throw e;
			}
		} else {
			throw new IllegalStateException();
		}
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder("(");
		String sep = "";
		for (final PipeComponent component : components) {
			builder.append(sep);
			builder.append(component.toString());
			sep = " | ";
		}
		builder.append(")");
		return builder.toString();
	}
}
