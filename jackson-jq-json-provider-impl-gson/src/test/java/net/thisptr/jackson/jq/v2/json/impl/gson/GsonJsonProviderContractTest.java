package net.thisptr.jackson.jq.v2.json.impl.gson;

import com.google.gson.JsonElement;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.JsonProviderContractTest;

/**
 * Contract test for {@link GsonJsonProviderImpl}.
 */
public class GsonJsonProviderContractTest implements JsonProviderContractTest<JsonElement> {

	@Override
	public JsonProvider<JsonElement> getProvider() {
		return GsonJsonProviderImpl.getInstance();
	}
}
