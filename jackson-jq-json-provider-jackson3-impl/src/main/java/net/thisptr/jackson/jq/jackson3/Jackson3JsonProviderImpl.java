package net.thisptr.jackson.jq.jackson3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.DoubleNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.LongNode;
import tools.jackson.databind.node.MissingNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;

public class Jackson3JsonProviderImpl implements JsonProvider<JsonNode> {
	private static final Jackson3JsonProviderImpl DEFAULT_INSTANCE = new Jackson3JsonProviderImpl(JsonMapper.builder().addModule(JsonQueryJacksonModule.getInstance()).build());

	private final ObjectMapper mapper;

	public Jackson3JsonProviderImpl(final ObjectMapper mapper) {
		this.mapper = mapper;
	}

	/**
	 * Returns a singleton instance using a default ObjectMapper.
	 */
	public static Jackson3JsonProviderImpl getInstance() {
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
		return StringNode.valueOf(value);
	}

	@Override
	public JsonNode createLong(final long value) {
		return LongNode.valueOf(value);
	}

	@Override
	public JsonNode createInt(final int value) {
		return IntNode.valueOf(value);
	}

	@Override
	public JsonNode createDouble(final double value) {
		return DoubleNode.valueOf(value);
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
		// Jackson3's NullNode.asString() returns "" but we need "null" to match Jackson2 behavior
		if (node.isNull()) {
			return "null";
		}
		return node.asString();
	}

	@Override
	public long asLong(final JsonNode node) {
		return node.asLong();
	}

	@Override
	public int asInt(final JsonNode node) {
		return node.asInt();
	}

	@Override
	public byte[] asByteArray(final JsonNode node) {
		return node.binaryValue();
	}

	@Override
	public Iterator<Entry<String, JsonNode>> fields(final JsonNode node) {
		return node.properties().iterator();
	}

	@Override
	public Iterator<JsonNode> elements(final JsonNode node) {
		return node.iterator();
	}

	@Override
	public Iterator<String> fieldNames(final JsonNode node) {
		return node.propertyNames().iterator();
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
		} catch (JacksonException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public JsonNode fromString(final String json) throws IOException {
		try {
			return mapper.readTree(json);
		} catch (JacksonException e) {
			throw new IOException(e);
		}
	}

	@Override
	public JsonNode fromStringStrict(final String json) throws IOException {
		try (final JsonParser parser = mapper.createParser(json)) {
			final JsonNode tree = parser.readValueAsTree();
			if (tree == null)
				throw new IOException("empty input");
			if (parser.nextToken() != null)
				throw new IOException("trailing content");
			return tree;
		} catch (JacksonException e) {
			throw new IOException(e);
		}
	}

	@Override
	public List<JsonNode> readMultipleValues(final String json) throws IOException {
		final List<JsonNode> result = new ArrayList<>();
		try (final MappingIterator<JsonNode> iter = mapper.readerFor(JsonNode.class).readValues(json)) {
			while (iter.hasNext()) {
				result.add(iter.next());
			}
		} catch (JacksonException e) {
			throw new IOException(e);
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
