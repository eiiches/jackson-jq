package net.thisptr.jackson.jq.v2.test.providers.jackson3;

import tools.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson3.Jackson3JsonProviderImpl;
import net.thisptr.jackson.jq.v2.test.AbstractJsonQueryTest;

/**
 * Concrete implementation of AbstractJsonQueryTest for Jackson 3.
 * Runs the standard jq test suite using Jackson3JsonProviderImpl.
 */
public class Jackson3JsonQueryTest extends AbstractJsonQueryTest<JsonNode> {
	public static void main(String[] args) throws Exception {
		new Jackson3JsonQueryTest().run(args);
	}

	@Override
	protected JsonProvider<JsonNode> getJsonProvider() {
		return Jackson3JsonProviderImpl.getInstance();
	}

	@Override
	protected JsonNode parseTestNode(com.fasterxml.jackson.databind.JsonNode node) {
		// Convert Jackson 2 JsonNode (from test data) to Jackson 3 JsonNode via JSON string
		return Jackson3JsonProviderImpl.getInstance().parse(node.toString());
	}
}
