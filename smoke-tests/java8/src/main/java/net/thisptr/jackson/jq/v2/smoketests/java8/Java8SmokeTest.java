package net.thisptr.jackson.jq.v2.smoketests.java8;

import java.util.ArrayList;
import java.util.List;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.gson.GsonJsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public final class Java8SmokeTest {
	private Java8SmokeTest() {
	}

	public static void main(String[] args) throws Exception {
		testWithJsonProvider(Jackson2JsonProvider.getInstance());
		testWithJsonProvider(GsonJsonProvider.getInstance());
		System.out.println("Java 8 compatibility test passed for Jackson 2, Gson, and all extension modules");
	}

	private static <JsonNode> void testWithJsonProvider(JsonProvider<JsonNode> jsonProvider) throws Exception {
		Version version = Version.valueOf("1.6");
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(jsonProvider, version, Java8SmokeTest.class.getClassLoader())
				.build();

		assertQuery(jsonProvider, env, "length", "[1,2]", 2);
		assertQuery(jsonProvider, env, "test(\"a.c\")", "\"abc\"", true);
		assertQuery(jsonProvider, env, "import \"jackson-jq/fs\" as fs; true", "null", true);
		assertQuery(jsonProvider, env, "import \"jackson-jq/gzip\" as gzip; gzip::decompress_text", "\"H4sIAAAAAAAAA8tIzcnJBwCGphA2BQAAAA==\"", "hello");
		assertQuery(jsonProvider, env, "import \"jackson-jq/http\" as http; true", "null", true);
		assertQuery(jsonProvider, env, "import \"jackson-jq/random\" as random; random::random | . >= 0 and . < 1", "null", true);
		assertQuery(jsonProvider, env, "import \"jackson-jq/re2\" as re; re::test(\"a.c\")", "\"abc\"", true);
		assertQuery(jsonProvider, env, "import \"jackson-jq/time\" as time; 1477162342372 | time::strftime(\"yyyy-MM-dd HH:mm:ss.SSSXXX\"; \"UTC\")", "null", "2016-10-22 18:52:22.372Z");
		assertQuery(jsonProvider, env, "import \"jackson-jq/uri\" as uri; uri::uridecode", "\"%66%6f%6f\"", "foo");
		assertQuery(jsonProvider, env, "import \"jackson-jq/uuid\" as uuid; uuid::uuid5(\"6ba7b810-9dad-11d1-80b4-00c04fd430c8\")", "\"example.com\"", "cfbff0d1-9375-5685-968c-48ce8b15ae17");
		assertQuery(jsonProvider, env, "import \"jackson-jq/zstd\" as zstd; zstd::decompress_text", "\"KLUv/QRYKQAAaGVsbG+jbZ+I\"", "hello");
	}

	private static <JsonNode> void assertQuery(JsonProvider<JsonNode> jsonProvider, Environment<JsonNode> env, String expression, String inputJson, Object expected) throws Exception {
		JsonQuery<JsonNode> query = env.compile(expression);
		JsonNode input = jsonProvider.parse(inputJson);
		List<JsonNode> output = new ArrayList<>();
		query.apply(input, output::add);
		if (output.size() != 1 || !output.get(0).equals(toJsonNode(jsonProvider, expected)))
			throw new AssertionError("Expected [" + expected + "] but was " + output);
	}

	private static <JsonNode> JsonNode toJsonNode(JsonProvider<JsonNode> jsonProvider, Object value) {
		if (value instanceof Integer)
			return jsonProvider.createNumber((Integer) value);
		if (value instanceof Boolean)
			return jsonProvider.createBoolean((Boolean) value);
		if (value instanceof String)
			return jsonProvider.createString((String) value);
		throw new IllegalArgumentException("Unsupported type: " + value.getClass());
	}
}
