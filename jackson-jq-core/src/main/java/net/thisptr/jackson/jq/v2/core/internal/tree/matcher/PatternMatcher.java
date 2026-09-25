package net.thisptr.jackson.jq.v2.core.internal.tree.matcher;

import java.util.function.UnaryOperator;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

/**
 * A compiled destructuring pattern. A matcher writes each variable it binds straight into the
 * {@link StackFrame} slot resolved for it, then calls {@link OnMatch} once per complete match.
 * <p>
 * Writing eagerly, rather than collecting the bindings and applying them once the whole pattern has
 * matched, is sound because the <em>sequence</em> of slots a matcher writes is fixed at compile
 * time: nothing in the tree matches conditionally, and the one source of runtime branching -- an
 * object key expression emitting several keys -- repeats the identical write sequence on every
 * branch. So every complete match writes every slot, and a later branch always fully overwrites an
 * earlier one. Duplicate variables, where jq keeps the <em>first</em> occurrence in traversal order,
 * are likewise decided statically: see {@link SlotResolver#claim}.
 * <p>
 * Nothing can observe a half-written frame either. An object key expression is compiled before the
 * pattern's own scope is pushed, so it can never name a variable the pattern binds, and the slots a
 * pattern binds are freshly allocated above every slot that is already live. If a matcher throws
 * part-way through, the values it already wrote are unreachable -- the body never runs.
 */
public interface PatternMatcher<JsonNode> {
	default Cardinality getCardinality() {
		return Cardinality.ONE;
	}

	default boolean dependsOnInput() {
		return false;
	}

	default boolean dependsOnExternalState() {
		return false;
	}

	/**
	 * Invoked once per complete match, after every slot the pattern binds has been written into the
	 * frame.
	 */
	interface OnMatch {
		void matched() throws JsonQueryException;
	}

	/**
	 * A visitor over every pattern a jq destructuring can compile to.
	 * <p>
	 * The three implementations below are the whole language: a variable, an array pattern and an object
	 * pattern. Analyses dispatch through this rather than on {@code instanceof} so that adding a fourth
	 * matcher breaks every analysis at compile time instead of silently taking some default branch.
	 *
	 * @param <JsonNode> the JSON node type
	 * @param <R> what the visit produces
	 */
	interface Visitor<JsonNode, R> {
		R visit(ValueMatcher<JsonNode> matcher);

		R visit(ArrayMatcher<JsonNode> matcher);

		R visit(ObjectMatcher<JsonNode> matcher);
	}

	/**
	 * Dispatches to the {@link Visitor} method for this matcher's kind.
	 */
	<R> R accept(Visitor<JsonNode, R> visitor);

	void match(StackFrame frame, JsonNode in, OnMatch onMatch) throws JsonQueryException;

	void matchWithPath(StackFrame frame, JsonNode in, Path<JsonNode> path, OnMatch onMatch) throws JsonQueryException;

	/**
	 * Returns a copy of this matcher with every variable it binds resolved to a frame slot.
	 * Implementations must visit their children in the same order they traverse them at runtime, so
	 * that {@code resolver} sees duplicate variables in the order the runtime would write them.
	 */
	PatternMatcher<JsonNode> resolveSlots(SlotResolver resolver);

	/**
	 * Rewrites expressions embedded in this matcher, returning this matcher when none changed.
	 */
	default PatternMatcher<JsonNode> rewriteExpressions(UnaryOperator<AnalyzedExpression<JsonNode>> rewriter) {
		return this;
	}
}
