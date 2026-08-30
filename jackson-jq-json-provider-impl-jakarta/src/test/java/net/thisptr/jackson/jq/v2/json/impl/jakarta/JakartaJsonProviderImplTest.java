package net.thisptr.jackson.jq.v2.json.impl.jakarta;

import java.util.Collections;
import java.util.List;

import jakarta.json.JsonValue;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.json.JsonException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JakartaJsonProviderImplTest {
	private final JakartaJsonProviderImpl provider = JakartaJsonProviderImpl.getInstance();

	@Test
	void deepCopyIsNoOpForImmutableValues() {
		JsonValue object = provider.createObject(Collections.singletonMap("value", provider.createNumber(42)));
		assertThat(provider.deepCopy(object)).isSameAs(object);
	}

	@Test
	void readsMultipleTopLevelValues() {
		List<JsonValue> values = provider.parseAll("1\n{\"value\": [true, \"}\"]}[2] \"text\"");

		assertThat(values).hasSize(4);
		assertThat(provider.asInt(values.get(0))).isEqualTo(1);
		assertThat(provider.getNodeType(values.get(1))).isEqualTo(net.thisptr.jackson.jq.v2.json.JsonNodeType.OBJECT);
		assertThat(provider.getNodeType(values.get(2))).isEqualTo(net.thisptr.jackson.jq.v2.json.JsonNodeType.ARRAY);
		assertThat(provider.asString(values.get(3))).isEqualTo("text");
	}

	@Test
	void strictParsingRejectsMalformedBoundaries() {
		assertThatThrownBy(() -> provider.parse("{}[]"))
				.isInstanceOf(JsonException.class)
				.hasMessage("trailing content");
		// The underlying JSON-P parser reports these, so only the rejection is asserted, not the wording.
		assertThatThrownBy(() -> provider.parseAll("{]"))
				.isInstanceOf(JsonException.class);
		assertThatThrownBy(() -> provider.parseAll("\"unterminated"))
				.isInstanceOf(JsonException.class);
	}
}
