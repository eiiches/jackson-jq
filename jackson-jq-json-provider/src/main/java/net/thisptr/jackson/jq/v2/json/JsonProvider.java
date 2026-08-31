package net.thisptr.jackson.jq.v2.json;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

/**
 * Abstracts jq's JSON tree operations over a concrete JSON library, so that the jq engine can run
 * against different JSON representations (e.g. Jackson's {@code JsonNode}, Gson's {@code JsonElement},
 * or a Jakarta JSON-P {@code JsonValue}) without depending on any one of them directly.
 * <p>
 * The type parameter {@code JsonNode} is the native, immutable tree node type of the underlying JSON
 * library; implementations wrap that library's parser/tree-model APIs. Nodes are expected to be
 * treated as immutable by callers: mutating operations return new nodes rather than modifying
 * existing ones in place.
 * <p>
 * Implementations are expected to be stateless and safe to share as singletons (see each
 * implementation's {@code getInstance()} method).
 *
 * @param <JsonNode> the native JSON tree node type used by the underlying JSON library
 */
// FIXME: too many methods
public interface JsonProvider<JsonNode> {
	/**
	 * Creates an array containing the supplied values.
	 *
	 * @param values the values to add
	 * @return the created array
	 */
	JsonNode createArray(Iterable<? extends JsonNode> values);

	/**
	 * Creates an object containing the supplied fields.
	 *
	 * @param values the fields to add
	 * @return the created object
	 */
	JsonNode createObject(Map<String, ? extends JsonNode> values);

	/**
	 * Creates a string node holding the given value.
	 *
	 * @param value the string value
	 * @return the created string node
	 */
	JsonNode createString(String value);

	/**
	 * Creates a number node holding the given long value.
	 *
	 * @param value the numeric value
	 * @return the created number node
	 */
	JsonNode createNumber(long value);

	/**
	 * Creates a number node holding the given int value.
	 *
	 * @param value the numeric value
	 * @return the created number node
	 */
	JsonNode createNumber(int value);

	/**
	 * Creates a number node holding the given float value.
	 *
	 * @param value the numeric value
	 * @return the created number node
	 */
	JsonNode createNumber(float value);

	/**
	 * Creates a number node holding the given double value.
	 *
	 * @param value the numeric value
	 * @return the created number node
	 */
	JsonNode createNumber(double value);

	/**
	 * Creates a number node holding the given arbitrary-precision integer value.
	 *
	 * @param value the numeric value
	 * @return the created number node
	 */
	JsonNode createNumber(BigInteger value);

	/**
	 * Creates a number node holding the given arbitrary-precision decimal value.
	 *
	 * @param value the numeric value
	 * @return the created number node
	 */
	JsonNode createNumber(BigDecimal value);

	/**
	 * Creates a boolean node holding the given value.
	 *
	 * @param value the boolean value
	 * @return the created boolean node
	 */
	JsonNode createBoolean(boolean value);

	/**
	 * Creates a JSON {@code null} node.
	 *
	 * @return the null node
	 */
	JsonNode createNull();

	/**
	 * Classifies the given node into one of {@link JsonNodeType}'s categories.
	 *
	 * @param node the JSON node
	 * @return the node's type
	 */
	JsonNodeType getNodeType(JsonNode node);

	/**
	 * Returns the Java type this provider uses to represent the given number node.
	 * <p>
	 * Unlike {@link #asInt(Object)} and friends, this answers without converting the value, so it is
	 * a cheap way to ask what a number actually is. The answer describes the representation rather
	 * than the logical value, and is deliberately not promised to be the same across providers: a
	 * provider that widens on construction, or that stores every number the same way, reports what it
	 * actually holds. {@link NumberType#UNKNOWN} means the node is a number whose representation the
	 * provider does not track.
	 *
	 * @param node the JSON number node
	 * @return the representation of {@code node}
	 * @throws IllegalArgumentException if the node is not a number
	 */
	NumberType getNumberType(JsonNode node);

	/**
	 * Returns the node's value under jq truthiness semantics: {@code null} and the boolean
	 * {@code false} are falsy, every other value (including {@code 0}, {@code ""}, and empty
	 * arrays/objects) is truthy.
	 *
	 * @param node the JSON node
	 * @return {@code false} if the node is JSON {@code null} or boolean {@code false}, {@code true} otherwise
	 */
	boolean asBoolean(JsonNode node);

