package net.thisptr.jackson.jq.gson;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonStreamParser;

import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;

public class GsonJsonProviderImpl implements JsonProvider<JsonElement> {
	private static final GsonJsonProviderImpl DEFAULT_INSTANCE = new GsonJsonProviderImpl(GsonUtils.createJqCompatibleGson());

	private final Gson gson;

	public GsonJsonProviderImpl(final Gson gson) {
		this.gson = gson;
	}

	/**
	 * Returns a singleton instance using a default Gson.
	 */
	public static GsonJsonProviderImpl getInstance() {
		return DEFAULT_INSTANCE;
	}

	@Override
	public JsonElement createObject() {
		return new JsonObject();
	}

	@Override
	public JsonElement createArray() {
		return new JsonArray();
	}

	@Override
	public JsonElement createString(final String value) {
		return new JsonPrimitive(value);
	}

	@Override
	public JsonElement createLong(final long value) {
		return new JsonPrimitive(value);
	}

	@Override
	public JsonElement createInt(final int value) {
		return new JsonPrimitive(value);
	}

	@Override
	public JsonElement createDouble(final double value) {
		return new JsonPrimitive(value);
	}

	@Override
	public JsonElement createBoolean(final boolean value) {
		return new JsonPrimitive(value);
	}

	@Override
	public JsonElement createNull() {
		return JsonNull.INSTANCE;
	}

	@Override
	public JsonElement createMissing() {
		return GsonMissingNode.INSTANCE;
	}

