package net.thisptr.jackson.jq.v2.json.impl.jakarta;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.Maybe;
import net.thisptr.jackson.jq.v2.json.NumberType;

/**
 * A jackson-jq JSON provider backed by the Jakarta JSON Processing tree model.
 */
public class JakartaJsonProvider implements JsonProvider<JsonValue> {
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

	private final jakarta.json.spi.JsonProvider delegate;

	/**
	 * Creates an adapter backed by the specified JSON-P provider.
	 *
	 * @param delegate the underlying JSON-P provider
	 */
	public JakartaJsonProvider(jakarta.json.spi.JsonProvider delegate) {
		this.delegate = Objects.requireNonNull(delegate);
	}

	/**
	 * Returns a singleton adapter backed by the JSON-P provider discovered by JSON-P.
	 *
	 * @return the default adapter
	 */
	public static JakartaJsonProvider getInstance() {
		return DefaultInstanceHolder.INSTANCE;
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
	public JsonValue createNumber(BigInteger value) {
		return delegate.createValue(value);
	}

	@Override
	public JsonValue createNumber(BigDecimal value) {
		return delegate.createValue(value);
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
	public JsonValue createBinary(byte[] bytes) {
		throw new UnsupportedOperationException("JSON-P has no binary value type");
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
		return switch (node.getValueType()) {
			case ARRAY -> JsonNodeType.ARRAY;
			case OBJECT -> JsonNodeType.OBJECT;
			case STRING -> JsonNodeType.STRING;
			case NUMBER -> JsonNodeType.NUMBER;
			case TRUE, FALSE -> JsonNodeType.BOOLEAN;
			case NULL -> JsonNodeType.NULL;
		};
	}

	// Reading getValueType() directly is cheaper than getNodeType(), which switches over it.
	@Override
	public boolean isObject(JsonValue node) {
		return node.getValueType() == JsonValue.ValueType.OBJECT;
	}

	@Override
	public boolean isArray(JsonValue node) {
		return node.getValueType() == JsonValue.ValueType.ARRAY;
	}

	@Override
	public boolean isString(JsonValue node) {
		return node.getValueType() == JsonValue.ValueType.STRING;
	}

	@Override
	public boolean isNumber(JsonValue node) {
		return node.getValueType() == JsonValue.ValueType.NUMBER;
	}

	@Override
	public boolean isBoolean(JsonValue node) {
		// JSON-P splits boolean into two value types.
		JsonValue.ValueType valueType = node.getValueType();
		return valueType == JsonValue.ValueType.TRUE || valueType == JsonValue.ValueType.FALSE;
	}

	@Override
	public boolean isNull(JsonValue node) {
		return node.getValueType() == JsonValue.ValueType.NULL;
	}

	@Override
	public boolean isBinary(JsonValue node) {
		// JSON-P has no binary value type.
		return false;
	}

	@Override
	public NumberType getNumberType(JsonValue node) {
		if (!(node instanceof JsonNumber))
			throw new IllegalArgumentException("Cannot get the number type of " + getNodeType(node));
		// Our own wrapper always holds a double, so a value created from a float reports as DOUBLE.
		if (node instanceof FloatingPointJsonNumber)
			return NumberType.DOUBLE;
		Number number;
		try {
			number = ((JsonNumber) node).numberValue();
		} catch (UnsupportedOperationException e) {
			// JsonNumber.numberValue() is a JSON-P 2.1 default method that throws unless the
			// implementation overrides it. Parsson does; not every JSON-P provider has to.
			return NumberType.UNKNOWN;
		}
		return numberTypeOf(number);
	}

	private static NumberType numberTypeOf(Number number) {
		// Short and Byte join Integer, mirroring Jackson's ShortNode reporting as INT.
		if (number instanceof Integer || number instanceof Short || number instanceof Byte)
			return NumberType.INT;
		if (number instanceof Long)
			return NumberType.LONG;
		if (number instanceof BigInteger)
			return NumberType.BIG_INTEGER;
		if (number instanceof BigDecimal)
			return NumberType.BIG_DECIMAL;
		if (number instanceof Double)
			return NumberType.DOUBLE;
		if (number instanceof Float)
			return NumberType.FLOAT;
		return NumberType.UNKNOWN;
	}

	@Override
	public boolean getBoolean(JsonValue node) {
		JsonValue.ValueType type = node.getValueType();
		if (type != JsonValue.ValueType.TRUE && type != JsonValue.ValueType.FALSE)
			throw new IllegalArgumentException("Cannot get the boolean value of " + getNodeType(node));
		return type == JsonValue.ValueType.TRUE;
	}

	@Override
	public double getNumberAsDoubleRounded(JsonValue node) {
		return requireNumber(node, "double").doubleValue();
	}

	@Override
	public @Nullable BigDecimal getNumberAsBigDecimalExact(JsonValue node) {
		if (!(node instanceof JsonNumber jsonNumber))
			throw new IllegalArgumentException("Cannot convert non-number to BigDecimal");
		// JSON-P itself cannot represent non-finite values; only our own wrapper can hold them.
		if (jsonNumber instanceof FloatingPointJsonNumber fp && !Double.isFinite(fp.doubleValue()))
			return null;
		return jsonNumber.bigDecimalValue();
	}

	@Override
	public @Nullable BigInteger getNumberAsBigIntegerExact(JsonValue node) {
		BigDecimal value = finiteDecimal(requireNumber(node, "BigInteger"));
		if (value == null)
			return null;
		try {
			return value.toBigIntegerExact();
		} catch (ArithmeticException e) {
			return null;
		}
	}

	@Override
	public @Nullable BigInteger getNumberAsBigIntegerTruncated(JsonValue node) {
		BigDecimal value = finiteDecimal(requireNumber(node, "BigInteger"));
		return value == null ? null : value.toBigInteger();
	}

	@Override
	public String getString(JsonValue node) {
		if (!(node instanceof JsonString js))
			throw new IllegalArgumentException("Cannot get the string value of " + getNodeType(node));
		return js.getString();
	}

	@Override
	public @Nullable Long getNumberAsLongExact(JsonValue node) {
		JsonNumber number = requireNumber(node, "long");
		if (number instanceof FloatingPointJsonNumber) {
			double value = number.doubleValue();
			if (!Double.isFinite(value) || value != Math.rint(value) || value < -0x1p63 || value >= 0x1p63)
				return null;
			return (long) value;
		}
		try {
			return number.bigDecimalValue().longValueExact();
		} catch (ArithmeticException e) {
			return null;
		}
	}

	@Override
	public @Nullable Long getNumberAsLongTruncated(JsonValue node) {
		JsonNumber number = requireNumber(node, "long");
		if (number instanceof FloatingPointJsonNumber) {
			double value = number.doubleValue();
			if (!Double.isFinite(value))
				return null;
			double truncated = value < 0 ? Math.ceil(value) : Math.floor(value);
			if (truncated < -0x1p63 || truncated >= 0x1p63)
				return null;
			return (long) truncated;
		}
		try {
			return number.bigDecimalValue().setScale(0, RoundingMode.DOWN).longValueExact();
		} catch (ArithmeticException e) {
			return null;
		}
	}

	@Override
	public @Nullable Integer getNumberAsIntExact(JsonValue node) {
		BigDecimal value = finiteDecimal(requireNumber(node, "int"));
		if (value == null)
			return null;
		try {
			return value.intValueExact();
		} catch (ArithmeticException e) {
			return null;
		}
	}

	@Override
	public @Nullable Integer getNumberAsIntTruncated(JsonValue node) {
		BigDecimal value = finiteDecimal(requireNumber(node, "int"));
		if (value == null)
			return null;
		try {
			return value.setScale(0, RoundingMode.DOWN).intValueExact();
		} catch (ArithmeticException e) {
			return null;
		}
	}

	private static JsonNumber requireNumber(JsonValue node, String targetType) {
		if (!(node instanceof JsonNumber jn))
			throw new IllegalArgumentException("Cannot convert non-number to " + targetType);
		return jn;
	}

	/**
	 * The value as a BigDecimal, or null for NaN and the infinities. JSON-P itself cannot represent
	 * those; only our own wrapper can hold them.
	 */
	private static @Nullable BigDecimal finiteDecimal(JsonNumber number) {
		if (number instanceof FloatingPointJsonNumber fp && !Double.isFinite(fp.doubleValue()))
			return null;
		return number.bigDecimalValue();
	}

	@Override
	public byte[] getBinaryAsByteArray(JsonValue node) {
		// JSON-P has no binary value type, so no node is ever binary.
		throw new IllegalArgumentException("Cannot get the binary value of " + getNodeType(node));
	}

	private static JsonObject requireObject(JsonValue node) {
		if (!(node instanceof JsonObject obj))
			throw new IllegalArgumentException("Expected an object node");
		return obj;
	}

	private static JsonArray requireArray(JsonValue node) {
		if (!(node instanceof JsonArray arr))
			throw new IllegalArgumentException("Expected an array node");
		return arr;
	}

	@Override
	public Iterator<Map.Entry<String, JsonValue>> getObjectMembers(JsonValue node) {
		return requireObject(node).entrySet().iterator();
	}

	@Override
	public Iterator<JsonValue> getArrayElements(JsonValue node) {
		return requireArray(node).iterator();
	}

	@Override
	public Iterator<JsonValue> getObjectMemberValues(JsonValue node) {
		return requireObject(node).values().iterator();
	}

	@Override
	public Iterator<String> getObjectMemberNames(JsonValue node) {
		return requireObject(node).keySet().iterator();
	}

	@Override
	public Maybe<JsonValue> getObjectMember(JsonValue node, String name) {
		JsonValue value = requireObject(node).get(name);
		if (value == null)
			return Maybe.absent();
		return Maybe.of(value);
	}

	@Override
	public JsonValue getObjectMemberOrDefault(JsonValue node, String name, JsonValue defaultValue) {
		JsonValue value = requireObject(node).get(name);
		return value != null ? value : defaultValue;
	}

	@Override
	public JsonValue getArrayElement(JsonValue node, int index) {
		JsonArray array = requireArray(node);
		if (index < 0 || index >= array.size())
			throw new IndexOutOfBoundsException("Index " + index + " out of bounds for array length " + array.size());
		return array.get(index);
	}

	@Override
	public int getArrayLength(JsonValue node) {
		return requireArray(node).size();
	}

	@Override
	public int getObjectMemberCount(JsonValue node) {
		return requireObject(node).size();
	}

	@Override
	public boolean hasObjectMember(JsonValue node, String name) {
		return requireObject(node).containsKey(name);
	}

	@Override
	public JsonValue deepCopy(JsonValue node) {
		return node;
	}

	private JsonValue createFloatingPointNumber(double value) {
		return new FloatingPointJsonNumber(value);
	}

	/**
	 * Reads a sequence of top-level JSON values from a reader.
	 * <p>
	 * {@code jakarta.json.stream.JsonParser} is documented to read a sequence of values that are not
	 * enclosed in an array, but no implementation actually does: both Parsson 1.1.9 and Johnzon 2.2.0
	 * reject the second value with "EOF expected" (see
	 * <a href="https://github.com/eclipse-ee4j/parsson/issues/127">parsson#127</a>). So instead of
	 * reading every value from one parser, each value gets its own parser, positioned at the end of
	 * the previous one: {@code JsonLocation#getStreamOffset()} reports exactly how many characters a
	 * value consumed, which is where the next value begins. That holds for both implementations.
	 * <p>
	 * Input is buffered only as far as the current value extends, so arbitrarily long sequences stream
	 * with memory proportional to the largest single value rather than to the whole input.
	 */

	private static class DefaultInstanceHolder {
		private static final JakartaJsonProvider INSTANCE = new JakartaJsonProvider(jakarta.json.spi.JsonProvider.provider());
	}

	private static class FloatingPointJsonNumber implements JsonNumber {
		private final double value;

		FloatingPointJsonNumber(double value) {
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
			return JsonValue.ValueType.NUMBER;
		}

		@Override
		public String toString() {
			return Double.toString(value);
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return this == other || (other instanceof FloatingPointJsonNumber fp
					&& Double.doubleToLongBits(value) == Double.doubleToLongBits(fp.value));
		}

		@Override
		public int hashCode() {
			return Double.hashCode(value);
		}
	}
}
