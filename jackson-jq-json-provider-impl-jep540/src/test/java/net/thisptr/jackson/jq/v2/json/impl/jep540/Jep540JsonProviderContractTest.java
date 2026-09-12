package net.thisptr.jackson.jq.v2.json.impl.jep540;

import jdk.incubator.json.JsonValue;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.JsonProviderContractTest;

/**
 * Contract tests for {@link Jep540JsonProviderImpl}.
 */
public class Jep540JsonProviderContractTest implements JsonProviderContractTest<JsonValue> {
	@Override
	public JsonProvider<JsonValue> getProvider() {
		return Jep540JsonProviderImpl.getInstance();
	}
}
