package net.thisptr.jackson.jq.v2.json.impl.jackson3;

import tools.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.JsonProviderContractTest;

/**
 * Contract test for {@link Jackson3JsonProviderImpl}.
 */
public class Jackson3JsonProviderContractTest implements JsonProviderContractTest<JsonNode> {

	@Override
	public JsonProvider<JsonNode> getProvider() {
		return Jackson3JsonProviderImpl.getInstance();
	}
}
