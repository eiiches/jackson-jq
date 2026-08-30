package net.thisptr.jackson.jq.v2.jakarta;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.json.JsonValue;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jakarta.JakartaJsonProviderImpl;
import net.thisptr.jackson.jq.v2.test.AbstractJsonQueryTest;

/**
 * Runs the standard jq test suite with Jakarta JSON Processing.
 */
public class JakartaJsonQueryTest extends AbstractJsonQueryTest<JsonValue> {

	@Override
	protected JsonProvider<JsonValue> getJsonProvider() {
		return JakartaJsonProviderImpl.getInstance();
	}

	@Override
	protected JsonValue parseTestNode(JsonNode node) {
		return JakartaJsonProviderImpl.getInstance().parse(node.toString());
	}
}
