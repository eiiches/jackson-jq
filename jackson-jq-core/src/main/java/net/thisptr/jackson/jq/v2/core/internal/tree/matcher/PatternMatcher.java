package net.thisptr.jackson.jq.v2.core.internal.tree.matcher;

import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
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
	/**
	 * Invoked once per complete match, after every slot the pattern binds has been written into the
	 * frame.
	 */
	interface OnMatch {
		void matched() throws JsonQueryException;
	}

	void match(StackFrame frame, JsonNode in, OnMatch onMatch) throws JsonQueryException;

	void matchWithPath(StackFrame frame, JsonNode in, Path<JsonNode> path, OnMatch onMatch) throws JsonQueryException;

	/**
	 * Returns a copy of this matcher with every variable it binds resolved to a frame slot.
	 * Implementations must visit their children in the same order they traverse them at runtime, so
	 * that {@code resolver} sees duplicate variables in the order the runtime would write them.
	 */
	PatternMatcher<JsonNode> resolveSlots(SlotResolver resolver);
}
