package net.thisptr.jackson.jq.v2.json.impl.jackson2;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BigIntegerNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonException;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonParser;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.NumberType;

public class Jackson2JsonProviderImpl implements JsonProvider<JsonNode> {
	private static final Jackson2JsonProviderImpl DEFAULT_INSTANCE = new Jackson2JsonProviderImpl(new ObjectMapper().registerModule(JsonQueryJacksonModule.getInstance()));

	private final ObjectMapper mapper;

	public Jackson2JsonProviderImpl(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	/**
	 * Returns a singleton instance using a default ObjectMapper.
	 */
	public static Jackson2JsonProviderImpl getInstance() {
		return DEFAULT_INSTANCE;
	}

	@Override
	public JsonNode createObject(Map<String, ? extends JsonNode> values) {
		ObjectNode result = mapper.createObjectNode();
		for (Map.Entry<String, ? extends JsonNode> entry : values.entrySet()) {
			result.set(entry.getKey(), entry.getValue());
		}
		return result;
	}

	@Override
	public JsonNode createArray(Iterable<? extends JsonNode> values) {
		ArrayNode result = mapper.createArrayNode();
		for (JsonNode value : values) {
			result.add(value);
		}
		return result;
	}

	@Override
	public JsonNode createString(String value) {
		return TextNode.valueOf(value);
	}

	@Override
	public JsonNode createNumber(long value) {
		return new LongNode(value);
	}

	@Override
	public JsonNode createNumber(int value) {
		return new IntNode(value);
	}

	@Override
	public JsonNode createNumber(float value) {
		return new FloatNode(value);
	}

	@Override
	public JsonNode createNumber(double value) {
		return new DoubleNode(value);
	}

	@Override
	public JsonNode createNumber(BigInteger value) {
		return new BigIntegerNode(value);
	}

	@Override
	public JsonNode createNumber(BigDecimal value) {
		return new DecimalNode(value);
	}

	@Override
	public JsonNode createBoolean(boolean value) {
		return BooleanNode.valueOf(value);
	}

	@Override
	public JsonNode createNull() {
		return NullNode.getInstance();
	}

	@Override
	public JsonNodeType getNodeType(JsonNode node) {
		switch (node.getNodeType()) {
			case ARRAY:
				return JsonNodeType.ARRAY;
			case BINARY:
				return JsonNodeType.BINARY;
			case BOOLEAN:
				return JsonNodeType.BOOLEAN;
			case NULL:
				return JsonNodeType.NULL;
			case NUMBER:
				return JsonNodeType.NUMBER;
			case OBJECT:
				return JsonNodeType.OBJECT;
			case STRING:
				return JsonNodeType.STRING;
			default:
				throw new IllegalStateException("Unknown JsonNodeType: " + node.getNodeType());
		}
	}

	@Override
	public NumberType getNumberType(JsonNode node) {
		// NumericNode is exactly what JsonNode.isNumber() covers.
		if (!(node instanceof NumericNode))
			throw new IllegalArgumentException("Cannot get the number type of " + getNodeType(node));
		switch (((NumericNode) node).numberType()) {
			case INT:
				// Jackson maps ShortNode here too.
				return NumberType.INT;
			case LONG:
				return NumberType.LONG;
			case BIG_INTEGER:
				return NumberType.BIG_INTEGER;
			case BIG_DECIMAL:
				return NumberType.BIG_DECIMAL;
			case DOUBLE:
				return NumberType.DOUBLE;
			case FLOAT:
				return NumberType.FLOAT;
			default:
				return NumberType.UNKNOWN;
		}
	}

	@Override
	public boolean asBoolean(JsonNode node) {
		return node.asBoolean();
	}

	@Override
	public double asDouble(JsonNode node) {
		return node.asDouble();
	}

	@Override
	public @Nullable BigDecimal asBigDecimal(JsonNode node) {
		if (!node.isNumber())
			throw new IllegalArgumentException("Cannot convert non-number to BigDecimal");
		if (node.isDouble() && !Double.isFinite(node.doubleValue()))
			return null;
		if (node.isFloat() && !Float.isFinite(node.floatValue()))
			return null;
		// DoubleNode/FloatNode use the shortest round-trip representation (e.g. 0.1 stays 0.1),
		// not the exact binary expansion.
		return node.decimalValue();
	}

	@Override
	public String asString(JsonNode node) {
		return node.asText();
	}

	@Override
	public long asLong(JsonNode node) {
		if (!node.isNumber())
			throw new IllegalArgumentException("Cannot convert non-number to long");
		if (node.isIntegralNumber()) {
			if (node.canConvertToLong())
				return node.longValue();
			throw new IllegalArgumentException("Value " + node + " cannot be represented as long");
		}
		if (node.isDouble()) {
			double value = node.doubleValue();
			if (Double.isNaN(value))
				throw new IllegalArgumentException("Cannot convert NaN to long");
			if (Double.isInfinite(value))
				throw new IllegalArgumentException("Cannot convert Infinity to long");
			if (value != Math.rint(value) || value < -0x1p63 || value >= 0x1p63)
				throw new IllegalArgumentException("Value " + value + " cannot be represented as long");
			return (long) value;
		}
		if (node.isFloat()) {
			float value = node.floatValue();
			if (Float.isNaN(value))
				throw new IllegalArgumentException("Cannot convert NaN to long");
			if (Float.isInfinite(value))
				throw new IllegalArgumentException("Cannot convert Infinity to long");
			if (value != Math.rint(value) || value < -0x1p63 || value >= 0x1p63)
				throw new IllegalArgumentException("Value " + value + " cannot be represented as long");
			return (long) value;
		}
		BigDecimal value = node.decimalValue();
		try {
			return value.longValueExact();
		} catch (ArithmeticException e) {
			throw new IllegalArgumentException("Value " + value + " cannot be represented as long", e);
		}
	}

	@Override
	public long asLongTruncated(JsonNode node) {
		if (!node.isNumber())
			throw new IllegalArgumentException("Cannot convert non-number to long");
		if (node.isIntegralNumber()) {
			if (node.canConvertToLong())
				return node.longValue();
			throw new IllegalArgumentException("Value " + node + " cannot be represented as long");
		}
		if (node.isDouble()) {
			double value = node.doubleValue();
			if (Double.isNaN(value))
				throw new IllegalArgumentException("Cannot convert NaN to long");
			if (Double.isInfinite(value))
				throw new IllegalArgumentException("Cannot convert Infinity to long");
			double truncated = value < 0 ? Math.ceil(value) : Math.floor(value);
			if (truncated < -0x1p63 || truncated >= 0x1p63)
				throw new IllegalArgumentException("Value " + value + " cannot be represented as long");
			return (long) truncated;
		}
		if (node.isFloat()) {
			float value = node.floatValue();
			if (Float.isNaN(value))
				throw new IllegalArgumentException("Cannot convert NaN to long");
			if (Float.isInfinite(value))
				throw new IllegalArgumentException("Cannot convert Infinity to long");
			double truncated = value < 0 ? Math.ceil(value) : Math.floor(value);
			if (truncated < -0x1p63 || truncated >= 0x1p63)
				throw new IllegalArgumentException("Value " + value + " cannot be represented as long");
			return (long) truncated;
		}
		BigDecimal value = node.decimalValue().setScale(0, RoundingMode.DOWN);
		try {
			return value.longValueExact();
		} catch (ArithmeticException e) {
			throw new IllegalArgumentException("Value " + value + " cannot be represented as long", e);
		}
	}

	@Override
	public int asInt(JsonNode node) {
		if (!node.isNumber())
			throw new IllegalArgumentException("Cannot convert non-number to int");
		if (node.isIntegralNumber()) {
			if (node.canConvertToInt())
				return node.intValue();
			throw new IllegalArgumentException("Value " + node + " cannot be represented as int");
		}
		if (node.isDouble()) {
			double value = node.doubleValue();
			if (Double.isNaN(value))
				throw new IllegalArgumentException("Cannot convert NaN to int");
			if (Double.isInfinite(value))
				throw new IllegalArgumentException("Cannot convert Infinity to int");
			if (value != Math.rint(value) || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE)
				throw new IllegalArgumentException("Value " + value + " cannot be represented as int");
			return (int) value;
		}
		if (node.isFloat()) {
			float value = node.floatValue();
			if (Float.isNaN(value))
				throw new IllegalArgumentException("Cannot convert NaN to int");
			if (Float.isInfinite(value))
				throw new IllegalArgumentException("Cannot convert Infinity to int");
			if (value != Math.rint(value) || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE)
				throw new IllegalArgumentException("Value " + value + " cannot be represented as int");
			return (int) value;
		}
		BigDecimal value = node.decimalValue();
		try {
			return value.intValueExact();
		} catch (ArithmeticException e) {
			throw new IllegalArgumentException("Value " + value + " cannot be represented as int", e);
		}
	}

	@Override
	public int asIntTruncated(JsonNode node) {
		if (!node.isNumber())
			throw new IllegalArgumentException("Cannot convert non-number to int");
		if (node.isIntegralNumber()) {
			if (node.canConvertToInt())
				return node.intValue();
			throw new IllegalArgumentException("Value " + node + " cannot be represented as int");
		}
		if (node.isDouble()) {
			double value = node.doubleValue();
			if (Double.isNaN(value))
				throw new IllegalArgumentException("Cannot convert NaN to int");
			if (Double.isInfinite(value))
				throw new IllegalArgumentException("Cannot convert Infinity to int");
			double truncated = value < 0 ? Math.ceil(value) : Math.floor(value);
			if (truncated < Integer.MIN_VALUE || truncated > Integer.MAX_VALUE)
				throw new IllegalArgumentException("Value " + value + " cannot be represented as int");
			return (int) truncated;
		}
		if (node.isFloat()) {
			float value = node.floatValue();
			if (Float.isNaN(value))
				throw new IllegalArgumentException("Cannot convert NaN to int");
			if (Float.isInfinite(value))
				throw new IllegalArgumentException("Cannot convert Infinity to int");
			double truncated = value < 0 ? Math.ceil(value) : Math.floor(value);
			if (truncated < Integer.MIN_VALUE || truncated > Integer.MAX_VALUE)
				throw new IllegalArgumentException("Value " + value + " cannot be represented as int");
			return (int) truncated;
		}
		BigDecimal value = node.decimalValue().setScale(0, RoundingMode.DOWN);
		try {
			return value.intValueExact();
		} catch (ArithmeticException e) {
			throw new IllegalArgumentException("Value " + value + " cannot be represented as int", e);
		}
	}

	@Override
	public byte[] asByteArray(JsonNode node) {
		try {
			return node.binaryValue();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	// Prefer fields() over properties() for broader Jackson 2.x version compatibility
	@SuppressWarnings("deprecation")
	public Iterator<Map.Entry<String, JsonNode>> fields(JsonNode node) {
		return node.fields();
	}

	@Override
	public Iterator<JsonNode> elements(JsonNode node) {
		return node.elements();
	}

	@Override
	public Iterator<String> fieldNames(JsonNode node) {
		return node.fieldNames();
	}

	@Override
	public @Nullable JsonNode get(JsonNode node, String fieldName) {
		return node.get(fieldName);
	}

	@Override
	public @Nullable JsonNode get(JsonNode node, int index) {
		return node.get(index);
	}

	@Override
	public int size(JsonNode node) {
		return node.size();
	}

	@Override
	public boolean has(JsonNode node, String fieldName) {
		return node.has(fieldName);
	}

	@Override
	public boolean has(JsonNode node, int index) {
		return node.has(index);
	}

	@Override
	public JsonNode deepCopy(JsonNode node) {
		return node.deepCopy();
	}

	@Override
	public String format(JsonNode node) {
		try {
			return mapper.writeValueAsString(node);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public JsonParser<JsonNode> createParser(InputStream in) {
		try {
			return new JacksonJsonParser(mapper.getFactory().createParser(in));
		} catch (IOException e) {
			throw new JsonException(e);
		}
	}

	/**
	 * Reads one value per call off a streaming parser. {@code ObjectReader#readValues} is deliberately
	 * not used: it unwraps a root-level array into its elements, whereas an array is a single value here.
	 */
	private static class JacksonJsonParser implements JsonParser<JsonNode> {
		private final com.fasterxml.jackson.core.JsonParser parser;

		JacksonJsonParser(com.fasterxml.jackson.core.JsonParser parser) {
			this.parser = parser;
		}

		@Override
		public @Nullable JsonNode next() {
			try {
				// readValueAsTree() binds the token the parser already sits on, so without advancing
				// first it would return the same value forever.
				if (parser.nextToken() == null)
					return null;
				return parser.readValueAsTree();
			} catch (IOException e) {
				throw new JsonException(e);
			}
		}

		@Override
		public void close() {
			try {
				parser.close();
			} catch (IOException e) {
				throw new JsonException(e);
			}
		}
	}

	@Override
	public boolean isJsonNodeInstance(@Nullable Object arg) {
		return arg instanceof JsonNode;
	}
}
