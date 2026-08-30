package net.thisptr.jackson.jq.v2.gson;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.gson.GsonJsonProviderImpl;
import net.thisptr.jackson.jq.v2.test.AbstractJsonQueryTest;

/**
 * Concrete implementation of AbstractJsonQueryTest for Gson.
 * Runs the standard jq test suite using GsonJsonProviderImpl.
 */
public class GsonJsonQueryTest extends AbstractJsonQueryTest<JsonElement> {

	@Override
	protected JsonProvider<JsonElement> getJsonProvider() {
		return GsonJsonProviderImpl.getInstance();
	}

	@Override
	protected JsonElement parseTestNode(JsonNode node) {
		// Convert Jackson JsonNode to Gson JsonElement via JSON string
		return JsonParser.parseString(node.toString());
	}
}
