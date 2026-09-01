package net.thisptr.jackson.jq.v2.json.impl.gson;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

import com.google.errorprone.annotations.Var;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonStreamParser;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonException;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonParser;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.NumberType;

public class GsonJsonProviderImpl implements JsonProvider<JsonElement> {
	private static final GsonJsonProviderImpl DEFAULT_INSTANCE = new GsonJsonProviderImpl(GsonUtils.createJqCompatibleGson());

	private final Gson gson;

	public GsonJsonProviderImpl(Gson gson) {
		this.gson = gson;
	}

	/**
	 * Returns a singleton instance using a default Gson.
	 */
	public static GsonJsonProviderImpl getInstance() {
		return DEFAULT_INSTANCE;
	}

	@Override
	public JsonElement createObject(Map<String, ? extends JsonElement> values) {
		JsonObject result = new JsonObject();
		for (Map.Entry<String, ? extends JsonElement> entry : values.entrySet()) {
			result.add(entry.getKey(), entry.getValue());
		}
		return result;
	}

	@Override
	public JsonElement createArray(Iterable<? extends JsonElement> values) {
		JsonArray result = new JsonArray();
		for (JsonElement value : values) {
			result.add(value);
		}
		return result;
	}

	@Override
	public JsonElement createString(String value) {
		return new JsonPrimitive(value);
	}

	@Override
	public JsonElement createNumber(long value) {
		return new JsonPrimitive(value);
	}

	@Override
	public JsonElement createNumber(int value) {
		return new JsonPrimitive(value);
	}

	@Override
	public JsonElement createNumber(float value) {
		return new JsonPrimitive(value);
	}

	@Override
	public JsonElement createNumber(double value) {
		return new JsonPrimitive(value);
	}

	@Override
	public JsonElement createNumber(BigInteger value) {
		return new JsonPrimitive(value);
	}

	@Override
	public JsonElement createNumber(BigDecimal value) {
		return new JsonPrimitive(value);
	}

	@Override
	public JsonElement createBoolean(boolean value) {
		return new JsonPrimitive(value);
	}

	@Override
	public JsonElement createNull() {
		return JsonNull.INSTANCE;
	}

	@Override
	public JsonNodeType getNodeType(JsonElement node) {
		if (node.isJsonNull()) {
			return JsonNodeType.NULL;
		}
		if (node.isJsonObject()) {
			return JsonNodeType.OBJECT;
		}
		if (node.isJsonArray()) {
			return JsonNodeType.ARRAY;
		}
		if (node.isJsonPrimitive()) {
			JsonPrimitive primitive = node.getAsJsonPrimitive();
			if (primitive.isBoolean()) {
				return JsonNodeType.BOOLEAN;
			}
			if (primitive.isNumber()) {
				return JsonNodeType.NUMBER;
			}
			if (primitive.isString()) {
				return JsonNodeType.STRING;
			}
		}
		throw new IllegalStateException("Unknown JsonElement type: " + node.getClass());
	}

	// JsonElement's own predicates are cheaper than getNodeType(), which walks a chain of them.
	// String, number and boolean have no JsonElement-level predicate, so they go through
	// JsonPrimitive; getAsJsonPrimitive() throws on other nodes, hence the instanceof guard.
	@Override
	public boolean isObject(JsonElement node) {
		return node.isJsonObject();
	}

	@Override
	public boolean isArray(JsonElement node) {
		return node.isJsonArray();
	}

	@Override
	public boolean isString(JsonElement node) {
		return node instanceof JsonPrimitive && ((JsonPrimitive) node).isString();
	}

	@Override
	public boolean isNumber(JsonElement node) {
		return node instanceof JsonPrimitive && ((JsonPrimitive) node).isNumber();
	}

	@Override
	public boolean isBoolean(JsonElement node) {
		return node instanceof JsonPrimitive && ((JsonPrimitive) node).isBoolean();
	}

	@Override
	public boolean isNull(JsonElement node) {
		return node.isJsonNull();
	}

	@Override
	public NumberType getNumberType(JsonElement node) {
		if (!node.isJsonPrimitive() || !node.getAsJsonPrimitive().isNumber())
			throw new IllegalArgumentException("Cannot get the number type of " + getNodeType(node));
		return numberTypeOf(node.getAsJsonPrimitive().getAsNumber());
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
		// Numbers straight from the parser are Gson's LazilyParsedNumber, which keeps the literal as
		// text and so commits to no representation at all.
		return NumberType.UNKNOWN;
	}

	@Override
	public boolean getBoolean(JsonElement node) {
		if (!node.isJsonPrimitive() || !node.getAsJsonPrimitive().isBoolean())
			throw new IllegalArgumentException("Cannot get the boolean value of " + getNodeType(node));
		return node.getAsJsonPrimitive().getAsBoolean();
	}

	@Override
	public double getNumberAsDoubleRounded(JsonElement node) {
		return requireNumber(node, "double").getAsDouble();
	}

