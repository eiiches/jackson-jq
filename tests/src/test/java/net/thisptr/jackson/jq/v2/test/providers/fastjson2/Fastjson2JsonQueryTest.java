package net.thisptr.jackson.jq.v2.test.providers.fastjson2;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.fastjson2.Fastjson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.test.AbstractJsonQueryTest;

/**
 * Runs the standard jq test suite using {@link Fastjson2JsonProviderImpl}.
 */
public class Fastjson2JsonQueryTest extends AbstractJsonQueryTest<Object> {
	public static void main(String[] args) throws Exception {
		new Fastjson2JsonQueryTest().run(args);
	}

	@Override
	protected JsonProvider<Object> getJsonProvider() {
		return Fastjson2JsonProviderImpl.getInstance();
	}

	@Override
	protected Object parseTestNode(JsonNode node) {
		return getJsonProvider().parse(node.toString());
	}
}
