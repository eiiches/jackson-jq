package net.thisptr.jackson.jq.v2.json.impl.jep540;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.errorprone.annotations.Var;
import jdk.incubator.json.Json;
import jdk.incubator.json.JsonArray;
import jdk.incubator.json.JsonBoolean;
import jdk.incubator.json.JsonNull;
import jdk.incubator.json.JsonNumber;
import jdk.incubator.json.JsonObject;
import jdk.incubator.json.JsonParseException;
import jdk.incubator.json.JsonString;
import jdk.incubator.json.JsonValue;
import jdk.incubator.json.JsonValueException;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonException;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonParser;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.Maybe;
import net.thisptr.jackson.jq.v2.json.NumberType;

/**
 * A jackson-jq JSON provider backed by JEP 540's {@code jdk.incubator.json} API.
 */
public final class Jep540JsonProviderImpl implements JsonProvider<JsonValue> {
	private Jep540JsonProviderImpl() {
	}

	/**
	 * Returns the shared provider.
	 *
	 * @return the shared provider
	 */
	public static Jep540JsonProviderImpl getInstance() {
		return DefaultInstanceHolder.INSTANCE;
	}

	@Override
	public JsonValue createArray(Iterable<? extends JsonValue> values) {
		List<JsonValue> result = new ArrayList<>();
		for (JsonValue value : values)
			result.add(value);
		return JsonArray.of(result);
	}

	@Override
	public JsonValue createObject(Map<String, ? extends JsonValue> values) {
		return JsonObject.of(new LinkedHashMap<>(values));
	}

	@Override
	public JsonValue createString(String value) {
		return JsonString.of(value);
	}

	@Override
	public JsonValue createNumber(long value) {
		return JsonNumber.of(value);
	}

	@Override
	public JsonValue createNumber(int value) {
		return JsonNumber.of(value);
	}

	@Override
	public JsonValue createNumber(float value) {
		return new FloatingPointJsonNumber(value, Float.toString(value), NumberType.FLOAT);
	}

	@Override
	public JsonValue createNumber(double value) {
		return new FloatingPointJsonNumber(value, Double.toString(value), NumberType.DOUBLE);
	}

	@Override
	public JsonValue createNumber(BigInteger value) {
		return JsonNumber.of(value.toString());
	}

	@Override
	public JsonValue createNumber(BigDecimal value) {
		return JsonNumber.of(value.toString());
	}

	@Override
	public JsonValue createBoolean(boolean value) {
		return JsonBoolean.of(value);
	}

	@Override
	public JsonValue createNull() {
		return JsonNull.of();
	}

	@Override
	public JsonValue createBinary(byte[] bytes) {
		throw new UnsupportedOperationException("JEP 540 has no binary node type");
	}

	@Override
	public JsonNodeType getNodeType(JsonValue node) {
		return switch (node) {
			case JsonObject _ -> JsonNodeType.OBJECT;
			case JsonArray _ -> JsonNodeType.ARRAY;
			case JsonString _ -> JsonNodeType.STRING;
			case JsonNumber _ -> JsonNodeType.NUMBER;
			case JsonBoolean _ -> JsonNodeType.BOOLEAN;
			case JsonNull _ -> JsonNodeType.NULL;
		};
	}

	@Override
	public boolean isObject(JsonValue node) {
		return node instanceof JsonObject;
	}

	@Override
	public boolean isArray(JsonValue node) {
		return node instanceof JsonArray;
	}

	@Override
	public boolean isString(JsonValue node) {
		return node instanceof JsonString;
	}

	@Override
	public boolean isNumber(JsonValue node) {
		return node instanceof JsonNumber;
	}

	@Override
	public boolean isBoolean(JsonValue node) {
		return node instanceof JsonBoolean;
	}

	@Override
	public boolean isNull(JsonValue node) {
		return node instanceof JsonNull;
	}

	@Override
	public boolean isBinary(JsonValue node) {
		return false;
	}

	@Override
	public NumberType getNumberType(JsonValue node) {
		JsonNumber number = requireNumber(node);
		return number instanceof FloatingPointJsonNumber floatingPoint ? floatingPoint.type : NumberType.UNKNOWN;
	}

	@Override
	public boolean getBoolean(JsonValue node) {
		if (node instanceof JsonBoolean value)
			return value.asBoolean();
		throw wrongType(node, "boolean");
	}

