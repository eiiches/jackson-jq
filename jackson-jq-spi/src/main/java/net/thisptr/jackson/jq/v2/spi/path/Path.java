package net.thisptr.jackson.jq.v2.spi.path;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

/**
 * An immutable jq path expression: a sequence of object-field / array-index steps from some root.
 * <p>
 * Instances are produced while evaluating {@code path(EXPR)} and consumed by update operators
 * ({@code |=}, {@code +=}, {@code del}, ...) to locate and rewrite the node they refer to.
 *
 * @param <JsonNode> the JSON node type
 */
// TODO: minimize API
public interface Path<JsonNode> {

	/**
	 * Serializes this path into jq's path representation, appending each string/number path
	 * component to the given array node.
	 * <p>
	 * This is what backs the result of the {@code path(EXPR)} builtin.
	 *
	 * @param jsonProvider the JSON provider
	 * @param out the array node to append this path's components to
	 * @throws JsonQueryException if an error occurs while building the representation
	 */
	void toJsonNode(JsonProvider<JsonNode> jsonProvider, JsonNode out) throws JsonQueryException;

	/**
	 * Walks {@code in} following this path, emitting each resolved value together with its
	 * concrete {@link Path} (prefixed by {@code ipath}, the path of {@code in} itself) via
	 * {@code output}.
	 *
	 * @param jsonProvider the JSON provider
	 * @param in the node to resolve this path against
	 * @param ipath the path of {@code in} itself, or {@code null} if untracked
	 * @param output the sink to receive resolved values and their paths
	 * @param permissive mirrors jq's {@code ?} operator: if {@code true}, type-mismatch errors are
	 *                   suppressed by emitting nothing instead of throwing
	 * @throws JsonQueryException if an error occurs during resolution and {@code permissive} is
	 *                            {@code false}
	 */
	void get(JsonProvider<JsonNode> jsonProvider, JsonNode in, @Nullable Path<JsonNode> ipath, Output<JsonNode> output, boolean permissive) throws JsonQueryException;

	/**
	 * A pure function computing the new value for the node currently at a path location.
	 *
	 * @param <JsonNode> the JSON node type
	 */
	interface Mutation<JsonNode> {
		/**
		 * Computes the replacement for the given node.
		 *
		 * @param node the current value at the path location, or {@code null} if absent
		 * @return the new value, or {@code null} to delete this entry
		 * @throws JsonQueryException if an error occurs while computing the replacement
		 */
		@Nullable JsonNode apply(@Nullable JsonNode node) throws JsonQueryException;
	}

	/**
	 * Equivalent to the four-argument {@code mutate} overload with {@code makeParent} set to
	 * {@code true}.
	 *
	 * @param jsonProvider the JSON provider
	 * @param in the node to apply the mutation within
	 * @param mutation the mutation to apply at this path's location
	 * @return the mutated node
	 * @throws JsonQueryException if an error occurs while mutating
	 */
	default JsonNode mutate(JsonProvider<JsonNode> jsonProvider, JsonNode in, Mutation<JsonNode> mutation) throws JsonQueryException {
		return mutate(jsonProvider, in, mutation, true);
	}

	/**
	 * Applies {@code mutation} to the node found at this path's location within {@code in},
	 * returning the resulting (structurally new) node.
	 *
	 * @param jsonProvider the JSON provider
	 * @param in the node to apply the mutation within
	 * @param mutation the mutation to apply at this path's location
	 * @param makeParent if {@code true}, missing intermediate containers (objects/arrays) along
	 *                    this path are auto-vivified so the mutation can proceed, matching jq's
	 *                    implicit path creation in assignment forms; if {@code false}, absent
	 *                    structure is left alone (used by {@code del}/{@code delpaths}, which must
	 *                    not create structure that wasn't there)
	 * @return the mutated node
	 * @throws JsonQueryException if an error occurs while mutating
	 */
	JsonNode mutate(JsonProvider<JsonNode> jsonProvider, JsonNode in, Mutation<JsonNode> mutation, boolean makeParent) throws JsonQueryException;
}
