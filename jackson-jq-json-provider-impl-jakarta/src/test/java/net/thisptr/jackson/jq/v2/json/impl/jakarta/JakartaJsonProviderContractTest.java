package net.thisptr.jackson.jq.v2.json.impl.jakarta;

import jakarta.json.JsonValue;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.JsonProviderContractTest;

/**
 * Contract tests for {@link JakartaJsonProvider}.
 */
public class JakartaJsonProviderContractTest implements JsonProviderContractTest<JsonValue> {

	@Override
	public JsonProvider<JsonValue> getProvider() {
		return JakartaJsonProvider.getInstance();
	}
}