	@Override
	public double getNumberAsDoubleRounded(JsonValue node) {
		JsonNumber number = requireNumber(node);
		if (number instanceof FloatingPointJsonNumber floatingPoint)
			return floatingPoint.value;
		return Double.parseDouble(number.toString());
	}

	@Override
	public @Nullable BigDecimal getNumberAsBigDecimalExact(JsonValue node) {
		JsonNumber number = requireNumber(node);
		if (number instanceof FloatingPointJsonNumber floatingPoint && !Double.isFinite(floatingPoint.value))
			return null;
		return new BigDecimal(number.toString());
	}

	@Override
	public @Nullable BigInteger getNumberAsBigIntegerExact(JsonValue node) {
		BigDecimal value = getNumberAsBigDecimalExact(node);
		if (value == null)
			return null;
		try {
			return value.toBigIntegerExact();
		} catch (ArithmeticException _) {
			return null;
		}
	}

	@Override
	public @Nullable BigInteger getNumberAsBigIntegerTruncated(JsonValue node) {
		BigDecimal value = getNumberAsBigDecimalExact(node);
		return value == null ? null : value.toBigInteger();
	}

	@Override
	public String getString(JsonValue node) {
		if (node instanceof JsonString value)
			return value.asString();
		throw wrongType(node, "string");
	}

	@Override
	public @Nullable Long getNumberAsLongExact(JsonValue node) {
		JsonNumber number = requireNumber(node);
		if (number instanceof FloatingPointJsonNumber floatingPoint)
			return exactFloatingPointLong(floatingPoint.value);
		BigDecimal value = getNumberAsBigDecimalExact(node);
		if (value == null)
			return null;
		try {
			return value.longValueExact();
		} catch (ArithmeticException _) {
			return null;
		}
	}

	@Override
	public @Nullable Long getNumberAsLongTruncated(JsonValue node) {
		JsonNumber number = requireNumber(node);
		if (number instanceof FloatingPointJsonNumber floatingPoint)
			return truncatedFloatingPointLong(floatingPoint.value);
		BigInteger value = getNumberAsBigIntegerTruncated(node);
		if (value == null)
			return null;
		try {
			return value.longValueExact();
		} catch (ArithmeticException _) {
			return null;
		}
	}

	@Override
	public @Nullable Integer getNumberAsIntExact(JsonValue node) {
		JsonNumber number = requireNumber(node);
		if (number instanceof FloatingPointJsonNumber floatingPoint) {
			Long value = exactFloatingPointLong(floatingPoint.value);
			return value == null || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE ? null : value.intValue();
		}
		BigDecimal value = getNumberAsBigDecimalExact(node);
		if (value == null)
			return null;
		try {
			return value.intValueExact();
		} catch (ArithmeticException _) {
			return null;
		}
	}

	@Override
	public @Nullable Integer getNumberAsIntTruncated(JsonValue node) {
		JsonNumber number = requireNumber(node);
		if (number instanceof FloatingPointJsonNumber floatingPoint) {
			Long value = truncatedFloatingPointLong(floatingPoint.value);
			return value == null || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE ? null : value.intValue();
		}
		BigInteger value = getNumberAsBigIntegerTruncated(node);
		if (value == null)
			return null;
		try {
			return value.intValueExact();
		} catch (ArithmeticException _) {
			return null;
		}
	}

	@Override
	public byte[] getBinaryAsByteArray(JsonValue node) {
		throw wrongType(node, "binary");
	}

	@Override
	public Iterator<Map.Entry<String, JsonValue>> getObjectMembers(JsonValue node) {
		return requireObject(node).asMap().entrySet().iterator();
	}

	@Override
	public Iterator<JsonValue> getArrayElements(JsonValue node) {
		return requireArray(node).asList().iterator();
	}

	@Override
	public Iterator<JsonValue> getObjectMemberValues(JsonValue node) {
		return requireObject(node).asMap().values().iterator();
	}

	@Override
	public Iterator<String> getObjectMemberNames(JsonValue node) {
		return requireObject(node).asMap().keySet().iterator();
	}

	@Override
	public Maybe<JsonValue> getObjectMember(JsonValue node, String name) {
		Map<String, JsonValue> values = requireObject(node).asMap();
		return values.containsKey(name) ? Maybe.of(values.get(name)) : Maybe.absent();
	}

