package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryBreakException;
import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.core.internal.utils.PathAndValue;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UnrepresentablePath;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public class PipedQuery<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	private List<PipeComponent<JsonNode>> components;

	@Override
	public Cardinality getCardinality() {
		if (components.isEmpty())
			return Cardinality.ONE;
		for (PipeComponent<JsonNode> comp : components) {
			if (comp instanceof AssignPipeComponent) {
				if (((AssignPipeComponent<JsonNode>) comp).expr.getCardinality() == Cardinality.ZERO)
					return Cardinality.ZERO;
			} else if (comp instanceof TransformPipeComponent) {
				if (((TransformPipeComponent<JsonNode>) comp).expr.getCardinality() == Cardinality.ZERO)
					return Cardinality.ZERO;
			}
		}
		for (PipeComponent<JsonNode> comp : components) {
			if (comp instanceof LabelPipeComponent)
				return Cardinality.UNKNOWN;
		}
		return CardinalityUtils.multiply(components, comp -> {
			if (comp instanceof AssignPipeComponent)
				return ((AssignPipeComponent<JsonNode>) comp).expr.getCardinality();
			if (comp instanceof TransformPipeComponent)
				return ((TransformPipeComponent<JsonNode>) comp).expr.getCardinality();
			return Cardinality.UNKNOWN;
		});
	}

	private final boolean dependsOnInput;
	private final boolean dependsOnExternalState;
	private final Set<Integer> freeLocalSlots;
	private final boolean hasOpaqueVariableReference;

	public PipedQuery(List<PipeComponent<JsonNode>> components) {
		this.components = components;

		// dependsOnInput/dependsOnExternalState: the shielding chain for `.` is already resolved by
		// the compiler as it compiles each stage in order (see Compiler's PipedQueryAstNode handling),
		// so by construction time this is just a flat OR over every component's already-correct expr.
		@Var boolean anyDependsOnInput = false;
		@Var boolean anyDependsOnExternalState = false;
		@Var boolean anyOpaque = false;
		List<Expression<StackFrame, JsonNode>> exprs = new ArrayList<>(components.size());
		List<Integer> allBoundSlots = new ArrayList<>();
		for (PipeComponent<JsonNode> component : components) {
			Expression<StackFrame, JsonNode> expr;
			if (component instanceof AssignPipeComponent) {
				AssignPipeComponent<JsonNode> assign = (AssignPipeComponent<JsonNode>) component;
				expr = assign.expr;
				allBoundSlots.addAll(assign.boundSlots);
			} else if (component instanceof TransformPipeComponent) {
				expr = ((TransformPipeComponent<JsonNode>) component).expr;
			} else {
				continue; // LabelPipeComponent has no expr to contribute.
			}
			exprs.add(expr);
			anyDependsOnInput = anyDependsOnInput || expr.dependsOnInput();
			anyDependsOnExternalState = anyDependsOnExternalState || expr.dependsOnExternalState();
			anyOpaque = anyOpaque || FreeVariables.opaqueIn(expr);
		}
		this.dependsOnInput = anyDependsOnInput;
		this.dependsOnExternalState = anyDependsOnExternalState;
		this.hasOpaqueVariableReference = anyOpaque;
		// Every `as`-bound slot in this pipe is "closed" -- subtracted from what every component
		// (including other components' own bound expressions) contributes as free.
		this.freeLocalSlots = FreeVariables.minus(FreeVariables.unionAll(exprs), allBoundSlots);
	}

	public List<PipeComponent<JsonNode>> components() {
		return components;
	}

	@Override
	public boolean dependsOnInput() {
		return dependsOnInput;
	}

	@Override
	public boolean dependsOnExternalState() {
		return dependsOnExternalState;
	}

	@Override
	public Set<Integer> freeLocalSlots() {
		return freeLocalSlots;
	}

	@Override
	public boolean hasOpaqueVariableReference() {
		return hasOpaqueVariableReference;
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		pathRecursive(frame, in, path, output, components);
	}

	private static <JsonNode> void pathRecursive(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output, List<PipeComponent<JsonNode>> components) throws JsonQueryException {
		if (components.isEmpty()) {
			output.emit(in, path);
			return;
		}

		PipeComponent<JsonNode> head = components.get(0);
		List<PipeComponent<JsonNode>> tail = components.subList(1, components.size());

		if (head instanceof AssignPipeComponent) {
			((AssignPipeComponent<JsonNode>) head).expr.apply(frame, in, UntrackedPath.getInstance(), (o, opath) -> {
				Deque<PatternMatcher.MatchWithPath<JsonNode>> accumulate = new ArrayDeque<>();
				((AssignPipeComponent<JsonNode>) head).matcher.matchWithPath(frame, o, path, (Deque<PatternMatcher.MatchWithPath<JsonNode>> vars) -> {
					// Set values in reverse order since if there is the variable name crash,
					// jq only uses the first match.
					for (Iterator<PatternMatcher.MatchWithPath<JsonNode>> it = vars.descendingIterator(); it.hasNext(); ) {
						PatternMatcher.MatchWithPath<JsonNode> var = it.next();
						if (var.slot >= 0) {
							frame.set(var.slot, var.path instanceof UntrackedPath ? var.value : new PathAndValue<>(var.path, var.value));
						}
					}
					pathRecursive(frame, in, path, output, tail);
				}, accumulate);
			});
		} else if (head instanceof TransformPipeComponent) {
			((TransformPipeComponent<JsonNode>) head).expr.apply(frame, in, path, (pobj, ppath) -> {
				pathRecursive(frame, pobj, !(path instanceof UntrackedPath) && ppath instanceof UntrackedPath ? UnrepresentablePath.getInstance() : ppath, output, tail);
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
