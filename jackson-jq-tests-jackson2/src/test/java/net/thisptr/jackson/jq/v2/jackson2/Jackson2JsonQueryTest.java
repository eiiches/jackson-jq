package net.thisptr.jackson.jq.v2.jackson2;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.test.AbstractJsonQueryTest;

/**
 * Concrete implementation of AbstractJsonQueryTest for Jackson 2.
 * Runs the standard jq test suite using Jackson2JsonProviderImpl.
 */
public class Jackson2JsonQueryTest extends AbstractJsonQueryTest<JsonNode> {

	@Override
	protected JsonProvider<JsonNode> getJsonProvider() {
		return Jackson2JsonProviderImpl.getInstance();
	}

	@Override
	protected JsonNode parseTestNode(JsonNode node) {
		// Test data is already in Jackson JsonNode format
		return node;
	}
}
