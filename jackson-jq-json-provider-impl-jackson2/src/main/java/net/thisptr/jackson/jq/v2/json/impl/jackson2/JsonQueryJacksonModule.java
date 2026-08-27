package net.thisptr.jackson.jq.v2.json.impl.jackson2;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

		if (Double.isInfinite(val))
			return (val > 0) ? "1.7976931348623157e+308" : "-1.7976931348623157e+308";

		String repr = (val == (long) val) ? Long.toString((long) val) : Double.toString(val);
		if (repr.contains("E-")) {
			return repr;
		} else {
			return repr.replace("E", "E+");
		}
	}

	private static class ArrayNodeSerializer extends JsonSerializer<ArrayNode> {
		@Override
		public void serialize(ArrayNode value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
			gen.writeStartArray();
			for (JsonNode element : value)
				gen.writeObject(element);
			gen.writeEndArray();
		}
	}

	private static class ObjectNodeSerializer extends JsonSerializer<ObjectNode> {

		@Override
		public void serialize(ObjectNode value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
			gen.writeStartObject();
			// Prefer fields() over properties() for broader Jackson 2.x version compatibility
			@SuppressWarnings("deprecation")
			Iterator<Map.Entry<String, JsonNode>> iter = value.fields();
			while (iter.hasNext()) {
				Map.Entry<String, JsonNode> entry = iter.next();
				gen.writeObjectField(entry.getKey(), entry.getValue());
			}
			gen.writeEndObject();
		}
	}

	private static class DoubleNodeSerializer extends JsonSerializer<DoubleNode> {
		@Override
		public void serialize(DoubleNode value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
			gen.writeRawValue(format(value.asDouble()));
		}
	}

	private static class FloatNodeSerializer extends JsonSerializer<FloatNode> {
		@Override
		public void serialize(FloatNode value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
			gen.writeRawValue(format(value.asDouble()));
		}
	}
}
