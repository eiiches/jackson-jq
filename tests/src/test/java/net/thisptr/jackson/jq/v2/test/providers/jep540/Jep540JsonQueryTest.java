package net.thisptr.jackson.jq.v2.test.providers.jep540;

import com.fasterxml.jackson.databind.JsonNode;
import jdk.incubator.json.JsonValue;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jep540.Jep540JsonProviderImpl;
import net.thisptr.jackson.jq.v2.test.AbstractJsonQueryTest;

/**
 * Runs the standard jq test suite with the JEP 540 JSON API.
 */
public class Jep540JsonQueryTest extends AbstractJsonQueryTest<JsonValue> {
	public static void main(String[] args) throws Exception {
		new Jep540JsonQueryTest().run(args);
	}

	@Override
	protected JsonProvider<JsonValue> getJsonProvider() {
		return Jep540JsonProviderImpl.getInstance();
	}

	@Override
	protected JsonValue parseTestNode(JsonNode node) {
		return Jep540JsonProviderImpl.getInstance().parse(node.toString());
	}
}
