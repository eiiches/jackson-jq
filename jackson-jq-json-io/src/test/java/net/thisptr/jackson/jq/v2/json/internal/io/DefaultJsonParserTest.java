package net.thisptr.jackson.jq.v2.json.internal.io;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.errorprone.annotations.Var;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultJsonParserTest {
	private static final JsonProvider<JsonNode> PROVIDER = Jackson2JsonProvider.getInstance();
	private static final int DEEP = 100_000;

	@Test
	void readsDeeplyNestedArrays() {
		@Var JsonNode node = JsonCodec.parse(PROVIDER, "[".repeat(DEEP) + "0" + "]".repeat(DEEP));
		for (int i = 0; i < DEEP; i++) {
			assertThat(node.isArray()).isTrue();
			assertThat(node.size()).isEqualTo(1);
			node = node.get(0);
		}
		assertThat(node.decimalValue()).isEqualByComparingTo("0");
	}

	@Test
	void readsDeeplyNestedObjects() {
		@Var JsonNode node = JsonCodec.parse(PROVIDER, "{\"a\":".repeat(DEEP) + "true" + "}".repeat(DEEP));
		for (int i = 0; i < DEEP; i++) {
			assertThat(node.isObject()).isTrue();
			assertThat(node.size()).isEqualTo(1);
			node = node.get("a");
		}
		assertThat(node.isBoolean()).isTrue();
		assertThat(node.booleanValue()).isTrue();
	}

	@Test
	void readsMixedContainersAndDuplicateKeys() {
		JsonNode node = JsonCodec.parse(PROVIDER, "{\"a\":[{}, {\"x\":1,\"x\":2}],\"b\":[]}");
		assertThat(JsonCodec.format(PROVIDER, node)).isEqualTo("{\"a\":[{},{\"x\":2}],\"b\":[]}");
	}

	@Test
	void appliesNumberOptionsInsideContainers() {
		String json = "[9007199254740993]";
		JsonNode exact = JsonCodec.parse(PROVIDER, json);
		JsonNode rounded = JsonCodec.parse(PROVIDER, json, ParseOptions.newBuilder().setRoundNumbersToDouble(true).build());
		assertThat(JsonCodec.format(PROVIDER, exact)).isEqualTo(json);
		assertThat(JsonCodec.format(PROVIDER, rounded)).isEqualTo("[9007199254740992]");
	}

	@Test
	void enforcesContainerLimitsWhileParsing() {
		ParseOptions options = ParseOptions.newBuilder().setMaxArrayLength(1).setMaxObjectMemberCount(1).build();
		assertThat(JsonCodec.format(PROVIDER, JsonCodec.parse(PROVIDER, "{\"a\":[1]}", options)))
				.isEqualTo("{\"a\":[1]}");
		assertThatThrownBy(() -> JsonCodec.parse(PROVIDER, "[1,2]", options))
				.isInstanceOf(JsonSizeExceededException.class)
				.satisfies(error -> {
					JsonSizeExceededException exceeded = (JsonSizeExceededException) error;
					assertThat(exceeded.getKind()).isEqualTo(JsonSizeExceededException.Kind.ARRAY);
					assertThat(exceeded.getSize()).isEqualTo(2L);
				})
				.hasMessageContaining("Array of 2 elements");
		assertThatThrownBy(() -> JsonCodec.parse(PROVIDER, "{\"a\":1,\"b\":2}", options))
				.isInstanceOf(JsonSizeExceededException.class)
				.satisfies(error -> {
					JsonSizeExceededException exceeded = (JsonSizeExceededException) error;
					assertThat(exceeded.getKind()).isEqualTo(JsonSizeExceededException.Kind.OBJECT);
					assertThat(exceeded.getSize()).isEqualTo(2L);
				})
				.hasMessageContaining("Object of 2 members");
		assertThat(JsonCodec.format(PROVIDER, JsonCodec.parse(PROVIDER, "{\"a\":1,\"a\":2}", options)))
				.isEqualTo("{\"a\":2}");
		assertThatThrownBy(() -> JsonCodec.parse(PROVIDER, "[1,{\"x\":bad}]", options))
				.isInstanceOf(JsonSizeExceededException.class);
		assertThatThrownBy(() -> JsonCodec.parse(PROVIDER, "{\"a\":1,\"b\":bad}", options))
				.isInstanceOf(JsonSizeExceededException.class);
	}

	@Test
	void readsOneValueAtATime() {
		byte[] input = "[1] {\"a\":2} false".getBytes(StandardCharsets.UTF_8);
		try (JsonParser<JsonNode> parser = JsonCodec.createParser(PROVIDER, new ByteArrayInputStream(input))) {
			assertThat(JsonCodec.format(PROVIDER, parser.next().get())).isEqualTo("[1]");
			assertThat(JsonCodec.format(PROVIDER, parser.next().get())).isEqualTo("{\"a\":2}");
			assertThat(JsonCodec.format(PROVIDER, parser.next().get())).isEqualTo("false");
			assertThat(parser.next().isAbsent()).isTrue();
		}
	}

	@Test
	void rejectsMalformedNestedInput() {
		for (String json : new String[] { "[1,]", "{\"a\":1,}", "{\"a\":[1}", "[1", "{\"a\":}" })
			assertThatThrownBy(() -> JsonCodec.parse(PROVIDER, json)).as(json).isInstanceOf(JsonException.class);
	}
}
