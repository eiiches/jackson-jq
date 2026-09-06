package net.thisptr.jackson.jq.v2.json.impl.jackson2;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.JsonProviderContractTest;

/**
 * Contract test for {@link Jackson2JsonProviderImpl}.
 */
public class Jackson2JsonProviderContractTest implements JsonProviderContractTest<JsonNode> {

	@Override
	public JsonProvider<JsonNode> getProvider() {
		return Jackson2JsonProviderImpl.getInstance();
	}
}
