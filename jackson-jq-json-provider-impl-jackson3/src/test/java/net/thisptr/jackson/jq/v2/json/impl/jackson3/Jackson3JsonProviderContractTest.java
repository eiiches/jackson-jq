package net.thisptr.jackson.jq.v2.json.impl.jackson3;

import tools.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.JsonProviderContractTest;

/**
 * Contract test for {@link Jackson3JsonProviderImpl}.
 */
public class Jackson3JsonProviderContractTest extends JsonProviderContractTest<JsonNode> {

	@Override
	protected JsonProvider<JsonNode> createProvider() {
		return Jackson3JsonProviderImpl.getInstance();
	}
}
