package net.thisptr.jackson.jq.v2.json;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

public interface JsonProvider<JsonNode> {
	JsonNode createObject();

	JsonNode createArray();

	JsonNode createString(String value);

	JsonNode createNumber(long value);

	JsonNode createNumber(int value);

	JsonNode createNumber(double value);

	JsonNode createBoolean(boolean value);

	JsonNode createNull();

	JsonNode createMissing();

	JsonNodeType getNodeType(JsonNode node);

	boolean isMissingNode(JsonNode node);

	boolean asBoolean(JsonNode node);

	double asDouble(JsonNode node);

	String asText(JsonNode node);

	/**
	 * Returns the value of the node as a long.
	 * <p>
	 * This method has strict semantics and will throw an exception if the value
	 * cannot be represented as a long without loss of information.
	 *
	 * @param node the JSON node
	 * @return the long value
	 * @throws IllegalArgumentException if the value is NaN, Infinity, or cannot be
	 *                                  represented as a long
	 */
	long asLong(JsonNode node);

	/**
	 * Returns the value of the node as an int.
	 * <p>
	 * This method has strict semantics and will throw an exception if the value
	 * cannot be represented as an int without loss of information.
	 *
	 * @param node the JSON node
	 * @return the int value
	 * @throws IllegalArgumentException if the value is NaN, Infinity, or outside
	 *                                  the range of int
	 */
	int asInt(JsonNode node);

	byte[] asByteArray(JsonNode node);

	Iterator<Entry<String, JsonNode>> fields(JsonNode node);

	Iterator<JsonNode> elements(JsonNode node);

	Iterator<String> fieldNames(JsonNode node);

	/**
	 * Returns an Iterable over elements of an array node for use in foreach loops.
	 */
	default Iterable<JsonNode> iterate(JsonNode node) {
		return () -> elements(node);
	}

	@Nullable JsonNode get(JsonNode node, String fieldName);

	@Nullable JsonNode get(JsonNode node, int index);

	default JsonNode requireGet(JsonNode node, String fieldName) {
		return Objects.requireNonNull(get(node, fieldName));
	}

	default JsonNode requireGet(JsonNode node, int index) {
		return Objects.requireNonNull(get(node, index));
	}

	/**
	 * Sets a field on an object node. For mutable implementations, this modifies the node in place.
	 * For immutable implementations, this returns a new node.
	 *
	 * @return the modified node (same as input for mutable implementations)
	 */
	JsonNode set(JsonNode node, String fieldName, JsonNode value);

	/**
	 * Adds a value to an array node. For mutable implementations, this modifies the node in place.
	 * For immutable implementations, this returns a new node.
	 *
	 * @return the modified node (same as input for mutable implementations)
	 */
	JsonNode add(JsonNode node, JsonNode value);

	/**
	 * Sets an element at a specific index in an array node.
	 *
	 * @return the modified node (same as input for mutable implementations)
	 */
	JsonNode set(JsonNode node, int index, JsonNode value);

	int size(JsonNode node);

	boolean has(JsonNode node, String fieldName);

	boolean has(JsonNode node, int index);

	JsonNode deepCopy(JsonNode node);

	String toString(JsonNode node);

	JsonNode fromString(String json) throws Exception;

	/**
	 * Parses multiple JSON documents from a string.
	 * Used for loading configuration files containing multiple JSON values.
	 */
	List<JsonNode> readMultipleValues(String json) throws Exception;

	/**
	 * Parses a JSON string strictly, ensuring no trailing content exists.
	 *
	 * @param json The JSON string to parse
	 * @return The parsed JSON node
	 * @throws Exception if parsing fails or trailing content exists
	 */
	default JsonNode fromStringStrict(String json) throws Exception {
		return fromString(json);
	}

	/**
	 * Converts a Java object to a JSON node.
	 */
	JsonNode valueToTree(@Nullable Object value);

	// TODO: We should instead add Class<JsonNode> getNodeClass().
	boolean isJsonNodeInstance(@Nullable Object arg);
}