	@Override
	public JsonNodeType getNodeType(final JsonElement node) {
		if (node instanceof GsonMissingNode) {
			return JsonNodeType.MISSING;
		}
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
			final JsonPrimitive primitive = node.getAsJsonPrimitive();
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
	public boolean isMissingNode(final JsonElement node) {
		return node instanceof GsonMissingNode;
	}

	@Override
	public boolean asBoolean(final JsonElement node) {
		if (node.isJsonPrimitive()) {
			final JsonPrimitive primitive = node.getAsJsonPrimitive();
			if (primitive.isBoolean()) {
				return primitive.getAsBoolean();
			}
		}
		// jq semantics: null and false are falsy, everything else is truthy
		if (node.isJsonNull() || node instanceof GsonMissingNode) {
			return false;
		}
		return true;
	}

	@Override
	public double asDouble(final JsonElement node) {
		if (node.isJsonPrimitive()) {
			final JsonPrimitive primitive = node.getAsJsonPrimitive();
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
	public String asText(final JsonElement node) {
		if (node instanceof GsonMissingNode) {
			return "";
		}
		if (node.isJsonNull()) {
			return "null";
		}
		if (node.isJsonPrimitive()) {
			return node.getAsJsonPrimitive().getAsString();
		}
		return gson.toJson(node);
	}

	@Override
	public long asLong(final JsonElement node) {
		if (node.isJsonPrimitive()) {
			final JsonPrimitive primitive = node.getAsJsonPrimitive();
			if (primitive.isNumber()) {
				double d = primitive.getAsDouble();
				if (Double.isNaN(d)) {
					throw new IllegalArgumentException("Cannot convert NaN to long");
				}
				if (Double.isInfinite(d)) {
					throw new IllegalArgumentException("Cannot convert Infinity to long");
				}
				return primitive.getAsLong();
			}
			if (primitive.isString()) {
				try {
					return Long.parseLong(primitive.getAsString());
				} catch (NumberFormatException e) {
					return 0;
				}
			}
		}
		return 0;
	}

	@Override
	public int asInt(final JsonElement node) {
		if (node.isJsonPrimitive()) {
			final JsonPrimitive primitive = node.getAsJsonPrimitive();
			if (primitive.isNumber()) {
				double d = primitive.getAsDouble();
				if (Double.isNaN(d)) {
					throw new IllegalArgumentException("Cannot convert NaN to int");
				}
				if (Double.isInfinite(d)) {
					throw new IllegalArgumentException("Cannot convert Infinity to int");
				}
				long l = primitive.getAsLong();
				if (l > Integer.MAX_VALUE || l < Integer.MIN_VALUE) {
					throw new IllegalArgumentException("Value " + l + " is outside the range of int");
				}
				return primitive.getAsInt();
			}
			if (primitive.isString()) {
				try {
					return Integer.parseInt(primitive.getAsString());
				} catch (NumberFormatException e) {
					return 0;
				}
			}
		}
		return 0;
	}

	@Override
	public byte[] asByteArray(final JsonElement node) {
		throw new UnsupportedOperationException("Binary data is not supported by Gson provider");
	}

	@Override
	public Iterator<Entry<String, JsonElement>> fields(final JsonElement node) {
		if (node.isJsonObject()) {
			return node.getAsJsonObject().entrySet().iterator();
		}
		return java.util.Collections.emptyIterator();
	}

	@Override
	public Iterator<JsonElement> elements(final JsonElement node) {
		if (node.isJsonArray()) {
			return node.getAsJsonArray().iterator();
		}
		if (node.isJsonObject()) {
			// For objects, return an iterator over the values (like Jackson does)
			return node.getAsJsonObject().entrySet().stream()
					.map(Entry::getValue)
					.iterator();
		}
		return java.util.Collections.emptyIterator();
	}

	@Override
	public Iterator<String> fieldNames(final JsonElement node) {
		if (node.isJsonObject()) {
			return node.getAsJsonObject().keySet().iterator();
		}
		return java.util.Collections.emptyIterator();
	}

	@Override
	public JsonElement get(final JsonElement node, final String fieldName) {
		if (node.isJsonObject()) {
			return node.getAsJsonObject().get(fieldName);
		}
		return null;
	}

	@Override
	public JsonElement get(final JsonElement node, final int index) {
		if (node.isJsonArray()) {
			final JsonArray array = node.getAsJsonArray();
			if (index >= 0 && index < array.size()) {
				return array.get(index);
			}
		}
		return null;
	}

	@Override
	public JsonElement set(final JsonElement node, final String fieldName, final JsonElement value) {
		node.getAsJsonObject().add(fieldName, value);
		return node;
	}

	@Override
	public JsonElement add(final JsonElement node, final JsonElement value) {
		node.getAsJsonArray().add(value);
		return node;
	}

	@Override
	public JsonElement set(final JsonElement node, final int index, final JsonElement value) {
		node.getAsJsonArray().set(index, value);
		return node;
	}

	@Override
	public int size(final JsonElement node) {
		if (node.isJsonArray()) {
			return node.getAsJsonArray().size();
		}
		if (node.isJsonObject()) {
			return node.getAsJsonObject().size();
		}
		return 0;
	}

	@Override
	public boolean has(final JsonElement node, final String fieldName) {
		if (node.isJsonObject()) {
			return node.getAsJsonObject().has(fieldName);
		}
		return false;
	}

	@Override
	public boolean has(final JsonElement node, final int index) {
		if (node.isJsonArray()) {
			final JsonArray array = node.getAsJsonArray();
			return index >= 0 && index < array.size();
		}
		return false;
	}

	@Override
	public JsonElement deepCopy(final JsonElement node) {
		return node.deepCopy();
	}

	@Override
	public String toString(final JsonElement node) {
		if (node instanceof GsonMissingNode) {
			return "null";
		}
		return toJqString(node);
	}

	/**
	 * Converts a JsonElement to a jq-compatible JSON string.
	 * Handles NaN, Infinity, and number formatting.
	 */
	private String toJqString(final JsonElement node) {
		if (node == null || node.isJsonNull()) {
			return "null";
		}
		if (node.isJsonPrimitive()) {
			final JsonPrimitive primitive = node.getAsJsonPrimitive();
			if (primitive.isNumber()) {
				double val = primitive.getAsDouble();
				return GsonUtils.formatDouble(val);
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
			final JsonArray array = node.getAsJsonArray();
			final StringBuilder sb = new StringBuilder("[");
			boolean first = true;
			for (final JsonElement element : array) {
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
			final JsonObject obj = node.getAsJsonObject();
			final StringBuilder sb = new StringBuilder("{");
			boolean first = true;
			for (final Entry<String, JsonElement> entry : obj.entrySet()) {
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
	public JsonElement fromString(final String json) throws Exception {
		return JsonParser.parseString(json);
	}

	@Override
	public JsonElement fromStringStrict(final String json) throws Exception {
		if (json == null || json.isEmpty()) {
			throw new IllegalArgumentException("empty input");
		}
		final JsonStreamParser parser = new JsonStreamParser(json);
		if (!parser.hasNext()) {
			throw new IllegalArgumentException("empty input");
		}
		final JsonElement result = parser.next();
		if (parser.hasNext()) {
			throw new IllegalArgumentException("trailing content");
		}
		return result;
	}

	@Override
	public List<JsonElement> readMultipleValues(final String json) throws Exception {
		final List<JsonElement> result = new ArrayList<>();
		final JsonStreamParser parser = new JsonStreamParser(json);
		while (parser.hasNext()) {
			result.add(parser.next());
		}
		return result;
	}

	@Override
	public JsonElement valueToTree(final Object value) {
		return gson.toJsonTree(value);
	}

	@Override
	public boolean isJsonNodeInstance(final Object arg) {
		return arg instanceof JsonElement;
	}
}