	@Override
	public @Nullable BigDecimal getNumberAsBigDecimalExact(JsonElement node) {
		JsonPrimitive primitive = requireNumber(node, "BigDecimal");
		// Gson goes through Number.toString(), i.e. the shortest round-trip representation for
		// Double/Float (e.g. 0.1 stays 0.1) and the original literal for parsed numbers.
		return finiteDecimal(primitive);
	}

	@Override
	public @Nullable BigInteger getNumberAsBigIntegerExact(JsonElement node) {
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
	public @Nullable BigInteger getNumberAsBigIntegerTruncated(JsonElement node) {
		BigDecimal value = finiteDecimal(requireNumber(node, "BigInteger"));
		return value == null ? null : value.toBigInteger();
	}

	@Override
	public String getString(JsonElement node) {
		if (!node.isJsonPrimitive() || !node.getAsJsonPrimitive().isString())
			throw new IllegalArgumentException("Cannot get the string value of " + getNodeType(node));
		return node.getAsJsonPrimitive().getAsString();
	}

	@Override
	public @Nullable Long getNumberAsLongExact(JsonElement node) {
		JsonPrimitive primitive = requireNumber(node, "long");
		Number number = primitive.getAsNumber();
		if (number instanceof Double || number instanceof Float) {
			double value = number.doubleValue();
			if (!Double.isFinite(value) || value != Math.rint(value) || value < -0x1p63 || value >= 0x1p63)
				return null;
			return (long) value;
		}
		BigDecimal value = finiteDecimal(primitive);
		if (value == null)
			return null;
		try {
			return value.longValueExact();
		} catch (ArithmeticException e) {
			return null;
		}
	}

	@Override
	public @Nullable Long getNumberAsLongTruncated(JsonElement node) {
		JsonPrimitive primitive = requireNumber(node, "long");
		Number number = primitive.getAsNumber();
		if (number instanceof Double || number instanceof Float) {
			double value = number.doubleValue();
			if (!Double.isFinite(value))
				return null;
			double truncated = value < 0 ? Math.ceil(value) : Math.floor(value);
			if (truncated < -0x1p63 || truncated >= 0x1p63)
				return null;
			return (long) truncated;
		}
		BigDecimal value = finiteDecimal(primitive);
		if (value == null)
			return null;
		try {
			return value.setScale(0, RoundingMode.DOWN).longValueExact();
		} catch (ArithmeticException e) {
			return null;
		}
	}

	@Override
	public @Nullable Integer getNumberAsIntExact(JsonElement node) {
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
	public @Nullable Integer getNumberAsIntTruncated(JsonElement node) {
		BigDecimal value = finiteDecimal(requireNumber(node, "int"));
		if (value == null)
			return null;
		try {
			return value.setScale(0, RoundingMode.DOWN).intValueExact();
		} catch (ArithmeticException e) {
			return null;
		}
	}

	private static JsonPrimitive requireNumber(JsonElement node, String targetType) {
		if (!node.isJsonPrimitive() || !node.getAsJsonPrimitive().isNumber())
			throw new IllegalArgumentException("Cannot convert non-number to " + targetType);
		return node.getAsJsonPrimitive();
	}

	/**
	 * The value as a BigDecimal, or null for NaN and the infinities, which BigDecimal cannot hold.
	 */
	private static @Nullable BigDecimal finiteDecimal(JsonPrimitive primitive) {
		Number number = primitive.getAsNumber();
		if ((number instanceof Double || number instanceof Float) && !Double.isFinite(number.doubleValue()))
			return null;
		return primitive.getAsBigDecimal();
	}

	@Override
	public byte[] getBinaryAsByteArray(JsonElement node) {
		throw new UnsupportedOperationException("Binary data is not supported by Gson provider");
	}

	@Override
	public Iterator<Map.Entry<String, JsonElement>> getObjectEntries(JsonElement node) {
		if (!node.isJsonObject())
			throw new IllegalArgumentException("Expected an object node");
		return node.getAsJsonObject().entrySet().iterator();
	}

	@Override
	public Iterator<JsonElement> getArrayElements(JsonElement node) {
		if (!node.isJsonArray())
			throw new IllegalArgumentException("Expected an array node");
		return node.getAsJsonArray().iterator();
	}

	@Override
	public Iterator<JsonElement> getObjectFieldValues(JsonElement node) {
		if (!node.isJsonObject())
			throw new IllegalArgumentException("Expected an object node");
		return node.getAsJsonObject().entrySet().stream()
				.map(Map.Entry::getValue)
				.iterator();
	}

	@Override
	public Iterator<String> getObjectFieldNames(JsonElement node) {
		if (!node.isJsonObject())
			throw new IllegalArgumentException("Expected an object node");
		return node.getAsJsonObject().keySet().iterator();
	}

	@Override
	public @Nullable JsonElement getObjectField(JsonElement node, String fieldName) {
		if (!node.isJsonObject())
			throw new IllegalArgumentException("Expected an object node");
		return node.getAsJsonObject().get(fieldName);
	}

	@Override
	public JsonElement getArrayElement(JsonElement node, int index) {
		if (!node.isJsonArray())
			throw new IllegalArgumentException("Expected an array node");
		JsonArray array = node.getAsJsonArray();
		if (index < 0 || index >= array.size())
			throw new IndexOutOfBoundsException("Index " + index + " out of bounds for array length " + array.size());
		return array.get(index);
	}

	@Override
	public int getArrayLength(JsonElement node) {
		if (!node.isJsonArray())
			throw new IllegalArgumentException("Expected an array node");
		return node.getAsJsonArray().size();
	}

	@Override
	public int getObjectSize(JsonElement node) {
		if (!node.isJsonObject())
			throw new IllegalArgumentException("Expected an object node");
		return node.getAsJsonObject().size();
	}

	@Override
	public boolean hasObjectField(JsonElement node, String fieldName) {
		if (!node.isJsonObject())
			throw new IllegalArgumentException("Expected an object node");
		return node.getAsJsonObject().has(fieldName);
	}

	@Override
	public JsonElement deepCopy(JsonElement node) {
		return node.deepCopy();
	}

	@Override
	public String format(JsonElement node) {
		return toJqString(node);
	}

	/**
	 * Converts a JsonElement to a jq-compatible JSON string.
	 * Handles NaN, Infinity, and number formatting.
	 */
	private String toJqString(JsonElement node) {
		if (node == null || node.isJsonNull()) {
			return "null";
		}
		if (node.isJsonPrimitive()) {
			JsonPrimitive primitive = node.getAsJsonPrimitive();
			if (primitive.isNumber()) {
				Number number = primitive.getAsNumber();
				// Exact numbers keep their exact representation, mirroring Jackson's Int/Long/
				// BigInteger/BigDecimal nodes; narrowing them to double here would print e.g.
				// 2871948651097801136 as 2871948651097801000 and 1.50 as 1.5. Only genuine
				// floating-point values go through jq's double formatting. (Numbers straight from
				// the parser are Gson's LazilyParsedNumber and take the double path, which is what
				// Jackson's parse-to-double does.)
				if (number instanceof Integer || number instanceof Long || number instanceof BigInteger || number instanceof BigDecimal)
					return number.toString();
				return GsonUtils.formatDouble(number.doubleValue());
			}
			if (primitive.isBoolean()) {
				return String.valueOf(primitive.getAsBoolean());
			}
			if (primitive.isString()) {
				// Need proper JSON string escaping
				return gson.toJson(primitive.getAsString());
			}
		}
		if (node.isJsonArray()) {
			JsonArray array = node.getAsJsonArray();
			StringBuilder sb = new StringBuilder("[");
			@Var boolean first = true;
			for (JsonElement element : array) {
				if (!first) {
					sb.append(",");
				}
				first = false;
				sb.append(toJqString(element));
			}
			sb.append("]");
			return sb.toString();
		}
		if (node.isJsonObject()) {
			JsonObject obj = node.getAsJsonObject();
			StringBuilder sb = new StringBuilder("{");
			@Var boolean first = true;
			for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
				if (!first) {
					sb.append(",");
				}
				first = false;
				sb.append(gson.toJson(entry.getKey()));
				sb.append(":");
				sb.append(toJqString(entry.getValue()));
			}
			sb.append("}");
			return sb.toString();
		}
		return gson.toJson(node);
	}

	@Override
	public JsonParser<JsonElement> createParser(InputStream in) {
		return new JsonStreamParserAdapter(in);
	}

	private static class JsonStreamParserAdapter implements JsonParser<JsonElement> {
		private final Reader reader;
		private final JsonStreamParser parser;

		JsonStreamParserAdapter(InputStream in) {
			this.reader = new InputStreamReader(in, StandardCharsets.UTF_8);
			this.parser = new JsonStreamParser(reader);
		}

		@Override
		public @Nullable JsonElement next() {
			try {
				return hasNext() ? parser.next() : null;
			} catch (JsonParseException e) {
				throw new JsonException(e);
			}
		}

		/**
		 * Reports whether another value follows. Gson signals end of input by throwing
		 * {@link EOFException} out of {@code hasNext()} rather than returning {@code false}; a value
		 * that is merely truncated throws from {@code next()} instead, so treating this one case as
		 * end of input does not swallow malformed JSON.
		 */
		private boolean hasNext() {
			try {
				return parser.hasNext();
			} catch (JsonIOException e) {
				if (e.getCause() instanceof EOFException)
					return false;
				throw new JsonException(e);
			}
		}

		@Override
		public void close() {
			try {
				reader.close();
			} catch (IOException e) {
				throw new JsonException(e);
			}
		}
	}

	@Override
	public boolean isJsonNodeInstance(@Nullable Object arg) {
		return arg instanceof JsonElement;
	}
}
