package net.thisptr.jackson.jq.v2.json.impl.jep540;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jdk.incubator.json.JsonArray;
import jdk.incubator.json.JsonBoolean;
import jdk.incubator.json.JsonNull;
import jdk.incubator.json.JsonNumber;
import jdk.incubator.json.JsonObject;
import jdk.incubator.json.JsonString;
import jdk.incubator.json.JsonValue;
import jdk.incubator.json.JsonValueException;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.Maybe;
import net.thisptr.jackson.jq.v2.json.NumberType;

/**
 * A jackson-jq JSON provider backed by JEP 540's {@code jdk.incubator.json} API.
 */
public final class Jep540JsonProvider implements JsonProvider<JsonValue> {
	private static final Set<JsonNodeType> SUPPORTED_NODE_TYPES = Collections.unmodifiableSet(EnumSet.of(
			JsonNodeType.OBJECT, JsonNodeType.ARRAY, JsonNodeType.STRING, JsonNodeType.NUMBER,
			JsonNodeType.BOOLEAN, JsonNodeType.NULL));
	private static final Set<NumberType> SUPPORTED_NUMBER_TYPES = Collections.unmodifiableSet(EnumSet.of(
			NumberType.INT,
			NumberType.LONG,
			NumberType.BIG_INTEGER,
			NumberType.BIG_DECIMAL,
			NumberType.FLOAT,
			NumberType.DOUBLE));

	private Jep540JsonProvider() {
	}

	/**
	 * Returns the shared provider.
	 *
	 * @return the shared provider
	 */
	public static Jep540JsonProvider getInstance() {
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
	public Set<JsonNodeType> getSupportedNodeTypes() {
		return SUPPORTED_NODE_TYPES;
	}

	@Override
	public Set<NumberType> getSupportedNumberTypes() {
		return SUPPORTED_NUMBER_TYPES;
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

	private static final class DefaultInstanceHolder {
		private static final Jep540JsonProvider INSTANCE = new Jep540JsonProvider();
	}

	/**
	 * Reads one top-level value at a time and delegates each complete value to JEP 540.
	 */

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
