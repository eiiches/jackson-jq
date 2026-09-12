package net.thisptr.jackson.jq.v2.json.impl.jakarta;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import com.google.errorprone.annotations.Var;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonException;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonParser;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.Maybe;
import net.thisptr.jackson.jq.v2.json.NumberType;

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
		if (!(node instanceof JsonNumber))
			throw new IllegalArgumentException("Cannot convert non-number to BigDecimal");
		// JSON-P itself cannot represent non-finite values; only our own wrapper can hold them.
		if (node instanceof FloatingPointJsonNumber && !Double.isFinite(((FloatingPointJsonNumber) node).doubleValue()))
			return null;
		return ((JsonNumber) node).bigDecimalValue();
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
		if (!(node instanceof JsonString))
			throw new IllegalArgumentException("Cannot get the string value of " + getNodeType(node));
		return ((JsonString) node).getString();
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
		if (!(node instanceof JsonNumber))
			throw new IllegalArgumentException("Cannot convert non-number to " + targetType);
		return (JsonNumber) node;
	}

	/**
	 * The value as a BigDecimal, or null for NaN and the infinities. JSON-P itself cannot represent
	 * those; only our own wrapper can hold them.
	 */
	private static @Nullable BigDecimal finiteDecimal(JsonNumber number) {
		if (number instanceof FloatingPointJsonNumber && !Double.isFinite(number.doubleValue()))
			return null;
		return number.bigDecimalValue();
	}

	@Override
	public byte[] getBinaryAsByteArray(JsonValue node) {
		// JSON-P has no binary value type, so no node is ever binary.
		throw new IllegalArgumentException("Cannot get the binary value of " + getNodeType(node));
	}

	@Override
	public Iterator<Map.Entry<String, JsonValue>> getObjectMembers(JsonValue node) {
		if (!(node instanceof JsonObject))
			throw new IllegalArgumentException("Expected an object node");
		return ((JsonObject) node).entrySet().iterator();
	}

	@Override
	public Iterator<JsonValue> getArrayElements(JsonValue node) {
		if (!(node instanceof JsonArray))
			throw new IllegalArgumentException("Expected an array node");
		return ((JsonArray) node).iterator();
	}

	@Override
	public Iterator<JsonValue> getObjectMemberValues(JsonValue node) {
		if (!(node instanceof JsonObject))
			throw new IllegalArgumentException("Expected an object node");
		return ((JsonObject) node).values().iterator();
	}

	@Override
	public Iterator<String> getObjectMemberNames(JsonValue node) {
		if (!(node instanceof JsonObject))
			throw new IllegalArgumentException("Expected an object node");
		return ((JsonObject) node).keySet().iterator();
	}

	@Override
	public Maybe<JsonValue> getObjectMember(JsonValue node, String name) {
		if (!(node instanceof JsonObject))
			throw new IllegalArgumentException("Expected an object node");
		JsonValue value = ((JsonObject) node).get(name);
		if (value == null)
			return Maybe.absent();
		return Maybe.of(value);
	}

	@Override
	public JsonValue getObjectMemberOrDefault(JsonValue node, String name, JsonValue defaultValue) {
		if (!(node instanceof JsonObject))
			throw new IllegalArgumentException("Expected an object node");
		JsonValue value = ((JsonObject) node).get(name);
		return value != null ? value : defaultValue;
	}

	@Override
	public JsonValue getArrayElement(JsonValue node, int index) {
		if (!(node instanceof JsonArray))
			throw new IllegalArgumentException("Expected an array node");
		JsonArray array = (JsonArray) node;
		if (index < 0 || index >= array.size())
			throw new IndexOutOfBoundsException("Index " + index + " out of bounds for array length " + array.size());
		return array.get(index);
	}

	@Override
	public int getArrayLength(JsonValue node) {
		if (!(node instanceof JsonArray))
			throw new IllegalArgumentException("Expected an array node");
		return ((JsonArray) node).size();
	}

	@Override
	public int getObjectMemberCount(JsonValue node) {
		if (!(node instanceof JsonObject))
			throw new IllegalArgumentException("Expected an object node");
		return ((JsonObject) node).size();
	}

	@Override
	public boolean hasObjectMember(JsonValue node, String name) {
		if (!(node instanceof JsonObject))
			throw new IllegalArgumentException("Expected an object node");
		return ((JsonObject) node).containsKey(name);
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
		return new JsonPParser(delegate, new InputStreamReader(in, StandardCharsets.UTF_8));
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
	private static final class JsonPParser implements JsonParser<JsonValue> {
		private final jakarta.json.spi.JsonProvider delegate;
		private final Reader in;
		private char[] buf = new char[1024];
		/**
		 * Start of the not-yet-parsed input within {@link #buf}.
		 */
		private int start;
		/**
		 * End of the valid input within {@link #buf}.
		 */
		private int end;
		private boolean eof;

		JsonPParser(jakarta.json.spi.JsonProvider delegate, Reader in) {
			this.delegate = delegate;
			this.in = in;
		}

		@Override
		public Maybe<JsonValue> next() {
			try {
				// Parsson reports hasNext() == true on a fresh parser without reading the input, so end
				// of input has to be detected here rather than by asking the parser.
				if (!skipWhitespace())
					return Maybe.absent();
				try (jakarta.json.stream.JsonParser parser = delegate.createParser(new Window())) {
					parser.next();
					JsonValue value = parser.getValue();
					// The offset counts the characters of this one value, which came out of buf and so
					// always fits in an int.
					start += (int) parser.getLocation().getStreamOffset();
					return Maybe.of(value);
				}
			} catch (IOException | jakarta.json.JsonException e) {
				throw new JsonException(e);
			}
		}

		/**
		 * Advances {@link #start} to the next non-whitespace character.
		 *
		 * @return {@code false} if the input is exhausted
		 */
		private boolean skipWhitespace() throws IOException {
			while (true) {
				while (start < end) {
					if (!isJsonWhitespace(buf[start]))
						return true;
					++start;
				}
				if (!fill())
					return false;
			}
		}

		/**
		 * Reads more input into {@link #buf}, compacting or growing it as needed. Characters before
		 * {@link #start} have been parsed already and may be discarded; the rest must be preserved.
		 *
		 * @return {@code false} if the input is exhausted
		 */
		private boolean fill() throws IOException {
			if (eof)
				return false;
			if (end == buf.length) {
				if (start > 0) {
					System.arraycopy(buf, start, buf, 0, end - start);
					end -= start;
					start = 0;
				} else {
					buf = Arrays.copyOf(buf, buf.length * 2);
				}
			}
			int count = in.read(buf, end, buf.length - end);
			if (count < 0) {
				eof = true;
				return false;
			}
			end += count;
			return true;
		}

		@Override
		public void close() {
			try {
				in.close();
			} catch (IOException e) {
				throw new JsonException(e);
			}
		}

		/**
		 * A view of the buffered input starting at {@link #start}, handed to one underlying parser.
		 * Closing it does not close the underlying reader, which outlives any single value.
		 */
		private final class Window extends Reader {
			private int pos = start;

			@Override
			public int read(char[] cbuf, int off, int len) throws IOException {
				if (len == 0)
					return 0;
				while (pos >= end) {
					int previousStart = start;
					if (!fill())
						return -1;
					pos -= previousStart - start; // fill() may have compacted the buffer
				}
				int count = Math.min(len, end - pos);
				System.arraycopy(buf, pos, cbuf, off, count);
				pos += count;
				return count;
			}

			@Override
			public void close() {
				// The underlying reader is shared across values and is closed by JsonPParser.close().
			}
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
		// A non-integral JsonNumber that is not one of our own wrappers holds an exact decimal.
		// Render it exactly when a double cannot represent its value, so that e.g.
		// "3.14159265358979323846264338327950288" | tonumber does not print as 3.141592653589793.
		// Values a double does represent keep the double formatting: preserving their scale as
		// well (1.50 rather than 1.5) is jq 1.7 literal preservation, which jackson-jq does not
		// implement on the parse path, and doing it here alone would print a parsed 0.0 as "0.0".
		if (!(number instanceof FloatingPointJsonNumber) && BigDecimal.valueOf(value).compareTo(number.bigDecimalValue()) != 0)
			return number.bigDecimalValue().toString();
		if (value == Math.floor(value) && Math.abs(value) < Long.MAX_VALUE)
			return Long.toString((long) value);
		@Var String text = Double.toString(value).replace('e', 'E');
		int exponentIndex = text.indexOf('E');
		if (exponentIndex >= 0 && text.charAt(exponentIndex + 1) != '-')
			text = text.substring(0, exponentIndex + 1) + "+" + text.substring(exponentIndex + 1);
		return text;
	}

	private static boolean isJsonWhitespace(char ch) {
		return ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r';
	}

	private static class DefaultInstanceHolder {
		private static final JakartaJsonProviderImpl INSTANCE = new JakartaJsonProviderImpl(jakarta.json.spi.JsonProvider.provider());
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
			return formatNumber(this);
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return this == other || (other instanceof FloatingPointJsonNumber
					&& Double.doubleToLongBits(value) == Double.doubleToLongBits(((FloatingPointJsonNumber) other).value));
		}

		@Override
		public int hashCode() {
			return Double.hashCode(value);
		}
	}
}
