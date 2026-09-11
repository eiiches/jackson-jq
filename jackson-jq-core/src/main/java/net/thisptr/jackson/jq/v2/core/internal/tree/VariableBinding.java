package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.core.internal.path.PathAndValue;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public class VariableBinding<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	private final Expression<StackFrame, JsonNode> value;
	private final PatternMatcher<JsonNode> matcher;
	private final Expression<StackFrame, JsonNode> body;
	private final boolean dependsOnInput;
	private final boolean dependsOnExternalState;
	private final Set<Integer> freeLocalSlots;
	private final boolean hasOpaqueVariableReference;

	public VariableBinding(Expression<StackFrame, JsonNode> value, PatternMatcher<JsonNode> matcher, Set<Integer> boundSlots, Expression<StackFrame, JsonNode> body) {
		this.value = value;
		this.matcher = matcher;
		this.body = body;
		this.dependsOnInput = value.dependsOnInput() || body.dependsOnInput();
		this.dependsOnExternalState = value.dependsOnExternalState() || body.dependsOnExternalState();
		this.freeLocalSlots = FreeVariables.unionSets(FreeVariables.slotsOf(value), FreeVariables.minus(FreeVariables.slotsOf(body), boundSlots));
		this.hasOpaqueVariableReference = FreeVariables.anyOpaque(value, body);
	}

	@Override
	public Cardinality getCardinality() {
		return CardinalityUtils.multiply(value.getCardinality(), body.getCardinality());
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
		value.apply(frame, in, UntrackedPath.getInstance(), (matchedValue, ignoredPath) -> {
			Deque<PatternMatcher.MatchWithPath<JsonNode>> accumulate = new ArrayDeque<>();
			matcher.matchWithPath(frame, matchedValue, path, (Deque<PatternMatcher.MatchWithPath<JsonNode>> variables) -> {
				// Set values in reverse order since if there is a variable name clash, jq only uses the first match.
				for (Iterator<PatternMatcher.MatchWithPath<JsonNode>> it = variables.descendingIterator(); it.hasNext(); ) {
					PatternMatcher.MatchWithPath<JsonNode> variable = it.next();
					if (variable.slot >= 0)
						frame.set(variable.slot, variable.path instanceof UntrackedPath ? variable.value : new PathAndValue<>(variable.path, variable.value));
				}
				body.apply(frame, in, path, output);
			}, accumulate);
		});
	}
}
