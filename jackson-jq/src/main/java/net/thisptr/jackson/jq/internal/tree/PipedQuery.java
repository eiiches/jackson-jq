package net.thisptr.jackson.jq.internal.tree;

import java.util.Collections;
import java.util.List;
import java.util.Stack;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryBreakException;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.tree.matcher.PatternMatcher.MatchWithPath;
import net.thisptr.jackson.jq.path.Path;

public class PipedQuery implements Expression {
	private List<PipeComponent> components;

	public PipedQuery() {}

	public PipedQuery(final List<PipeComponent> components) {
		this.components = components;
	}

	public List<PipeComponent> getComponents() {
		return Collections.unmodifiableList(components);
	}

	public void setComponents(List<PipeComponent> components) {
		this.components = components;
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Path path, final PathOutput output, final boolean requirePath) throws JsonQueryException {
		pathRecursive(scope, in, path, output, components, requirePath);
	}

	private static void pathRecursive(final Scope scope, final JsonNode in, final Path path, final PathOutput output, final List<PipeComponent> components, final boolean requirePath) throws JsonQueryException {
		if (components.isEmpty()) {
			output.emit(in, path);
			return;
		}

		final PipeComponent head = components.get(0);
		final List<PipeComponent> tail = components.subList(1, components.size());

		if (head instanceof AssignPipeComponent) {
			final Scope childScope = Scope.newChildScope(scope);
			((AssignPipeComponent) head).expr.apply(scope, in, (o) -> {
				final Stack<MatchWithPath> accumulate = new Stack<>();
				((AssignPipeComponent) head).matcher.matchWithPath(scope, o, path, (final List<MatchWithPath> vars) -> {
					// Set values in reverse order since if there is the variable name crash,
					// jq only uses the first match.
					for (int i = vars.size() - 1; i >= 0; --i) {
						final MatchWithPath var = vars.get(i);
						childScope.setValueWithPath(var.name, var.value, var.path);
					}
					pathRecursive(childScope, in, path, output, tail, requirePath);
				}, accumulate);
			});
		} else if (head instanceof TransformPipeComponent) {
			((TransformPipeComponent) head).expr.apply(scope, in, path, (pobj, ppath) -> {
				pathRecursive(scope, pobj, ppath, output, tail, requirePath);
			}, requirePath);
		} else if (head instanceof LabelPipeComponent) {
			try {
				pathRecursive(scope, in, path, output, tail, requirePath);
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