	@Override
	public JsonValue getObjectMemberOrDefault(JsonValue node, String name, JsonValue defaultValue) {
		return requireObject(node).asMap().getOrDefault(name, defaultValue);
	}

	@Override
	public JsonValue getArrayElement(JsonValue node, int index) {
		return requireArray(node).asList().get(index);
	}

	@Override
	public int getArrayLength(JsonValue node) {
		return requireArray(node).asList().size();
	}

	@Override
	public int getObjectMemberCount(JsonValue node) {
		return requireObject(node).asMap().size();
	}

	@Override
	public boolean hasObjectMember(JsonValue node, String name) {
		return requireObject(node).asMap().containsKey(name);
	}

	@Override
	public JsonValue deepCopy(JsonValue node) {
		return node;
	}

	@Override
	public String format(JsonValue node) {
		StringBuilder result = new StringBuilder();
		appendJson(result, node);
		return result.toString();
	}

	@Override
	public JsonParser<JsonValue> createParser(InputStream in) {
		return new Jep540Parser(new PushbackReader(new InputStreamReader(in, StandardCharsets.UTF_8), 1));
	}

	private static JsonNumber requireNumber(JsonValue node) {
		if (node instanceof JsonNumber value)
			return value;
		throw wrongType(node, "number");
	}

	private static JsonArray requireArray(JsonValue node) {
		if (node instanceof JsonArray value)
			return value;
		throw wrongType(node, "array");
	}

	private static JsonObject requireObject(JsonValue node) {
		if (node instanceof JsonObject value)
			return value;
		throw wrongType(node, "object");
	}

	private static IllegalArgumentException wrongType(JsonValue node, String expected) {
		return new IllegalArgumentException("Expected " + expected + ", got " + node.getClass().getName());
	}

	private static @Nullable Long exactFloatingPointLong(double value) {
		return value == Math.rint(value) ? truncatedFloatingPointLong(value) : null;
	}

	private static @Nullable Long truncatedFloatingPointLong(double value) {
		return Double.isFinite(value) && value >= Long.MIN_VALUE && value < 0x1p63 ? (long) value : null;
	}

	private static void appendJson(StringBuilder result, JsonValue node) {
		switch (node) {
			case JsonNull _ -> result.append("null");
			case JsonBoolean value -> result.append(value.asBoolean());
			case JsonNumber value -> result.append(formatNumber(value));
			case JsonString value -> result.append(value);
			case JsonArray value -> appendArray(result, value);
			case JsonObject value -> appendObject(result, value);
		}
	}

	private static void appendArray(StringBuilder result, JsonArray array) {
		result.append('[');
		List<JsonValue> values = array.asList();
		for (int i = 0; i < values.size(); ++i) {
			if (i != 0)
				result.append(',');
			appendJson(result, values.get(i));
		}
		result.append(']');
	}

	private static void appendObject(StringBuilder result, JsonObject object) {
		result.append('{');
		@Var boolean first = true;
		for (Map.Entry<String, JsonValue> entry : object.asMap().entrySet()) {
			if (!first)
				result.append(',');
			first = false;
			result.append(JsonString.of(entry.getKey())).append(':');
			appendJson(result, entry.getValue());
		}
		result.append('}');
	}

	private static String formatNumber(JsonNumber number) {
		if (number instanceof FloatingPointJsonNumber floatingPoint) {
			if (Double.isNaN(floatingPoint.value))
				return "null";
			if (Double.isInfinite(floatingPoint.value))
				return floatingPoint.value > 0 ? "1.7976931348623157e+308" : "-1.7976931348623157e+308";
			return formatDouble(floatingPoint.value);
		}

		String source = number.toString();
		BigDecimal decimal = new BigDecimal(source);
		if (decimal.signum() == 0)
			return "0";
		if (source.indexOf('.') < 0 && source.indexOf('e') < 0 && source.indexOf('E') < 0)
			return decimal.toBigIntegerExact().toString();

		double value = decimal.doubleValue();
		if (!Double.isFinite(value) || BigDecimal.valueOf(value).compareTo(decimal) != 0)
			return decimal.toString();
		return formatDouble(value);
	}

