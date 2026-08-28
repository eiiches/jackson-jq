package net.thisptr.jackson.jq.v2.json.impl.jakarta;

import java.io.StringReader;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.errorprone.annotations.Var;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonParser;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;

/**
 * A jackson-jq JSON provider backed by the Jakarta JSON Processing tree model.
 */
public class JakartaJsonProviderImpl implements JsonProvider<JsonValue> {
	private final jakarta.json.spi.JsonProvider delegate;

	/**
	 * Creates an adapter backed by the specified JSON-P provider.
	 *
	 * @param delegate the underlying JSON-P provider
	 */
	public JakartaJsonProviderImpl(jakarta.json.spi.JsonProvider delegate) {
		this.delegate = Objects.requireNonNull(delegate);
	}

	/**
	 * Returns a singleton adapter backed by the JSON-P provider discovered by JSON-P.
	 *
	 * @return the default adapter
	 */
	public static JakartaJsonProviderImpl getInstance() {
		return DefaultInstanceHolder.INSTANCE;
	}

	@Override
	public JsonValue createObject() {
		return delegate.createObjectBuilder().build();
	}

	@Override
	public JsonValue createArray() {
		return delegate.createArrayBuilder().build();
	}

	@Override
	public JsonValue createArray(Iterable<? extends JsonValue> values) {
		jakarta.json.JsonArrayBuilder builder = delegate.createArrayBuilder();
		for (JsonValue value : values)
			builder.add(value);
		return builder.build();
	}

	@Override
	public JsonValue createObject(Map<String, ? extends JsonValue> values) {
		jakarta.json.JsonObjectBuilder builder = delegate.createObjectBuilder();
		for (Map.Entry<String, ? extends JsonValue> entry : values.entrySet())
			builder.add(entry.getKey(), entry.getValue());
		return builder.build();
	}

	@Override
	public JsonValue createString(String value) {
		return delegate.createValue(value);
	}

	@Override
	public JsonValue createNumber(long value) {
		return delegate.createValue(value);
	}

	@Override
	public JsonValue createNumber(int value) {
		return delegate.createValue(value);
	}

	@Override
	public JsonValue createNumber(float value) {
		return createFloatingPointNumber(value);
	}

	@Override
	public JsonValue createNumber(double value) {
		return createFloatingPointNumber(value);
	}

	@Override
	public JsonValue createBoolean(boolean value) {
		return value ? JsonValue.TRUE : JsonValue.FALSE;
	}

	@Override
	public JsonValue createNull() {
		return JsonValue.NULL;
	}

	@Override
	public JsonNodeType getNodeType(JsonValue node) {
		switch (node.getValueType()) {
			case ARRAY:
				return JsonNodeType.ARRAY;
			case OBJECT:
				return JsonNodeType.OBJECT;
			case STRING:
				return JsonNodeType.STRING;
			case NUMBER:
				return JsonNodeType.NUMBER;
			case TRUE:
			case FALSE:
				return JsonNodeType.BOOLEAN;
			case NULL:
				return JsonNodeType.NULL;
			default:
				throw new IllegalStateException("Unknown JSON-P value type: " + node.getValueType());
		}
	}

	@Override
	public boolean asBoolean(JsonValue node) {
		return node.getValueType() != JsonValue.ValueType.FALSE && node.getValueType() != JsonValue.ValueType.NULL;
	}

	@Override
	public double asDouble(JsonValue node) {
		if (node instanceof JsonNumber)
			return ((JsonNumber) node).doubleValue();
		if (node instanceof JsonString) {
			try {
				return Double.parseDouble(((JsonString) node).getString());
			} catch (NumberFormatException e) {
				return Double.NaN;
			}
		}
		return Double.NaN;
	}

	@Override
	public String asText(JsonValue node) {
		if (node instanceof JsonString)
			return ((JsonString) node).getString();
		if (node.getValueType() == JsonValue.ValueType.NULL)
			return "null";
		return node instanceof JsonArray || node instanceof JsonObject ? toString(node) : node.toString();
	}

