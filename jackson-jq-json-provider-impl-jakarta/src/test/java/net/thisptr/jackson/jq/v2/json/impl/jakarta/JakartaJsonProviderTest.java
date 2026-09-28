package net.thisptr.jackson.jq.v2.json.impl.jakarta;

import java.util.Collections;
import java.util.List;

import jakarta.json.JsonValue;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.json.internal.io.JsonCodec;
import net.thisptr.jackson.jq.v2.json.internal.io.JsonException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JakartaJsonProviderTest {
	private final JakartaJsonProvider provider = JakartaJsonProvider.getInstance();

	@Test
	void deepCopyIsNoOpForImmutableValues() {
		JsonValue object = provider.createObject(Collections.singletonMap("value", provider.createNumber(42)));
		assertThat(provider.deepCopy(object)).isSameAs(object);
	}

	@Test
	void readsMultipleTopLevelValues() {
		List<JsonValue> values = JsonCodec.parseAll(provider, """
				1
				{"value": [true, "}"]}[2] "text"\
				""");

		assertThat(values).hasSize(4);
		assertThat(provider.getNumberAsIntExact(values.get(0))).isEqualTo(1);
		assertThat(provider.getNodeType(values.get(1))).isEqualTo(net.thisptr.jackson.jq.v2.json.JsonNodeType.OBJECT);
		assertThat(provider.getNodeType(values.get(2))).isEqualTo(net.thisptr.jackson.jq.v2.json.JsonNodeType.ARRAY);
		assertThat(provider.getString(values.get(3))).isEqualTo("text");
	}

	@Test
	void strictParsingRejectsMalformedBoundaries() {
		assertThatThrownBy(() -> JsonCodec.parse(provider, "{}[]"))
				.isInstanceOf(JsonException.class)
				.hasMessage("trailing content");
		// The underlying JSON-P parser reports these, so only the rejection is asserted, not the wording.
		assertThatThrownBy(() -> JsonCodec.parseAll(provider, "{]"))
				.isInstanceOf(JsonException.class);
		assertThatThrownBy(() -> JsonCodec.parseAll(provider, "\"unterminated"))
				.isInstanceOf(JsonException.class);
	}
}
