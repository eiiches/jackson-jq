package net.thisptr.jackson.jq.v2.json.internal.io;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.errorprone.annotations.Var;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * What the formatter writes, and what writing it costs.
 * <p>
 * Nesting is walked with an explicit stack, so the depth cases here are the ones that would exhaust
 * the Java stack if it were walked by recursion. {@code DEEP} is far past what any plausible stack
 * holds, which is what makes them a regression test rather than a measurement of the stack the test
 * happens to run on.
 */
class DefaultJsonFormatterTest {
	private static final JsonProvider<JsonNode> PROVIDER = Jackson2JsonProvider.getInstance();

	/**
	 * Comfortably past the few thousand levels the Java stack allows, and small enough to stay quick.
	 */
	private static final int DEEP = 100_000;

	private static String format(JsonNode node) {
		return JsonCodec.format(PROVIDER, node);
	}

	private static String format(JsonNode node, int maxLength) {
		return JsonCodec.format(PROVIDER, node, FormatOptions.newBuilder().setMaxLength(maxLength).build());
	}

	/**
	 * An array nested {@code depth} levels deep, innermost first: {@code [[[…[]…]]]}.
	 */
	private static JsonNode nestedArrays(int depth) {
		@Var JsonNode node = PROVIDER.createArray(List.of());
		for (int i = 1; i < depth; i++)
			node = PROVIDER.createArray(List.of(node));
		return node;
	}

	/**
	 * An object nested {@code depth} levels deep: <code>{"a":{"a":…{}…}}</code>.
	 */
	private static JsonNode nestedObjects(int depth) {
		@Var JsonNode node = PROVIDER.createObject(Map.of());
		for (int i = 1; i < depth; i++)
			node = PROVIDER.createObject(Map.of("a", node));
		return node;
	}

	@Test
	void writesADeeplyNestedArray() {
		String text = format(nestedArrays(DEEP));
		assertThat(text).hasSize(2 * DEEP);
		assertThat(text.chars().filter(ch -> ch == '[').count()).isEqualTo(DEEP);
		assertThat(text).startsWith("[[[").endsWith("]]]");
	}

	@Test
	void writesADeeplyNestedObject() {
		String text = format(nestedObjects(DEEP));
		// The innermost level writes {}; every level outside it writes {"a": and a closing }.
		assertThat(text).hasSize(2 + 6 * (DEEP - 1));
		assertThat(text).startsWith("{\"a\":{\"a\":").endsWith("}}}");
		assertThat(text).contains("{\"a\":{}}");
	}

	@Test
	void writesScalarsAndContainersAsJqDoes() {
		assertThat(format(PROVIDER.createNull())).isEqualTo("null");
		assertThat(format(PROVIDER.createBoolean(true))).isEqualTo("true");
		assertThat(format(PROVIDER.createBoolean(false))).isEqualTo("false");
		assertThat(format(PROVIDER.createArray(List.of()))).isEqualTo("[]");
		assertThat(format(PROVIDER.createObject(Map.of()))).isEqualTo("{}");
		assertThat(format(JsonCodec.parse(PROVIDER, "{\"a\":[1,2.5,true,null,{}],\"b\":[]}")))
				.isEqualTo("{\"a\":[1,2.5,true,null,{}],\"b\":[]}");
	}

	@Test
	void escapesTheCharactersJsonCannotHold() {
		assertThat(format(PROVIDER.createString("\"\\\b\f\n\r\t")))
				.isEqualTo("\"\\\"\\\\\\b\\f\\n\\r\\t\"");
		String controls = "" + (char) 0x01 + (char) 0x1f + " x";
		assertThat(format(PROVIDER.createString(controls)))
				.isEqualTo("\"\\u0001\\u001f x\"");
	}

	@Test
	void stopsAsSoonAsTheOutputWouldPassTheCap() {
		JsonNode node = JsonCodec.parse(PROVIDER, "[1,2,3]");
		assertThatCode(() -> format(node, 7)).doesNotThrowAnyException();
		assertThatThrownBy(() -> format(node, 6))
				.isInstanceOf(JsonSizeExceededException.class)
				.satisfies(error -> {
					JsonSizeExceededException exceeded = (JsonSizeExceededException) error;
					assertThat(exceeded.getKind()).isEqualTo(JsonSizeExceededException.Kind.STRING);
					assertThat(exceeded.getSize()).isEqualTo(7L);
				})
				.hasMessageContaining("exceeds the maximum length of 6");
	}

	@Test
	void capsTheWorkADeepValueCanCost() {
		// Giving up while writing, rather than measuring afterwards, is what keeps the 200,000-char
		// output of this value from being built at all -- and bounds the frame stack with it, since
		// every level writes at least one bracket.
		assertThatThrownBy(() -> format(nestedArrays(DEEP), 100))
				.isInstanceOf(JsonSizeExceededException.class)
				.hasMessageContaining("JSON of 101 characters");
	}

	@Test
	void countsTheCapInUtf16CodeUnits() {
		// An astral character is one codepoint but two chars, and is written unescaped.
		JsonNode node = PROVIDER.createString(new String(Character.toChars(0x1F600)));
		assertThat(format(node)).hasSize(4);
		assertThatCode(() -> format(node, 4)).doesNotThrowAnyException();
		assertThatThrownBy(() -> format(node, 3)).isInstanceOf(JsonSizeExceededException.class)
				.satisfies(error -> {
					JsonSizeExceededException exceeded = (JsonSizeExceededException) error;
					assertThat(exceeded.getKind()).isEqualTo(JsonSizeExceededException.Kind.STRING);
					assertThat(exceeded.getSize()).isEqualTo(4L);
				});
	}
}
