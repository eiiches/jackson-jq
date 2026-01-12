package net.thisptr.jackson.jq.jackson2;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.JsonProviderContractTest;

/**
 * Contract test for {@link Jackson2JsonProviderImpl}.
 */
public class Jackson2JsonProviderContractTest extends JsonProviderContractTest<JsonNode> {

	@Override
	protected JsonProvider<JsonNode> createProvider() {
		return Jackson2JsonProviderImpl.getInstance();
	}
}