	/**
	 * Returns the value of the node as a double, on a best-effort basis.
	 * <p>
	 * Number nodes are converted directly, which may lose precision for values outside the range
	 * exactly representable by {@code double}. String nodes are parsed as a number if possible.
	 *
	 * @param node the JSON node
	 * @return the double value, or {@link Double#NaN} if the node cannot be interpreted as a number
	 */
	double asDoubleRounded(JsonNode node);

	/**
	 * Returns the exact value of a number node.
	 * <p>
	 * Unlike {@link #asDoubleRounded(Object)}, this does not lose precision. {@link BigDecimal} cannot
	 * represent the non-finite values jq can produce, so those are reported as {@code null} rather
	 * than approximated.
	 *
	 * @param node the JSON number node
	 * @return the exact value, or {@code null} if the value is NaN, Infinity or -Infinity
	 * @throws IllegalArgumentException if the node is not a number
	 */
	@Nullable BigDecimal asBigDecimal(JsonNode node);

	/**
	 * Returns the textual value of the node.
	 * <p>
	 * String nodes return their raw (unescaped) value. The {@code null} node returns the literal
	 * string {@code "null"}, not an empty string. For array and object nodes, the returned
	 * representation is implementation-defined.
	 *
	 * @param node the JSON node
	 * @return the textual representation of the node
	 */
	String asString(JsonNode node);

	/**
	 * Returns the value of the node as a long.
	 * <p>
	 * This method has strict semantics and will throw an exception if the value
	 * cannot be represented as a long without loss of information.
	 *
	 * @param node the JSON node
	 * @return the long value
	 * @throws IllegalArgumentException if the value is NaN, Infinity, or cannot be
	 * represented as a long
	 */
	long asLong(JsonNode node);

	/**
	 * Returns the value of the node as a long after truncating its fractional part
	 * toward zero.
	 *
	 * @param node the JSON number node
	 * @return the truncated long value
	 * @throws IllegalArgumentException if the node is not a number, is NaN or
	 * Infinity, or the truncated value is outside the range of long
	 */
	long asLongTruncated(JsonNode node);

	/**
	 * Returns the value of the node as an int.
	 * <p>
	 * This method has strict semantics and will throw an exception if the value
	 * cannot be represented as an int without loss of information.
	 *
	 * @param node the JSON node
	 * @return the int value
	 * @throws IllegalArgumentException if the value is NaN, Infinity, or outside
	 * the range of int
	 */
	int asInt(JsonNode node);

	/**
	 * Returns the value of the node as an int after truncating its fractional part
	 * toward zero.
	 *
	 * @param node the JSON number node
	 * @return the truncated int value
	 * @throws IllegalArgumentException if the node is not a number, is NaN or
	 * Infinity, or the truncated value is outside the range of int
	 */
	int asIntTruncated(JsonNode node);

	/**
	 * Returns the binary value of the node.
	 * <p>
	 * Not all providers support a distinct binary node type; some may attempt to decode a string
	 * node (e.g. as base64) instead.
	 *
	 * @param node the JSON node
	 * @return the decoded bytes
	 * @throws RuntimeException if the node cannot be interpreted as binary data
	 * @throws UnsupportedOperationException if the provider does not support binary values at all
	 */
	byte[] asByteArray(JsonNode node);

	/**
	 * Returns an iterator over the field name/value pairs of an object node.
	 *
	 * @param node the JSON node
	 * @return an iterator over the object's entries, or an empty iterator if {@code node} is not an object
	 */
	Iterator<Map.Entry<String, JsonNode>> fields(JsonNode node);

	/**
	 * Returns an iterator over the child values of the node: the elements of an array, or the field
	 * values of an object (in the object's iteration order, discarding the field names).
	 *
	 * @param node the JSON node
	 * @return an iterator over the node's children, or an empty iterator if {@code node} is neither an array nor an object
	 */
	Iterator<JsonNode> elements(JsonNode node);

	/**
	 * Returns an iterator over the field names of an object node.
	 *
	 * @param node the JSON node
	 * @return an iterator over the object's field names, or an empty iterator if {@code node} is not an object
	 */
	Iterator<String> fieldNames(JsonNode node);

	/**
	 * Returns the value of the given field.
	 *
	 * @param node the JSON node
	 * @param fieldName the field name
	 * @return the field's value, or {@code null} if {@code node} is not an object or has no such field
	 */
	@Nullable JsonNode get(JsonNode node, String fieldName);

	/**
	 * Returns the element at the given index.
	 *
	 * @param node the JSON node
	 * @param index the element index
	 * @return the element at {@code index}, or {@code null} if {@code node} is not an array or {@code index} is out of range
	 */
	@Nullable JsonNode get(JsonNode node, int index);

