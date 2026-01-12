package net.thisptr.jackson.jq.jackson2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;

public class Jackson2JsonProviderImpl implements JsonProvider<JsonNode> {
	private static final Jackson2JsonProviderImpl DEFAULT_INSTANCE = new Jackson2JsonProviderImpl(new ObjectMapper().registerModule(JsonQueryJacksonModule.getInstance()));

	private final ObjectMapper mapper;

	public Jackson2JsonProviderImpl(final ObjectMapper mapper) {
		this.mapper = mapper;
	}

	/**
	 * Returns a singleton instance using a default ObjectMapper.
	 */
	public static Jackson2JsonProviderImpl getInstance() {
		return DEFAULT_INSTANCE;
	}

	@Override
	public JsonNode createObject() {
		return mapper.createObjectNode();
	}

	@Override
	public JsonNode createArray() {
		return mapper.createArrayNode();
	}

	@Override
	public JsonNode createString(final String value) {
		return TextNode.valueOf(value);
	}

	@Override
	public JsonNode createLong(final long value) {
		return new LongNode(value);
	}

	@Override
	public JsonNode createInt(final int value) {
		return new IntNode(value);
	}

	@Override
	public JsonNode createDouble(final double value) {
		return new DoubleNode(value);
	}

	@Override
	public JsonNode createBoolean(final boolean value) {
		return BooleanNode.valueOf(value);
	}

	@Override
	public JsonNode createNull() {
		return NullNode.getInstance();
	}

	@Override
	public JsonNode createMissing() {
		return MissingNode.getInstance();
	}

	@Override
	public JsonNodeType getNodeType(final JsonNode node) {
		switch (node.getNodeType()) {
			case ARRAY:
				return JsonNodeType.ARRAY;
			case BINARY:
				return JsonNodeType.BINARY;
			case BOOLEAN:
				return JsonNodeType.BOOLEAN;
			case MISSING:
				return JsonNodeType.MISSING;
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
	public boolean isMissingNode(final JsonNode node) {
		return node.isMissingNode();
	}

	@Override
	public boolean asBoolean(final JsonNode node) {
		return node.asBoolean();
	}

	@Override
	public double asDouble(final JsonNode node) {
		return node.asDouble();
	}

	@Override
	public String asText(final JsonNode node) {
		return node.asText();
	}

	@Override
	public long asLong(final JsonNode node) {
		if (node.isNumber()) {
			double d = node.asDouble();
			if (Double.isNaN(d)) {
				throw new IllegalArgumentException("Cannot convert NaN to long");
			}
			if (Double.isInfinite(d)) {
				throw new IllegalArgumentException("Cannot convert Infinity to long");
			}
		}
		return node.asLong();
	}

	@Override
	public int asInt(final JsonNode node) {
		if (node.isNumber()) {
			double d = node.asDouble();
			if (Double.isNaN(d)) {
				throw new IllegalArgumentException("Cannot convert NaN to int");
			}
			if (Double.isInfinite(d)) {
				throw new IllegalArgumentException("Cannot convert Infinity to int");
			}
			long l = node.asLong();
			if (l > Integer.MAX_VALUE || l < Integer.MIN_VALUE) {
				throw new IllegalArgumentException("Value " + l + " is outside the range of int");
			}
		}
		return node.asInt();
	}

	@Override
	public byte[] asByteArray(final JsonNode node) {
		try {
			return node.binaryValue();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Iterator<Entry<String, JsonNode>> fields(final JsonNode node) {
		return node.fields();
	}

	@Override
	public Iterator<JsonNode> elements(final JsonNode node) {
		return node.elements();
	}

	@Override
	public Iterator<String> fieldNames(final JsonNode node) {
		return node.fieldNames();
	}

	@Override
	public JsonNode get(final JsonNode node, final String fieldName) {
		return node.get(fieldName);
	}

	@Override
	public JsonNode get(final JsonNode node, final int index) {
		return node.get(index);
	}

	@Override
	public JsonNode set(final JsonNode node, final String fieldName, final JsonNode value) {
		((ObjectNode) node).set(fieldName, value);
		return node;
	}

	@Override
	public JsonNode add(final JsonNode node, final JsonNode value) {
		((ArrayNode) node).add(value);
		return node;
	}

	@Override
	public JsonNode set(final JsonNode node, final int index, final JsonNode value) {
		((ArrayNode) node).set(index, value);
		return node;
	}

	@Override
	public int size(final JsonNode node) {
		return node.size();
	}

	@Override
	public boolean has(final JsonNode node, final String fieldName) {
		return node.has(fieldName);
	}

	@Override
	public boolean has(final JsonNode node, final int index) {
		return node.has(index);
	}

	@Override
	public JsonNode deepCopy(final JsonNode node) {
		return node.deepCopy();
	}

	@Override
	public String toString(final JsonNode node) {
		try {
			return mapper.writeValueAsString(node);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public JsonNode fromString(final String json) throws IOException {
		return mapper.readTree(json);
	}

	@Override
	public JsonNode fromStringStrict(final String json) throws IOException {
		try (final com.fasterxml.jackson.core.JsonParser parser = mapper.getFactory().createParser(json)) {
			final JsonNode tree = parser.readValueAsTree();
			if (tree == null)
				throw new IOException("empty input");
			if (parser.nextToken() != null)
				throw new IOException("trailing content");
			return tree;
		}
	}

	@Override
	public List<JsonNode> readMultipleValues(final String json) throws IOException {
		final List<JsonNode> result = new ArrayList<>();
		try (final MappingIterator<JsonNode> iter = mapper.readValues(mapper.getFactory().createParser(json), JsonNode.class)) {
			while (iter.hasNext()) {
				result.add(iter.next());
			}
		}
		return result;
	}

	@Override
	public JsonNode valueToTree(final Object value) {
		return mapper.valueToTree(value);
	}

	@Override
	public boolean isJsonNodeInstance(final Object arg) {
		return arg instanceof JsonNode;
	}
}
