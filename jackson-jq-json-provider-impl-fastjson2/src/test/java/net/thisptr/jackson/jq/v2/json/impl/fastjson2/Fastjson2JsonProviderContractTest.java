package net.thisptr.jackson.jq.v2.json.impl.fastjson2;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.json.JsonException;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.JsonProviderContractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract tests for {@link Fastjson2JsonProvider}.
 */
public class Fastjson2JsonProviderContractTest implements JsonProviderContractTest<Object> {
	@Override
	public JsonProvider<Object> getProvider() {
		return Fastjson2JsonProvider.getInstance();
	}

	@Test
	void testCreateNullUsesJavaNull() {
		assertThat(getProvider().createNull()).isNull();
	}

	@Test
	void testFormatBinaryAsBase64() {
		Object binary = getProvider().createBinary(new byte[] { 0, 1, 127, -128 });

		assertThat(getProvider().format(binary)).isEqualTo("\"AAF/gA==\"");
		assertThat(getProvider().format(getProvider().createObject(Collections.singletonMap("binary", binary))))
				.isEqualTo("{\"binary\":\"AAF/gA==\"}");
	}

	@Test
	void testDeepCopyClonesBinaryValue() {
		byte[] bytes = { 1, 2, 3 };
		byte[] topLevelCopy = (byte[]) getProvider().deepCopy(bytes);
		Object original = getProvider().createObject(
				Collections.singletonMap("binary", getProvider().createBinary(bytes)));

		Object copy = getProvider().deepCopy(original);
		byte[] copiedBytes = getProvider().getBinaryAsByteArray(
				getProvider().getObjectMemberOrThrow(copy, "binary"));

		assertThat(topLevelCopy).isNotSameAs(bytes).containsExactly(bytes);
		assertThat(copiedBytes).isNotSameAs(bytes).containsExactly(bytes);
		copiedBytes[0] = 99;
		assertThat(getProvider().getBinaryAsByteArray(
				getProvider().getObjectMemberOrThrow(original, "binary"))).containsExactly(1, 2, 3);
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
