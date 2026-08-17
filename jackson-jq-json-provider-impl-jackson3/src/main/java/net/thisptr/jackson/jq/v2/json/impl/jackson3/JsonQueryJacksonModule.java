package net.thisptr.jackson.jq.v2.json.impl.jackson3;

import java.util.Iterator;
import java.util.Map;

import com.google.errorprone.annotations.Var;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.Version;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.DoubleNode;
import tools.jackson.databind.node.FloatNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.ser.std.StdSerializer;

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

	private static String format(@Var double val) {
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

	private static class ArrayNodeSerializer extends StdSerializer<ArrayNode> {
		private static long serialVersionUID = 1L;

		ArrayNodeSerializer() {
			super(ArrayNode.class);
		}

		@Override
		public void serialize(ArrayNode value, JsonGenerator gen, SerializationContext serializers) throws JacksonException {
			gen.writeStartArray();
			for (JsonNode element : value)
				gen.writePOJO(element);
			gen.writeEndArray();
		}
	}

	private static class ObjectNodeSerializer extends StdSerializer<ObjectNode> {
		private static long serialVersionUID = 1L;

		ObjectNodeSerializer() {
			super(ObjectNode.class);
		}

		@Override
		public void serialize(ObjectNode value, JsonGenerator gen, SerializationContext serializers) throws JacksonException {
			gen.writeStartObject();
			Iterator<Map.Entry<String, JsonNode>> iter = value.properties().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, JsonNode> entry = iter.next();
				gen.writeName(entry.getKey());
				gen.writePOJO(entry.getValue());
			}
			gen.writeEndObject();
		}
	}

	private static class DoubleNodeSerializer extends StdSerializer<DoubleNode> {
		private static long serialVersionUID = 1L;

		DoubleNodeSerializer() {
			super(DoubleNode.class);
		}

		@Override
		public void serialize(DoubleNode value, JsonGenerator gen, SerializationContext serializers) throws JacksonException {
			gen.writeRawValue(format(value.asDouble()));
		}
	}

	private static class FloatNodeSerializer extends StdSerializer<FloatNode> {
		private static long serialVersionUID = 1L;

		FloatNodeSerializer() {
			super(FloatNode.class);
		}

		@Override
		public void serialize(FloatNode value, JsonGenerator gen, SerializationContext serializers) throws JacksonException {
			gen.writeRawValue(format(value.asDouble()));
		}
	}
}
