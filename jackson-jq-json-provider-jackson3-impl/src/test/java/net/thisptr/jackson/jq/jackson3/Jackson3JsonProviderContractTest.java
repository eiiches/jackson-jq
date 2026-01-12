package net.thisptr.jackson.jq.jackson3;

import tools.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.JsonProviderContractTest;

/**
 * Contract test for {@link Jackson3JsonProviderImpl}.
 */
public class Jackson3JsonProviderContractTest extends JsonProviderContractTest<JsonNode> {

	@Override
	protected JsonProvider<JsonNode> createProvider() {
		return Jackson3JsonProviderImpl.getInstance();
	}
}
