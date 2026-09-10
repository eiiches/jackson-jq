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
import com.fasterxml.jackson.databind.node.BinaryNode;
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
	public JsonNode createBinary(byte[] bytes) {
		return BinaryNode.valueOf(bytes);
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

	// JsonNode's own predicates are cheaper than getNodeType(), which re-maps Jackson's enum.
	// They also answer for MissingNode and POJONode, which getNodeType() rejects outright; the
	// jq engine never holds either, so the two forms agree on every node it can see.
	@Override
	public boolean isObject(JsonNode node) {
		return node.isObject();
	}

	@Override
	public boolean isArray(JsonNode node) {
		return node.isArray();
	}

	@Override
	public boolean isString(JsonNode node) {
		// Jackson 2 has no isString(); isTextual() is the same test.
		return node.isTextual();
	}

	@Override
	public boolean isNumber(JsonNode node) {
		return node.isNumber();
	}

	@Override
	public boolean isBoolean(JsonNode node) {
		return node.isBoolean();
	}

	@Override
	public boolean isNull(JsonNode node) {
		return node.isNull();
	}

	@Override
	public boolean isBinary(JsonNode node) {
		return node.isBinary();
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
	public boolean getBoolean(JsonNode node) {
		if (!node.isBoolean())
			throw new IllegalArgumentException("Cannot get the boolean value of " + getNodeType(node));
		return node.booleanValue();
	}

	@Override
	public double getNumberAsDoubleRounded(JsonNode node) {
		if (!node.isNumber())
			throw new IllegalArgumentException("Cannot convert non-number to double");
		// asDouble() rather than doubleValue(): Jackson 3's doubleValue() rejects values outside the
		// double range, where this method is specified to overflow to an infinity.
		return node.asDouble();
	}

	@Override
	public @Nullable BigDecimal getNumberAsBigDecimalExact(JsonNode node) {
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
	public @Nullable BigInteger getNumberAsBigIntegerExact(JsonNode node) {
		if (!node.isNumber())
			throw new IllegalArgumentException("Cannot convert non-number to BigInteger");
		BigDecimal value = getNumberAsBigDecimalExact(node);
		if (value == null)
			return null;
		try {
			return value.toBigIntegerExact();
		} catch (ArithmeticException e) {
			return null;
		}
	}

	@Override
	public @Nullable BigInteger getNumberAsBigIntegerTruncated(JsonNode node) {
		if (!node.isNumber())
			throw new IllegalArgumentException("Cannot convert non-number to BigInteger");
		BigDecimal value = getNumberAsBigDecimalExact(node);
		return value == null ? null : value.toBigInteger();
	}

	@Override
	public String getString(JsonNode node) {
		// Prefer isTextual()/textValue() over isString()/stringValue() for broader Jackson 2.x version compatibility
		if (!node.isTextual())
			throw new IllegalArgumentException("Cannot get the string value of " + getNodeType(node));
		return node.textValue();
	}

	@Override
	public @Nullable Long getNumberAsLongExact(JsonNode node) {
		if (!node.isNumber())
			throw new IllegalArgumentException("Cannot convert non-number to long");
		if (node.isIntegralNumber()) {
			if (!node.canConvertToLong())
				return null;
			return node.longValue();
		}
		// Widening a float to a double is exact, so both share this path.
		if (node.isDouble() || node.isFloat()) {
			double value = node.doubleValue();
			if (!Double.isFinite(value) || value != Math.rint(value) || value < -0x1p63 || value >= 0x1p63)
				return null;
			return (long) value;
		}
		try {
			return node.decimalValue().longValueExact();
		} catch (ArithmeticException e) {
			return null;
		}
	}

	@Override
	public @Nullable Long getNumberAsLongTruncated(JsonNode node) {
		if (!node.isNumber())
			throw new IllegalArgumentException("Cannot convert non-number to long");
		if (node.isIntegralNumber()) {
			if (!node.canConvertToLong())
				return null;
			return node.longValue();
		}
		if (node.isDouble() || node.isFloat()) {
			double value = node.doubleValue();
			if (!Double.isFinite(value))
				return null;
			double truncated = value < 0 ? Math.ceil(value) : Math.floor(value);
			if (truncated < -0x1p63 || truncated >= 0x1p63)
				return null;
			return (long) truncated;
		}
		try {
			return node.decimalValue().setScale(0, RoundingMode.DOWN).longValueExact();
		} catch (ArithmeticException e) {
			return null;
		}
	}

	@Override
	public @Nullable Integer getNumberAsIntExact(JsonNode node) {
		if (!node.isNumber())
			throw new IllegalArgumentException("Cannot convert non-number to int");
		if (node.isIntegralNumber()) {
			if (!node.canConvertToInt())
				return null;
			return node.intValue();
		}
		if (node.isDouble() || node.isFloat()) {
			double value = node.doubleValue();
			if (!Double.isFinite(value) || value != Math.rint(value) || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE)
				return null;
			return (int) value;
		}
		try {
			return node.decimalValue().intValueExact();
		} catch (ArithmeticException e) {
			return null;
		}
	}

	@Override
	public @Nullable Integer getNumberAsIntTruncated(JsonNode node) {
		if (!node.isNumber())
			throw new IllegalArgumentException("Cannot convert non-number to int");
		if (node.isIntegralNumber()) {
			if (!node.canConvertToInt())
				return null;
			return node.intValue();
		}
		if (node.isDouble() || node.isFloat()) {
			double value = node.doubleValue();
			if (!Double.isFinite(value))
				return null;
			double truncated = value < 0 ? Math.ceil(value) : Math.floor(value);
			if (truncated < Integer.MIN_VALUE || truncated > Integer.MAX_VALUE)
				return null;
			return (int) truncated;
		}
		try {
			return node.decimalValue().setScale(0, RoundingMode.DOWN).intValueExact();
		} catch (ArithmeticException e) {
			return null;
		}
	}

	@Override
	public byte[] getBinaryAsByteArray(JsonNode node) {
		if (!(node instanceof BinaryNode))
			throw new IllegalArgumentException("Cannot get the binary value of " + getNodeType(node));
		// BinaryNode.binaryValue() does not declare the IOException that JsonNode.binaryValue() does.
		return ((BinaryNode) node).binaryValue();
	}

	@Override
	// Prefer fields() over properties() for broader Jackson 2.x version compatibility
	@SuppressWarnings("deprecation")
	public Iterator<Map.Entry<String, JsonNode>> getObjectMembers(JsonNode node) {
		if (!node.isObject())
			throw new IllegalArgumentException("Expected an object node");
		return node.fields();
	}

	@Override
	public Iterator<JsonNode> getArrayElements(JsonNode node) {
		if (!node.isArray())
			throw new IllegalArgumentException("Expected an array node");
		return node.elements();
	}

	@Override
	public Iterator<JsonNode> getObjectMemberValues(JsonNode node) {
		if (!node.isObject())
			throw new IllegalArgumentException("Expected an object node");
		return node.elements();
	}

	@Override
	public Iterator<String> getObjectMemberNames(JsonNode node) {
		if (!node.isObject())
			throw new IllegalArgumentException("Expected an object node");
		return node.fieldNames();
	}

	@Override
	public @Nullable JsonNode getObjectMember(JsonNode node, String name) {
		if (!node.isObject())
			throw new IllegalArgumentException("Expected an object node");
		return node.get(name);
	}

	@Override
	public JsonNode getArrayElement(JsonNode node, int index) {
		if (!node.isArray())
			throw new IllegalArgumentException("Expected an array node");
		if (index < 0 || index >= node.size())
			throw new IndexOutOfBoundsException("Index " + index + " out of bounds for array length " + node.size());
		return node.get(index);
	}

	@Override
	public int getArrayLength(JsonNode node) {
		if (!node.isArray())
			throw new IllegalArgumentException("Expected an array node");
		return node.size();
	}

	@Override
	public int getObjectMemberCount(JsonNode node) {
		if (!node.isObject())
			throw new IllegalArgumentException("Expected an object node");
		return node.size();
	}

	@Override
	public boolean hasObjectMember(JsonNode node, String name) {
		if (!node.isObject())
			throw new IllegalArgumentException("Expected an object node");
		return node.has(name);
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

}
