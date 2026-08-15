package net.thisptr.jackson.jq.v2.smoketests.java8;

import java.util.ArrayList;
import java.util.List;

import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.module.loaders.ClassPathModuleLoader;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.gson.GsonJsonProviderImpl;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Version;

public final class Java8SmokeTest {
	private Java8SmokeTest() {
	}

	public static void main(String[] args) throws Exception {
		testWithJsonProvider(Jackson2JsonProviderImpl.getInstance());
		testWithJsonProvider(GsonJsonProviderImpl.getInstance());
		System.out.println("Java 8 compatibility test passed for Jackson 2, Gson, and all extension modules");
	}

	private static <JsonNode> void testWithJsonProvider(JsonProvider<JsonNode> jsonProvider) throws Exception {
		Version version = Version.valueOf("1.6");
		Environment<JsonNode> env = new Environment<>(jsonProvider, version);
		env.setModuleLoader(new ClassPathModuleLoader<JsonNode>(Java8SmokeTest.class.getClassLoader()));

		assertQuery(jsonProvider, env, "length", "[1,2]", 2);
		assertQuery(jsonProvider, env, "test(\"a.c\")", "\"abc\"", true);
		assertQuery(jsonProvider, env, "import \"jackson-jq/random\" as random; random::random | . >= 0 and . < 1", "null", true);
		assertQuery(jsonProvider, env, "import \"jackson-jq/time\" as time; 1477162342372 | time::strftime(\"yyyy-MM-dd HH:mm:ss.SSSXXX\"; \"UTC\")", "null", "2016-10-22 18:52:22.372Z");
		assertQuery(jsonProvider, env, "import \"jackson-jq/uri\" as uri; uri::uridecode", "\"%66%6f%6f\"", "foo");
		assertQuery(jsonProvider, env, "import \"jackson-jq/uuid\" as uuid; uuid::uuid5(\"6ba7b810-9dad-11d1-80b4-00c04fd430c8\")", "\"example.com\"", "cfbff0d1-9375-5685-968c-48ce8b15ae17");
	}

	private static <JsonNode> void assertQuery(JsonProvider<JsonNode> jsonProvider, Environment<JsonNode> env, String expression, String inputJson, Object expected) throws Exception {
		JsonQuery<JsonNode> query = env.compile(expression);
		JsonNode input = jsonProvider.fromStringStrict(inputJson);
		List<JsonNode> output = new ArrayList<>();
		query.apply(input, (val, path) -> output.add(val));
		if (output.size() != 1 || !output.get(0).equals(jsonProvider.valueToTree(expected)))
			throw new AssertionError("Expected [" + expected + "] but was " + output);
	}
}
