package net.thisptr.jackson.jq.v2.smoketests.osgi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import net.thisptr.jackson.jq.v2.core.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.module.loaders.ClassPathModuleLoader;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.gson.GsonJsonProviderImpl;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.json.impl.jackson3.Jackson3JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class JacksonJqBundleTest {
	@Test
	void jackson2ProviderExecutesInsideOsgi() throws Exception {
		testWithJsonProvider(Jackson2JsonProviderImpl.getInstance());
	}

	@Test
	void jackson3ProviderExecutesInsideOsgi() throws Exception {
		testWithJsonProvider(Jackson3JsonProviderImpl.getInstance());
	}

	@Test
	void gsonProviderExecutesInsideOsgi() throws Exception {
		testWithJsonProvider(GsonJsonProviderImpl.getInstance());
	}

	private <JsonNode> void testWithJsonProvider(JsonProvider<JsonNode> jsonProvider) throws Exception {
		Bundle bundle = FrameworkUtil.getBundle(getClass());
		assertNotNull(bundle, "The test must be loaded from an OSGi bundle");
		assertEquals(Bundle.ACTIVE, bundle.getState());

		Version version = Version.valueOf("1.6");
		Scope<JsonNode> scope = Scope.newEmptyScope(jsonProvider);
		BuiltinFunctionLoader.getInstance().loadFunctions(version, scope);
		scope.setModuleLoader(new ClassPathModuleLoader<JsonNode>(getClass().getClassLoader()));

		assertQuery(jsonProvider, scope, version, "length", "[1,2]", 2);
		assertQuery(jsonProvider, scope, version, "test(\"a.c\")", "\"abc\"", true);
		assertQuery(jsonProvider, scope, version, "import \"jackson-jq/random\" as random; random::random | . >= 0 and . < 1", "null", true);
		assertQuery(jsonProvider, scope, version, "import \"jackson-jq/time\" as time; 1477162342372 | time::strftime(\"yyyy-MM-dd HH:mm:ss.SSSXXX\"; \"UTC\")", "null", "2016-10-22 18:52:22.372Z");
		assertQuery(jsonProvider, scope, version, "import \"jackson-jq/uri\" as uri; uri::uridecode", "\"%66%6f%6f\"", "foo");
		assertQuery(jsonProvider, scope, version, "import \"jackson-jq/uuid\" as uuid; uuid::uuid5(\"6ba7b810-9dad-11d1-80b4-00c04fd430c8\")", "\"example.com\"", "cfbff0d1-9375-5685-968c-48ce8b15ae17");
	}

	private static <JsonNode> void assertQuery(JsonProvider<JsonNode> jsonProvider, Scope<JsonNode> scope, Version version, String expression, String inputJson, Object expected) throws Exception {
		JsonQuery query = JsonQuery.compile(expression, version);
		JsonNode input = jsonProvider.fromStringStrict(inputJson);
		List<JsonNode> output = new ArrayList<>();
		query.apply(scope, input, output::add);
		assertEquals(java.util.Collections.singletonList(jsonProvider.valueToTree(expected)), output);
	}
}