	private static String formatDouble(double value) {
		if (value == 0 || (value == Math.floor(value) && Math.abs(value) < Long.MAX_VALUE))
			return Long.toString((long) value);
		@Var String text = Double.toString(value).replace('e', 'E');
		int exponentIndex = text.indexOf('E');
		if (exponentIndex >= 0 && text.charAt(exponentIndex + 1) != '-')
			text = text.substring(0, exponentIndex + 1) + "+" + text.substring(exponentIndex + 1);
		return text;
	}

	private static boolean isJsonWhitespace(int ch) {
		return ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r';
	}

	private static boolean startsStructuredValue(int ch) {
		return ch == '{' || ch == '[' || ch == '"';
	}

	private static final class DefaultInstanceHolder {
		private static final Jep540JsonProviderImpl INSTANCE = new Jep540JsonProviderImpl();
	}

	/**
	 * Reads one top-level value at a time and delegates each complete value to JEP 540.
	 */
	private static final class Jep540Parser implements JsonParser<JsonValue> {
		private final PushbackReader in;
		private boolean exhausted;

		Jep540Parser(PushbackReader in) {
			this.in = in;
		}

		@Override
		public Maybe<JsonValue> next() {
			if (exhausted)
				return Maybe.absent();
			try {
				int first = readFirst();
				if (first < 0) {
					exhausted = true;
					return Maybe.absent();
				}
				String value = readValue(first);
				try {
					return Maybe.of(Json.parse(value));
				} catch (JsonParseException | IllegalArgumentException e) {
					throw new JsonException(e);
				}
			} catch (IOException e) {
				throw new JsonException(e);
			}
		}

		private int readFirst() throws IOException {
			for (@Var int ch = in.read(); ch >= 0; ch = in.read()) {
				if (!isJsonWhitespace(ch))
					return ch;
			}
			return -1;
		}

		private String readValue(int first) throws IOException {
			StringBuilder result = new StringBuilder().append((char) first);
			if (first == '"')
				readString(result);
			else if (first == '{' || first == '[')
				readStructure(result, first);
			else
				readPrimitive(result);
			return result.toString();
		}

		private void readString(StringBuilder result) throws IOException {
			@Var boolean escaped = false;
			for (@Var int ch = in.read(); ch >= 0; ch = in.read()) {
				result.append((char) ch);
				if (escaped) {
					escaped = false;
				} else if (ch == '\\') {
					escaped = true;
				} else if (ch == '"') {
					return;
				}
			}
		}

		private void readStructure(StringBuilder result, int first) throws IOException {
			ArrayDeque<Character> expected = new ArrayDeque<>();
			expected.push(first == '{' ? '}' : ']');
			@Var boolean inString = false;
			@Var boolean escaped = false;
			for (@Var int ch = in.read(); ch >= 0; ch = in.read()) {
				result.append((char) ch);
				if (inString) {
					if (escaped)
						escaped = false;
					else if (ch == '\\')
						escaped = true;
					else if (ch == '"')
						inString = false;
				} else if (ch == '"') {
					inString = true;
				} else if (ch == '{') {
					expected.push('}');
				} else if (ch == '[') {
					expected.push(']');
				} else if (ch == '}' || ch == ']') {
					if (expected.isEmpty() || expected.pop() != ch)
						throw new JsonException("Mismatched JSON delimiters");
					if (expected.isEmpty())
						return;
				}
			}
		}

		private void readPrimitive(StringBuilder result) throws IOException {
			for (@Var int ch = in.read(); ch >= 0; ch = in.read()) {
				if (isJsonWhitespace(ch))
					return;
				if (startsStructuredValue(ch)) {
					in.unread(ch);
					return;
				}
				result.append((char) ch);
			}
		}

		@Override
		public void close() {
			try {
				in.close();
			} catch (IOException e) {
				throw new JsonException(e);
			}
		}
	}

	private static final class FloatingPointJsonNumber implements JsonNumber {
		private final double value;
		private final String text;
		private final NumberType type;

		FloatingPointJsonNumber(double value, String text, NumberType type) {
			this.value = value;
			this.text = text;
			this.type = type;
		}

		@Override
		public int asInt() {
			throw new JsonValueException("Cannot convert " + value + " to int");
		}

		@Override
		public long asLong() {
			throw new JsonValueException("Cannot convert " + value + " to long");
		}

		@Override
		public double asDouble() {
			return value;
		}

		@Override
		public String toString() {
			return text;
		}
	}
}
