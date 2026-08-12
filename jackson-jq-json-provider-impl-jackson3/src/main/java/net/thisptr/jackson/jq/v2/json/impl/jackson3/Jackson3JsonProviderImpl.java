package net.thisptr.jackson.jq.v2.json.impl.jackson3;

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

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;

public class Jackson3JsonProviderImpl implements JsonProvider<JsonNode> {
	private static final Jackson3JsonProviderImpl DEFAULT_INSTANCE = new Jackson3JsonProviderImpl(JsonMapper.builder().addModule(JsonQueryJacksonModule.getInstance()).build());

	private final ObjectMapper mapper;

	public Jackson3JsonProviderImpl(ObjectMapper mapper) {
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
	public JsonNode createString(String value) {
		return StringNode.valueOf(value);
	}

	@Override
	public JsonNode createLong(long value) {
		return LongNode.valueOf(value);
	}

	@Override
	public JsonNode createInt(int value) {
		return IntNode.valueOf(value);
	}

	@Override
	public JsonNode createDouble(double value) {
		return DoubleNode.valueOf(value);
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
	public JsonNode createMissing() {
		return MissingNode.getInstance();
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
	public boolean isMissingNode(JsonNode node) {
		return node.isMissingNode();
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
	public String asText(JsonNode node) {
		// Jackson3's NullNode.asString() returns "" but we need "null" to match Jackson2 behavior
		if (node.isNull()) {
			return "null";
		}
		return node.asString();
	}

	@Override
	public long asLong(JsonNode node) {
		return node.asLong();
	}

	@Override
	public int asInt(JsonNode node) {
		return node.asInt();
	}

	@Override
	public byte[] asByteArray(JsonNode node) {
		return node.binaryValue();
	}

	@Override
	public Iterator<Entry<String, JsonNode>> fields(JsonNode node) {
		return node.properties().iterator();
	}

	@Override
	public Iterator<JsonNode> elements(JsonNode node) {
		return node.iterator();
	}

	@Override
	public Iterator<String> fieldNames(JsonNode node) {
		return node.propertyNames().iterator();
	}

	@Override
	public JsonNode get(JsonNode node, String fieldName) {
		return node.get(fieldName);
	}

	@Override
	public JsonNode get(JsonNode node, int index) {
		return node.get(index);
	}

	@Override
	public JsonNode set(JsonNode node, String fieldName, JsonNode value) {
		((ObjectNode) node).set(fieldName, value);
		return node;
	}

	@Override
	public JsonNode add(JsonNode node, JsonNode value) {
		((ArrayNode) node).add(value);
		return node;
	}

	@Override
	public JsonNode set(JsonNode node, int index, JsonNode value) {
		((ArrayNode) node).set(index, value);
		return node;
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
	public String toString(JsonNode node) {
		try {
			return mapper.writeValueAsString(node);
		} catch (JacksonException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public JsonNode fromString(String json) throws IOException {
		try {
			return mapper.readTree(json);
		} catch (JacksonException e) {
			throw new IOException(e);
		}
	}

	@Override
	public JsonNode fromStringStrict(String json) throws IOException {
		try (JsonParser parser = mapper.createParser(json)) {
			JsonNode tree = parser.readValueAsTree();
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
	public List<JsonNode> readMultipleValues(String json) throws IOException {
		List<JsonNode> result = new ArrayList<>();
		try (MappingIterator<JsonNode> iter = mapper.readerFor(JsonNode.class).readValues(json)) {
			while (iter.hasNext()) {
				result.add(iter.next());
			}
		} catch (JacksonException e) {
			throw new IOException(e);
		}
		return result;
	}

	@Override
	public JsonNode valueToTree(Object value) {
		return mapper.valueToTree(value);
	}

	@Override
	public boolean isJsonNodeInstance(Object arg) {
		return arg instanceof JsonNode;
	}
}
