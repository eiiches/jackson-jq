package net.thisptr.jackson.jq.internal.functions;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.TextNode;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;

@BuiltinFunction("tojson/0")
public class ToJsonFunction implements Function {
	private static String format(double val) {
		if (Double.isNaN(val))
			return "null";

		if (Double.isInfinite(val) && val > 0)
			val = Double.MAX_VALUE;
		if (Double.isInfinite(val) && val < 0)
			val = -Double.MAX_VALUE;

		String repr = Double.toString(val);
		if (repr.contains("E-")) {
			return repr.replace('E', 'e');
		} else {
			return repr.replace("E", "e+");
		}
	}

	private static class DoubleNodeSerializer extends JsonSerializer<DoubleNode> {
		@Override
		public void serialize(DoubleNode value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
			gen.writeRaw(format(value.asDouble()));
		}
	}

	private static class FloatNodeSerializer extends JsonSerializer<FloatNode> {
		@Override
		public void serialize(FloatNode value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
			gen.writeRaw(format(value.asDouble()));
		}
	}

	private static final ObjectMapper MAPPER = new ObjectMapper();
	static {
		final SimpleModule module = new SimpleModule("JsonQuery", new Version(1, 0, 0, null, "net.thisptr", "jackson-jq"));
		module.addSerializer(DoubleNode.class, new DoubleNodeSerializer());
		module.addSerializer(FloatNode.class, new FloatNodeSerializer());
		MAPPER.registerModule(module);
	}

	@Override
	public List<JsonNode> apply(final Scope scope, final List<JsonQuery> args, final JsonNode in) throws JsonQueryException {
		try {
			return Collections.<JsonNode> singletonList(new TextNode(MAPPER.writeValueAsString(in)));
		} catch (IOException e) {
			throw new JsonQueryException(e);
		}
	}
}