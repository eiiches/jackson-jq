package net.thisptr.jackson.jq.v2.json.impl.jakarta;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.json.JsonValue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JakartaJsonProviderImplTest {
	private final JakartaJsonProviderImpl provider = JakartaJsonProviderImpl.getInstance();

	@Test
	void updatesAreCopyOnWrite() {
		JsonValue emptyObject = provider.createObject();
		JsonValue object = provider.set(emptyObject, "value", provider.createNumber(42));
		JsonValue emptyArray = provider.createArray();
		JsonValue array = provider.add(emptyArray, provider.createString("value"));

		assertThat(provider.size(emptyObject)).isZero();
		assertThat(provider.asInt(provider.requireGet(object, "value"))).isEqualTo(42);
		assertThat(provider.size(emptyArray)).isZero();
		assertThat(provider.asText(provider.requireGet(array, 0))).isEqualTo("value");
		assertThat(provider.deepCopy(object)).isSameAs(object);
	}

	@Test
	void readsMultipleTopLevelValues() {
		List<JsonValue> values = provider.readMultipleValues("1\n{\"value\": [true, \"}\"]}[2] \"text\"");

		assertThat(values).hasSize(4);
		assertThat(provider.asInt(values.get(0))).isEqualTo(1);
		assertThat(provider.getNodeType(values.get(1))).isEqualTo(net.thisptr.jackson.jq.v2.json.JsonNodeType.OBJECT);
		assertThat(provider.getNodeType(values.get(2))).isEqualTo(net.thisptr.jackson.jq.v2.json.JsonNodeType.ARRAY);
		assertThat(provider.asText(values.get(3))).isEqualTo("text");
	}

	@Test
	void strictParsingRejectsMalformedBoundaries() {
		assertThatThrownBy(() -> provider.fromStringStrict("{}[]"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("trailing content");
		assertThatThrownBy(() -> provider.readMultipleValues("{]"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("mismatched JSON delimiters");
		assertThatThrownBy(() -> provider.readMultipleValues("\"unterminated"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("unterminated string");
	}

	@Test
	void convertsJsonLikeJavaValuesRecursively() {
		Map<String, Object> input = new LinkedHashMap<>();
		input.put("items", Arrays.asList(1, true, null));
		input.put("array", new long[] { 2, 3 });

		JsonValue result = provider.valueToTree(input);

		assertThat(provider.toString(result)).isEqualTo("{\"items\":[1,true,null],\"array\":[2,3]}");
	}

	@Test
	void rejectsUnsupportedJavaValuesAndNonStringMapKeys() {
		assertThatThrownBy(() -> provider.valueToTree(new Object())).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> provider.valueToTree(java.util.Collections.singletonMap(1, "value")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("JSON object keys must be strings");
	}
}
