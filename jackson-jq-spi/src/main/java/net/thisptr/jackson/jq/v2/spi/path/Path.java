package net.thisptr.jackson.jq.v2.spi.path;

import java.util.List;

import com.google.errorprone.annotations.CheckReturnValue;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;

/**
 * An immutable jq path expression: a sequence of object-field / array-index steps from some root.
 * <p>
 * A path is data carried alongside an expression result. Concrete path types expose their path
 * components, while traversal, serialization, and mutation are provided by the query engine.
 * <p>
 * Custom implementations are not supported. Use one of the path implementations provided by this
 * package. A package-private constructor prevents subclassing from outside this package; Java 17
 * and later additionally seal this class via {@code permits}.
 *
 * @param <JsonNode> the JSON node type
 */
public abstract class Path<JsonNode> {
	Path() {
	}

	/**
	 * Returns this path as a mutable list of JSON path components.
	 *
	 * @param jsonProvider the JSON provider used to create path components
	 * @return this path's components, ordered from root to leaf
	 */
	public abstract List<JsonNode> toJsonList(JsonProvider<JsonNode> jsonProvider);

	/**
	 * Returns the path preceding this path, if one exists.
	 *
	 * @return the parent path, or {@code null} for a path without a parent
	 */
	public abstract @Nullable Path<JsonNode> getParentPath();

	/**
	 * Returns this path extended with an object field access step.
	 *
	 * @param key the field key
	 * @return a path with this as its parent and {@code key} as its field
	 */
	@CheckReturnValue
	public Path<JsonNode> appendKey(String key) {
		return StringKeyPath.of(this, key);
	}

	/**
	 * Returns this path extended with an array index access step.
	 *
	 * @param index the array index
	 * @return a path with this as its parent and {@code index} as its index
	 */
	@CheckReturnValue
	public Path<JsonNode> appendIndex(int index) {
		return IntIndexPath.of(this, index);
	}

	/**
	 * Returns this path extended with an array index access step.
	 *
	 * @param jsonProvider the JSON provider used to validate the index node
	 * @param index the numeric index node
	 * @return a path with this as its parent and {@code index} as its index
	 * @throws net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException if {@code index} is not a number
	 */
	@CheckReturnValue
	public Path<JsonNode> appendIndex(JsonProvider<JsonNode> jsonProvider, JsonNode index) {
		return NumberIndexPath.of(jsonProvider, this, index);
	}

	/**
	 * Returns this path extended with a slice / range index step.
	 *
	 * @param jsonProvider the JSON provider used to validate the range bounds
	 * @param start the start index node
	 * @param end the end index node
	 * @return a path with this as its parent and {@code [start:end]} as its range
	 * @throws net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException if either range bound is neither a number nor null
	 */
	@CheckReturnValue
	public Path<JsonNode> appendIndexRange(JsonProvider<JsonNode> jsonProvider, JsonNode start, JsonNode end) {
		return IndexRangePath.of(jsonProvider, this, start, end);
	}

	/**
	 * Returns this path extended with an array index-of search step.
	 *
	 * @param jsonProvider the JSON provider used to validate the search sequence
	 * @param searchSequence the target search sequence to search for
	 * @return a path with this as its parent and {@code searchSequence} as its search sequence
	 * @throws net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException if {@code searchSequence} is not an array
	 */
	@CheckReturnValue
	public Path<JsonNode> appendIndexOf(JsonProvider<JsonNode> jsonProvider, JsonNode searchSequence) {
		return IndexOfPath.of(jsonProvider, this, searchSequence);
	}

	/**
	 * Returns this path extended with an invalid path step.
	 *
	 * @param index the invalid index or field node
	 * @return a path with this as its parent and {@code index} as its invalid index or field
	 */
	@CheckReturnValue
	public Path<JsonNode> appendInvalid(JsonNode index) {
		return InvalidPath.of(this, index);
	}
}
