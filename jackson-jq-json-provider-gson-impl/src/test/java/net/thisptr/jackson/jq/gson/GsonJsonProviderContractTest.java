package net.thisptr.jackson.jq.gson;

import com.google.gson.JsonElement;

import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.JsonProviderContractTest;

/**
 * Contract test for {@link GsonJsonProviderImpl}.
 */
public class GsonJsonProviderContractTest extends JsonProviderContractTest<JsonElement> {

	@Override
	protected JsonProvider<JsonElement> createProvider() {
		return GsonJsonProviderImpl.getInstance();
	}
}
