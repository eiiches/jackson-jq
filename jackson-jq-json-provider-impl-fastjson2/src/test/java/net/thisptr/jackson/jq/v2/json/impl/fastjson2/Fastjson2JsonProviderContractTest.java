package net.thisptr.jackson.jq.v2.json.impl.fastjson2;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.json.JsonException;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.JsonProviderContractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract tests for {@link Fastjson2JsonProviderImpl}.
 */
public class Fastjson2JsonProviderContractTest implements JsonProviderContractTest<Object> {
	@Override
	public JsonProvider<Object> getProvider() {
		return Fastjson2JsonProviderImpl.getInstance();
	}

	@Test
	void testCreateNullUsesJavaNull() {
		assertThat(getProvider().createNull()).isNull();
	}

	/**
	 * Fastjson2 deliberately accepts a missing object value such as {@code {"a":}} and omits the member.
	 * Retain its native parser behavior while checking the malformed forms that it does reject.
	 */
	@Override
	@Test
	public void testCreateParserOnMalformedInput() {
		assertThatThrownBy(() -> parseStream("{,,,"))
				.isInstanceOf(JsonException.class);
		assertThatThrownBy(() -> parseStream("[1,2"))
				.isInstanceOf(JsonException.class);
		assertThatThrownBy(() -> parseStream("{\"a\":1"))
				.isInstanceOf(JsonException.class);
		assertThat(getProvider().hasObjectMember(getProvider().parse("{\"a\":}"), "a")).isFalse();
	}
}
