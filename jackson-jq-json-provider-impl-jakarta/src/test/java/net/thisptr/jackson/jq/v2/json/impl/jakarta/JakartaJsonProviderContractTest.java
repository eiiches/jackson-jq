package net.thisptr.jackson.jq.v2.json.impl.jakarta;

import jakarta.json.JsonValue;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.JsonProviderContractTest;

/**
 * Contract tests for {@link JakartaJsonProviderImpl}.
 */
public class JakartaJsonProviderContractTest extends JsonProviderContractTest<JsonValue> {

	@Override
	protected JsonProvider<JsonValue> createProvider() {
		return JakartaJsonProviderImpl.getInstance();
	}
}
