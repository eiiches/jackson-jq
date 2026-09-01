package net.thisptr.jackson.jq.v2.spi;

import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

/**
 * A sink for the values an {@link Expression} produces.
 *
 * @param <JsonNode> the JSON node type
 */
public interface Output<JsonNode> {

	/**
	 * Emits one output value, together with the path it was reached at, if known.
	 * <p>
	 * Pass the concrete {@link Path} when {@code out} was derived from the input by pure indexing
	 * (e.g. field/array access), so that {@code path(EXPR)} and update operators such as {@code |=}
	 * and {@code del} can track it. Pass {@link UntrackedPath#getInstance()} when {@code out} has no
	 * traceable path -- because it was synthesized, computed, or is otherwise unrelated to the input
	 * -- the same way {@link Expression#apply} treats its own input path argument.
	 *
	 * @param out the output JSON node
	 * @param path the path {@code out} was reached at, or {@link UntrackedPath#getInstance()} if untracked
	 * @throws JsonQueryException if an error occurs while consuming the output
	 */
	void emit(JsonNode out, Path<JsonNode> path) throws JsonQueryException;
}