	@Override
	public long asLong(JsonValue node) {
		if (node instanceof JsonNumber) {
			JsonNumber number = (JsonNumber) node;
			checkFinite(number.doubleValue(), "long");
			BigDecimal decimal = number.bigDecimalValue();
			if (decimal.compareTo(BigDecimal.valueOf(Long.MIN_VALUE)) < 0 || decimal.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) > 0)
				throw new IllegalArgumentException("Value " + decimal + " is outside the range of long");
			return number.longValue();
		}
		if (node instanceof JsonString) {
			try {
				return Long.parseLong(((JsonString) node).getString());
			} catch (NumberFormatException e) {
				return 0;
			}
		}
		return 0;
	}

	@Override
	public int asInt(JsonValue node) {
		if (node instanceof JsonNumber) {
			JsonNumber number = (JsonNumber) node;
			checkFinite(number.doubleValue(), "int");
			BigDecimal decimal = number.bigDecimalValue();
			if (decimal.compareTo(BigDecimal.valueOf(Integer.MIN_VALUE)) < 0 || decimal.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0)
				throw new IllegalArgumentException("Value " + decimal + " is outside the range of int");
			return number.intValue();
		}
		if (node instanceof JsonString) {
			try {
				return Integer.parseInt(((JsonString) node).getString());
			} catch (NumberFormatException e) {
				return 0;
			}
		}
		return 0;
	}

	@Override
	public byte[] asByteArray(JsonValue node) {
		throw new UnsupportedOperationException("Binary data is not supported by Jakarta JSON Processing");
	}

	@Override
	public Iterator<Map.Entry<String, JsonValue>> fields(JsonValue node) {
		if (node instanceof JsonObject)
			return ((JsonObject) node).entrySet().iterator();
		return Collections.emptyIterator();
	}

	@Override
	public Iterator<JsonValue> elements(JsonValue node) {
		if (node instanceof JsonArray)
			return ((JsonArray) node).iterator();
		if (node instanceof JsonObject)
			return ((JsonObject) node).values().iterator();
		return Collections.emptyIterator();
	}

	@Override
	public Iterator<String> fieldNames(JsonValue node) {
		if (node instanceof JsonObject)
			return ((JsonObject) node).keySet().iterator();
		return Collections.emptyIterator();
	}

	@Override
	public @Nullable JsonValue get(JsonValue node, String fieldName) {
		return node instanceof JsonObject ? ((JsonObject) node).get(fieldName) : null;
	}

	@Override
	public @Nullable JsonValue get(JsonValue node, int index) {
		if (node instanceof JsonArray && index >= 0 && index < ((JsonArray) node).size())
			return ((JsonArray) node).get(index);
		return null;
	}

	@Override
	public JsonValue set(JsonValue node, String fieldName, JsonValue value) {
		return delegate.createObjectBuilder(node.asJsonObject()).add(fieldName, value).build();
	}

	@Override
	public JsonValue add(JsonValue node, JsonValue value) {
		return delegate.createArrayBuilder(node.asJsonArray()).add(value).build();
	}

	@Override
	public JsonValue set(JsonValue node, int index, JsonValue value) {
		return delegate.createArrayBuilder(node.asJsonArray()).set(index, value).build();
	}

	@Override
	public int size(JsonValue node) {
		if (node instanceof JsonArray)
			return ((JsonArray) node).size();
		if (node instanceof JsonObject)
			return ((JsonObject) node).size();
		return 0;
	}

	@Override
	public boolean has(JsonValue node, String fieldName) {
		return node instanceof JsonObject && ((JsonObject) node).containsKey(fieldName);
	}

	@Override
	public boolean has(JsonValue node, int index) {
		return node instanceof JsonArray && index >= 0 && index < ((JsonArray) node).size();
	}

	@Override
	public JsonValue deepCopy(JsonValue node) {
		return node;
	}

	@Override
	public String toString(JsonValue node) {
		StringBuilder result = new StringBuilder();
		appendJson(result, node);
		return result.toString();
	}

	@Override
	public JsonValue fromString(String json) {
		return parseValue(json);
	}

	@Override
	public JsonValue fromStringStrict(String json) {
		List<String> values = splitValues(json);
		if (values.isEmpty())
			throw new IllegalArgumentException("empty input");
		if (values.size() != 1)
			throw new IllegalArgumentException("trailing content");
		return parseValue(values.get(0));
	}

	@Override
	public List<JsonValue> readMultipleValues(String json) {
		List<JsonValue> result = new ArrayList<>();
		for (String value : splitValues(json))
			result.add(parseValue(value));
		return result;
	}

	@Override
	public JsonValue valueToTree(@Nullable Object value) {
		if (value == null)
			return createNull();
		if (value instanceof JsonValue)
			return (JsonValue) value;
		if (value instanceof String || value instanceof Character || value instanceof Enum)
			return createString(value.toString());
		if (value instanceof Boolean)
			return createBoolean((Boolean) value);
		if (value instanceof Byte || value instanceof Short || value instanceof Integer)
			return createNumber(((Number) value).intValue());
		if (value instanceof Long)
			return createNumber(((Long) value).longValue());
		if (value instanceof Float)
			return createNumber(((Float) value).floatValue());
		if (value instanceof Double)
			return createNumber(((Double) value).doubleValue());
		if (value instanceof Number)
			return delegate.createValue((Number) value);
		if (value instanceof Map)
			return mapToTree((Map<?, ?>) value);
		if (value instanceof Iterable)
			return iterableToTree((Iterable<?>) value);
		if (value.getClass().isArray())
			return arrayToTree(value);
		throw new IllegalArgumentException("Cannot convert " + value.getClass().getName() + " to a JSON-P value");
	}

	@Override
	public boolean isJsonNodeInstance(@Nullable Object arg) {
		return arg instanceof JsonValue;
	}

	private JsonValue mapToTree(Map<?, ?> map) {
		jakarta.json.JsonObjectBuilder builder = delegate.createObjectBuilder();
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			if (!(entry.getKey() instanceof String))
				throw new IllegalArgumentException("JSON object keys must be strings");
			builder.add((String) entry.getKey(), valueToTree(entry.getValue()));
		}
		return builder.build();
	}

	private JsonValue iterableToTree(Iterable<?> iterable) {
		jakarta.json.JsonArrayBuilder builder = delegate.createArrayBuilder();
		for (Object value : iterable)
			builder.add(valueToTree(value));
		return builder.build();
	}

	private JsonValue arrayToTree(Object array) {
		jakarta.json.JsonArrayBuilder builder = delegate.createArrayBuilder();
		for (int i = 0; i < Array.getLength(array); ++i)
			builder.add(valueToTree(Array.get(array, i)));
		return builder.build();
	}

	private JsonValue createFloatingPointNumber(double value) {
		return Double.isFinite(value) ? delegate.createValue(value) : new NonFiniteJsonNumber(value);
	}

	private JsonValue parseValue(String json) {
		try (JsonParser parser = delegate.createParser(new StringReader(json))) {
			if (!parser.hasNext())
				throw new IllegalArgumentException("empty input");
			parser.next();
			JsonValue value = parser.getValue();
			if (parser.hasNext())
				throw new IllegalArgumentException("trailing content");
			return value;
		}
	}

	private void appendJson(StringBuilder result, JsonValue node) {
		switch (node.getValueType()) {
			case NULL:
				result.append("null");
				return;
			case TRUE:
				result.append("true");
				return;
			case FALSE:
				result.append("false");
				return;
			case STRING:
				result.append(delegate.createValue(((JsonString) node).getString()));
				return;
			case NUMBER:
				result.append(formatNumber((JsonNumber) node));
				return;
			case ARRAY:
				appendArray(result, (JsonArray) node);
				return;
			case OBJECT:
				appendObject(result, (JsonObject) node);
				return;
			default:
				throw new IllegalStateException("Unknown JSON-P value type: " + node.getValueType());
		}
	}

	private void appendArray(StringBuilder result, JsonArray array) {
		result.append('[');
		for (int i = 0; i < array.size(); ++i) {
			if (i != 0)
				result.append(',');
			appendJson(result, array.get(i));
		}
		result.append(']');
	}

	private void appendObject(StringBuilder result, JsonObject object) {
		result.append('{');
		@Var boolean first = true;
		for (Map.Entry<String, JsonValue> entry : object.entrySet()) {
			if (!first)
				result.append(',');
			first = false;
			result.append(delegate.createValue(entry.getKey())).append(':');
			appendJson(result, entry.getValue());
		}
		result.append('}');
	}

	private static String formatNumber(JsonNumber number) {
		double value = number.doubleValue();
		if (Double.isNaN(value))
			return "null";
		if (Double.isInfinite(value))
			return value > 0 ? "1.7976931348623157e+308" : "-1.7976931348623157e+308";
		if (number.isIntegral())
			return number.bigIntegerValue().toString();
		if (value == Math.floor(value) && Math.abs(value) < Long.MAX_VALUE)
			return Long.toString((long) value);
		@Var String text = Double.toString(value).replace('e', 'E');
		int exponentIndex = text.indexOf('E');
		if (exponentIndex >= 0 && text.charAt(exponentIndex + 1) != '-')
			text = text.substring(0, exponentIndex + 1) + "+" + text.substring(exponentIndex + 1);
		return text;
	}

	private static void checkFinite(double value, String targetType) {
		if (Double.isNaN(value))
			throw new IllegalArgumentException("Cannot convert NaN to " + targetType);
		if (Double.isInfinite(value))
			throw new IllegalArgumentException("Cannot convert Infinity to " + targetType);
	}

	private static List<String> splitValues(String json) {
		List<String> result = new ArrayList<>();
		@Var int index = 0;
		while (true) {
			while (index < json.length() && isJsonWhitespace(json.charAt(index)))
				++index;
			if (index == json.length())
				return result;
			int start = index;
			char first = json.charAt(index);
			if (first == '"') {
				index = scanString(json, index);
			} else if (first == '{' || first == '[') {
				index = scanStructure(json, index);
			} else {
				while (index < json.length() && !isJsonWhitespace(json.charAt(index)) && !isValueStart(json.charAt(index)))
					++index;
			}
			result.add(json.substring(start, index));
		}
	}

	private static int scanString(String json, int start) {
		@Var boolean escaped = false;
		for (int index = start + 1; index < json.length(); ++index) {
			char ch = json.charAt(index);
			if (escaped) {
				escaped = false;
			} else if (ch == '\\') {
				escaped = true;
			} else if (ch == '"') {
				return index + 1;
			}
		}
		throw new IllegalArgumentException("unterminated string");
	}

	private static int scanStructure(String json, int start) {
		ArrayDeque<Character> expectedClosings = new ArrayDeque<>();
		expectedClosings.push(json.charAt(start) == '{' ? '}' : ']');
		@Var boolean inString = false;
		@Var boolean escaped = false;
		for (int index = start + 1; index < json.length(); ++index) {
			char ch = json.charAt(index);
			if (inString) {
				if (escaped) {
					escaped = false;
				} else if (ch == '\\') {
					escaped = true;
				} else if (ch == '"') {
					inString = false;
				}
			} else if (ch == '"') {
				inString = true;
			} else if (ch == '{') {
				expectedClosings.push('}');
			} else if (ch == '[') {
				expectedClosings.push(']');
			} else if (ch == '}' || ch == ']') {
				if (expectedClosings.isEmpty() || expectedClosings.pop() != ch)
					throw new IllegalArgumentException("mismatched JSON delimiters");
				if (expectedClosings.isEmpty())
					return index + 1;
			}
		}
		throw new IllegalArgumentException("unterminated JSON structure");
	}

	private static boolean isJsonWhitespace(char ch) {
		return ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r';
	}

	private static boolean isValueStart(char ch) {
		return ch == '{' || ch == '[' || ch == '"';
	}

	private static class DefaultInstanceHolder {
		private static final JakartaJsonProviderImpl INSTANCE = new JakartaJsonProviderImpl(jakarta.json.spi.JsonProvider.provider());
	}

	private static class NonFiniteJsonNumber implements JsonNumber {
		private final double value;

		NonFiniteJsonNumber(double value) {
			this.value = value;
		}

		@Override
		public boolean isIntegral() {
			return false;
		}

		@Override
		public int intValue() {
			return (int) value;
		}

		@Override
		public int intValueExact() {
			return bigDecimalValue().intValueExact();
		}

		@Override
		public long longValue() {
			return (long) value;
		}

		@Override
		public long longValueExact() {
			return bigDecimalValue().longValueExact();
		}

		@Override
		public BigInteger bigIntegerValue() {
			return bigDecimalValue().toBigInteger();
		}

		@Override
		public BigInteger bigIntegerValueExact() {
			return bigDecimalValue().toBigIntegerExact();
		}

		@Override
		public double doubleValue() {
			return value;
		}

		@Override
		public BigDecimal bigDecimalValue() {
			return BigDecimal.valueOf(value);
		}

		@Override
		public ValueType getValueType() {
			return ValueType.NUMBER;
		}

		@Override
		public String toString() {
			return formatNumber(this);
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return this == other || (other instanceof NonFiniteJsonNumber
					&& Double.doubleToLongBits(value) == Double.doubleToLongBits(((NonFiniteJsonNumber) other).value));
		}

		@Override
		public int hashCode() {
			return Double.hashCode(value);
		}
	}
}
