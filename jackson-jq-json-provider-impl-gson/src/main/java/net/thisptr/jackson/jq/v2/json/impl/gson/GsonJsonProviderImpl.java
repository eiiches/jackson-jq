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
import java.util.Collections;
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

	@Override
	public boolean asBoolean(JsonElement node) {
		if (node.isJsonPrimitive()) {
			JsonPrimitive primitive = node.getAsJsonPrimitive();
			if (primitive.isBoolean()) {
				return primitive.getAsBoolean();
			}
		}
		// jq semantics: null and false are falsy, everything else is truthy
		if (node.isJsonNull()) {
			return false;
		}
		return true;
	}

	@Override
	public double asDouble(JsonElement node) {
		if (node.isJsonPrimitive()) {
			JsonPrimitive primitive = node.getAsJsonPrimitive();
			if (primitive.isNumber()) {
				return primitive.getAsDouble();
			}
			if (primitive.isString()) {
				try {
					return Double.parseDouble(primitive.getAsString());
				} catch (NumberFormatException e) {
					return Double.NaN;
				}
			}
		}
		return Double.NaN;
	}

	@Override
	public @Nullable BigDecimal asBigDecimal(JsonElement node) {
		if (!node.isJsonPrimitive() || !node.getAsJsonPrimitive().isNumber())
			throw new IllegalArgumentException("Cannot convert non-number to BigDecimal");
		JsonPrimitive primitive = node.getAsJsonPrimitive();
		Number number = primitive.getAsNumber();
		if ((number instanceof Double || number instanceof Float) && !Double.isFinite(number.doubleValue()))
			return null;
		// Gson goes through Number.toString(), i.e. the shortest round-trip representation for
		// Double/Float (e.g. 0.1 stays 0.1) and the original literal for parsed numbers.
		return primitive.getAsBigDecimal();
	}

	@Override
	public String asString(JsonElement node) {
		if (node.isJsonNull()) {
			return "null";
		}
		if (node.isJsonPrimitive()) {
			return node.getAsJsonPrimitive().getAsString();
		}
		return gson.toJson(node);
	}

	@Override
	public long asLong(JsonElement node) {
		if (!node.isJsonPrimitive() || !node.getAsJsonPrimitive().isNumber())
			throw new IllegalArgumentException("Cannot convert non-number to long");
		JsonPrimitive primitive = node.getAsJsonPrimitive();
		Number number = primitive.getAsNumber();
		if (number instanceof Double || number instanceof Float) {
			double value = number.doubleValue();
			if (Double.isNaN(value))
				throw new IllegalArgumentException("Cannot convert NaN to long");
			if (Double.isInfinite(value))
				throw new IllegalArgumentException("Cannot convert Infinity to long");
			if (value != Math.rint(value) || value < -0x1p63 || value >= 0x1p63)
				throw new IllegalArgumentException("Value " + value + " cannot be represented as long");
			return (long) value;
		}
		BigDecimal value = requireFiniteNumber(node, "long");
		try {
			return value.longValueExact();
		} catch (ArithmeticException e) {
			throw cannotRepresent(value, "long", e);
		}
	}

	@Override
	public long asLongTruncated(JsonElement node) {
		if (!node.isJsonPrimitive() || !node.getAsJsonPrimitive().isNumber())
			throw new IllegalArgumentException("Cannot convert non-number to long");
		JsonPrimitive primitive = node.getAsJsonPrimitive();
		Number number = primitive.getAsNumber();
		if (number instanceof Double || number instanceof Float) {
			double value = number.doubleValue();
			if (Double.isNaN(value))
				throw new IllegalArgumentException("Cannot convert NaN to long");
			if (Double.isInfinite(value))
				throw new IllegalArgumentException("Cannot convert Infinity to long");
			double truncated = value < 0 ? Math.ceil(value) : Math.floor(value);
			if (truncated < -0x1p63 || truncated >= 0x1p63)
				throw new IllegalArgumentException("Value " + value + " cannot be represented as long");
			return (long) truncated;
		}
		BigDecimal value = requireFiniteNumber(node, "long").setScale(0, RoundingMode.DOWN);
		try {
			return value.longValueExact();
		} catch (ArithmeticException e) {
			throw cannotRepresent(value, "long", e);
		}
	}

	@Override
	public int asInt(JsonElement node) {
		BigDecimal value = requireFiniteNumber(node, "int");
		try {
			return value.intValueExact();
		} catch (ArithmeticException e) {
			throw cannotRepresent(value, "int", e);
		}
	}

	@Override
	public int asIntTruncated(JsonElement node) {
		BigDecimal value = requireFiniteNumber(node, "int").setScale(0, RoundingMode.DOWN);
		try {
			return value.intValueExact();
		} catch (ArithmeticException e) {
			throw cannotRepresent(value, "int", e);
		}
	}

	private static BigDecimal requireFiniteNumber(JsonElement node, String targetType) {
		if (!node.isJsonPrimitive() || !node.getAsJsonPrimitive().isNumber())
			throw new IllegalArgumentException("Cannot convert non-number to " + targetType);
		JsonPrimitive primitive = node.getAsJsonPrimitive();
		double value = primitive.getAsDouble();
		if (Double.isNaN(value))
			throw new IllegalArgumentException("Cannot convert NaN to " + targetType);
		if (Double.isInfinite(value))
			throw new IllegalArgumentException("Cannot convert Infinity to " + targetType);
		return primitive.getAsBigDecimal();
	}

	private static IllegalArgumentException cannotRepresent(BigDecimal value, String targetType, ArithmeticException cause) {
		return new IllegalArgumentException("Value " + value + " cannot be represented as " + targetType, cause);
	}

	@Override
	public byte[] asByteArray(JsonElement node) {
		throw new UnsupportedOperationException("Binary data is not supported by Gson provider");
	}

	@Override
	public Iterator<Map.Entry<String, JsonElement>> fields(JsonElement node) {
		if (node.isJsonObject()) {
			return node.getAsJsonObject().entrySet().iterator();
		}
		return Collections.emptyIterator();
	}

	@Override
	public Iterator<JsonElement> elements(JsonElement node) {
		if (node.isJsonArray()) {
			return node.getAsJsonArray().iterator();
		}
		if (node.isJsonObject()) {
			// For objects, return an iterator over the values (like Jackson does)
			return node.getAsJsonObject().entrySet().stream()
					.map(Map.Entry::getValue)
					.iterator();
		}
		return Collections.emptyIterator();
	}

	@Override
	public Iterator<String> fieldNames(JsonElement node) {
		if (node.isJsonObject()) {
			return node.getAsJsonObject().keySet().iterator();
		}
		return Collections.emptyIterator();
	}

	@Override
	public @Nullable JsonElement get(JsonElement node, String fieldName) {
		if (node.isJsonObject()) {
			return node.getAsJsonObject().get(fieldName);
		}
		return null;
	}

	@Override
	public @Nullable JsonElement get(JsonElement node, int index) {
		if (node.isJsonArray()) {
			JsonArray array = node.getAsJsonArray();
			if (index >= 0 && index < array.size()) {
				return array.get(index);
			}
		}
		return null;
	}

	@Override
	public int size(JsonElement node) {
		if (node.isJsonArray()) {
			return node.getAsJsonArray().size();
		}
		if (node.isJsonObject()) {
			return node.getAsJsonObject().size();
		}
		return 0;
	}

	@Override
	public boolean has(JsonElement node, String fieldName) {
		if (node.isJsonObject()) {
			return node.getAsJsonObject().has(fieldName);
		}
		return false;
	}

	@Override
	public boolean has(JsonElement node, int index) {
		if (node.isJsonArray()) {
			JsonArray array = node.getAsJsonArray();
			return index >= 0 && index < array.size();
		}
		return false;
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