	/**
	 * Like {@link #get(Object, String)}, but requires the field to be present.
	 *
	 * @param node the JSON node
	 * @param fieldName the field name
	 * @return the field's value
	 * @throws NullPointerException if {@code node} is not an object or has no such field
	 */
	default JsonNode requireGet(JsonNode node, String fieldName) {
		return Objects.requireNonNull(get(node, fieldName));
	}

	/**
	 * Like {@link #get(Object, int)}, but requires the index to be present.
	 *
	 * @param node the JSON node
	 * @param index the element index
	 * @return the element at {@code index}
	 * @throws NullPointerException if {@code node} is not an array or {@code index} is out of range
	 */
	default JsonNode requireGet(JsonNode node, int index) {
		return Objects.requireNonNull(get(node, index));
	}

	/**
	 * Returns the number of elements in an array, or the number of fields in an object.
	 *
	 * @param node the JSON node
	 * @return the element/field count, or {@code 0} if {@code node} is neither an array nor an object
	 */
	int size(JsonNode node);

	/**
	 * Returns whether the object node has the given field.
	 *
	 * @param node the JSON node
	 * @param fieldName the field name
	 * @return {@code true} if {@code node} is an object and has a field named {@code fieldName}
	 */
	boolean has(JsonNode node, String fieldName);

	/**
	 * Returns whether the given index is within range of the array node.
	 *
	 * @param node the JSON node
	 * @param index the element index
	 * @return {@code true} if {@code node} is an array and {@code index} is within its bounds
	 */
	boolean has(JsonNode node, int index);

	/**
	 * Returns a deep copy of the node, safe to mutate without affecting the original.
	 * <p>
	 * For providers whose native node type is already immutable, this may return the same instance.
	 *
	 * @param node the JSON node
	 * @return a deep copy of {@code node}
	 */
	JsonNode deepCopy(JsonNode node);

	/**
	 * Serializes the node to a JSON string, following jq's serialization semantics rather than the
	 * underlying JSON library's default: non-finite doubles are substituted with a finite value
	 * ({@code NaN} becomes {@code null}; {@code Infinity}/{@code -Infinity} become the maximum/minimum
	 * finite double), and HTML-significant characters ({@code <}, {@code >}, {@code &}, {@code '})
	 * are not Unicode-escaped.
	 *
	 * @param node the JSON node
	 * @return the JSON text representation of {@code node}
	 */
	String format(JsonNode node);

	/**
	 * Creates a parser reading a sequence of JSON values from the given UTF-8 stream.
	 * <p>
	 * The stream is read lazily as values are pulled, and is closed when the parser is closed.
	 *
	 * @param in the stream to read from
	 * @return a parser over {@code in}
	 */
	JsonParser<JsonNode> createParser(InputStream in);

	/**
	 * Parses multiple JSON documents from a string.
	 * Used for loading configuration files containing multiple JSON values.
	 *
	 * @param json the JSON text to parse
	 * @return the parsed JSON nodes, in document order
	 * @throws JsonException if any document fails to parse
	 */
	default List<JsonNode> parseAll(String json) {
		List<JsonNode> result = new ArrayList<>();
		try (JsonParser<JsonNode> parser = createParser(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))) {
			for (@Var JsonNode value = parser.next(); value != null; value = parser.next())
				result.add(value);
		}
		return result;
	}

	/**
	 * Parses a JSON string strictly: the string must contain exactly one JSON value, with no
	 * leading/trailing garbage other than whitespace, and must not be empty or whitespace-only.
	 *
	 * @param json the JSON string to parse
	 * @return the parsed JSON node
	 * @throws JsonException if parsing fails, {@code json} is empty or whitespace-only, or trailing content exists
	 */
	default JsonNode parse(String json) {
		try (JsonParser<JsonNode> parser = createParser(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))) {
			JsonNode value = parser.next();
			if (value == null)
				throw new JsonException("empty input");
			if (parser.next() != null)
				throw new JsonException("trailing content");
			return value;
		}
	}

	/**
	 * Returns whether the given object is an instance of this provider's native node type.
	 * <p>
	 * Because the {@code JsonNode} type parameter is erased at runtime, code that only holds a
	 * {@code JsonProvider<JsonNode>} cannot use {@code instanceof} against {@code JsonNode} directly;
	 * this method exists as a runtime substitute.
	 *
	 * @param arg the object to check, possibly {@code null}
	 * @return {@code true} if {@code arg} is an instance of this provider's node type
	 */
	// TODO: We should instead add Class<JsonNode> getNodeClass().
	boolean isJsonNodeInstance(@Nullable Object arg);
}
