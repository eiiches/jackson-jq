package net.thisptr.jackson.jq.internal.misc;

import java.util.Map.Entry;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.Version;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.DoubleNode;
import tools.jackson.databind.node.FloatNode;
import tools.jackson.databind.node.ObjectNode;

public class JsonQueryJacksonModule extends SimpleModule {
	private static final long serialVersionUID = 1137650244815104623L;

	private static final JsonQueryJacksonModule INSTANCE = new JsonQueryJacksonModule();

	public static JsonQueryJacksonModule getInstance() {
		return INSTANCE;
	}

	private JsonQueryJacksonModule() {
		super("JsonQuery", new Version(1, 0, 0, null, "net.thisptr", "jackson-jq"));
		addSerializer(DoubleNode.class, new DoubleNodeSerializer());
		addSerializer(FloatNode.class, new FloatNodeSerializer());
		addSerializer(ArrayNode.class, new ArrayNodeSerializer());
		addSerializer(ObjectNode.class, new ObjectNodeSerializer());
	}

	private static String format(double val) {
		if (Double.isNaN(val))
			return "null";

		if (Double.isInfinite(val) && val > 0)
			val = Double.MAX_VALUE;
		if (Double.isInfinite(val) && val < 0)
			val = -Double.MAX_VALUE;

		String repr = (val == (long) val) ? Long.toString((long) val) : Double.toString(val);
		if (repr.contains("E-")) {
			return repr.replace('E', 'e');
		} else {
			return repr.replace("E", "e+");
		}
	}

	private static class ArrayNodeSerializer extends ValueSerializer<ArrayNode> {
		@Override
		public void serialize(final ArrayNode value, final JsonGenerator gen, final SerializationContext serializers) {
			gen.writeStartArray();
			for (final JsonNode element : value)
				gen.writePOJO(element);
			gen.writeEndArray();
		}
	}

	private static class ObjectNodeSerializer extends ValueSerializer<ObjectNode> {

		@Override
		public void serialize(final ObjectNode value, final JsonGenerator gen, final SerializationContext serializers) {
			gen.writeStartObject();
			for (final Entry<String, JsonNode> entry : value.properties()) {
				gen.writePOJOProperty(entry.getKey(), entry.getValue());
			}
			gen.writeEndObject();
		}
	}

	private static class DoubleNodeSerializer extends ValueSerializer<DoubleNode> {
		@Override
		public void serialize(DoubleNode value, JsonGenerator gen, SerializationContext serializers) {
			gen.writeRawValue(format(value.asDouble()));
		}
	}

	private static class FloatNodeSerializer extends ValueSerializer<FloatNode> {
		@Override
		public void serialize(FloatNode value, JsonGenerator gen, SerializationContext serializers) {
			gen.writeRawValue(format(value.asDouble()));
		}
	}
}
