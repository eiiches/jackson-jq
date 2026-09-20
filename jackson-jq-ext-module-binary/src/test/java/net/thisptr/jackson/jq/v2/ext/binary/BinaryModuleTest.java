package net.thisptr.jackson.jq.v2.ext.binary;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonElement;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.RuntimeOptions;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.gson.GsonJsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BinaryModuleTest {
	private static final String IMPORT = "import \"jackson-jq/binary\" as binary; ";
	private static final String HELLO_BASE64 = "aGVsbG8=";
	private static final JsonProvider<JsonNode> BINARY_PROVIDER = Jackson2JsonProvider.getInstance();
	private static final JsonProvider<JsonElement> BASE64_PROVIDER = GsonJsonProvider.getInstance();

	@Test
	public void exposesFunctions() {
		assertThat(new ModuleImpl().getFunctions().keySet()).containsExactlyInAnyOrder(
				FunctionSignature.of("decode_text", 0),
				FunctionSignature.of("decode_text", 1),
				FunctionSignature.of("encode_text", 0),
				FunctionSignature.of("encode_text", 1));
	}

	@Test
	public void roundTripsText() {
		assertThat(text(BINARY_PROVIDER, "binary::encode_text | binary::decode_text", BINARY_PROVIDER.createString("hello"))).isEqualTo("hello");
		assertThat(text(BINARY_PROVIDER, "binary::encode_text | binary::decode_text", BINARY_PROVIDER.createString(""))).isEqualTo("");
		assertThat(text(BINARY_PROVIDER, "binary::encode_text | binary::decode_text", BINARY_PROVIDER.createString("こんにちは"))).isEqualTo("こんにちは");
	}

	@Test
	public void supportsSurrogatePairs() {
		String supplementary = "😀𠮷𝄞";
		assertThat(text(BINARY_PROVIDER, "binary::encode_text | binary::decode_text", BINARY_PROVIDER.createString(supplementary))).isEqualTo(supplementary);
		assertThat(text(BINARY_PROVIDER, "binary::encode_text({encoding: \"UTF-16BE\"}) | binary::decode_text({encoding: \"UTF-16BE\"})", BINARY_PROVIDER.createString(supplementary)))
				.isEqualTo(supplementary);

		// Test surrogate pair crossing the 8192-character buffer boundary in decodeText
		String str = "a".repeat(8191) + supplementary + "b".repeat(100);
		assertThat(text(BINARY_PROVIDER, "binary::encode_text | binary::decode_text", BINARY_PROVIDER.createString(str))).isEqualTo(str);
	}

	@Test
	public void rejectsUnpairedSurrogates() {
		assertThatThrownBy(() -> run(BINARY_PROVIDER, "binary::encode_text", BINARY_PROVIDER.createString("\uD800"), unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("binary::encode_text failed to encode the input using UTF-8");

		assertThatThrownBy(() -> run(BINARY_PROVIDER, "binary::encode_text", BINARY_PROVIDER.createString("\uDC00"), unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("binary::encode_text failed to encode the input using UTF-8");

		assertThatThrownBy(() -> run(BINARY_PROVIDER, "binary::encode_text", BINARY_PROVIDER.createString("\uDC00\uD800"), unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("binary::encode_text failed to encode the input using UTF-8");
	}

	@Test
	public void encodesToBinaryNodesWhenSupported() {
		JsonNode encoded = one(BINARY_PROVIDER, "binary::encode_text", BINARY_PROVIDER.createString("hello"), unlimited());
		assertThat(BINARY_PROVIDER.getNodeType(encoded)).isEqualTo(JsonNodeType.BINARY);
		assertThat(BINARY_PROVIDER.getBinaryAsByteArray(encoded)).containsExactly("hello".getBytes(StandardCharsets.UTF_8));
	}

	@Test
	public void decodesBothBinaryAndBase64String() {
		byte[] raw = "hello".getBytes(StandardCharsets.UTF_8);
		assertThat(text(BINARY_PROVIDER, "binary::decode_text", BINARY_PROVIDER.createBinary(raw))).isEqualTo("hello");
		assertThat(text(BINARY_PROVIDER, "binary::decode_text", BINARY_PROVIDER.createString(HELLO_BASE64))).isEqualTo("hello");
		assertThat(text(BINARY_PROVIDER, "binary::decode_text", BINARY_PROVIDER.createBinary(new byte[0]))).isEqualTo("");
		assertThat(text(BINARY_PROVIDER, "binary::decode_text", BINARY_PROVIDER.createString(""))).isEqualTo("");
	}

	@Test
	public void fallsBackToBase64WithoutBinaryNodes() {
		JsonElement encoded = one(BASE64_PROVIDER, "binary::encode_text", BASE64_PROVIDER.createString("hello"), unlimited());
		assertThat(BASE64_PROVIDER.getNodeType(encoded)).isEqualTo(JsonNodeType.STRING);
		assertThat(BASE64_PROVIDER.getString(encoded)).isEqualTo(HELLO_BASE64);

		assertThat(text(BASE64_PROVIDER, "binary::encode_text | binary::decode_text", BASE64_PROVIDER.createString("hello"))).isEqualTo("hello");
		assertThat(text(BASE64_PROVIDER, "binary::decode_text", BASE64_PROVIDER.createString(HELLO_BASE64))).isEqualTo("hello");
	}

	@Test
	public void honoursEncodingOption() {
		String latin1 = "{encoding: \"ISO-8859-1\"}";
		assertThat(text(BINARY_PROVIDER, "binary::encode_text(" + latin1 + ") | binary::decode_text(" + latin1 + ")", BINARY_PROVIDER.createString("café")))
				.isEqualTo("café");

		JsonNode encoded = one(BINARY_PROVIDER, "binary::encode_text(" + latin1 + ")", BINARY_PROVIDER.createString("café"), unlimited());
		assertThat(BINARY_PROVIDER.getBinaryAsByteArray(encoded)).containsExactly("café".getBytes(StandardCharsets.ISO_8859_1));

		String utf16be = "{encoding: \"UTF-16BE\"}";
		assertThat(text(BINARY_PROVIDER, "binary::encode_text(" + utf16be + ") | binary::decode_text(" + utf16be + ")", BINARY_PROVIDER.createString("test")))
				.isEqualTo("test");
	}

	@Test
	public void rejectsTextThatDoesNotFitTheEncoding() {
		assertThatThrownBy(() -> run(BINARY_PROVIDER, "binary::encode_text({encoding: \"ISO-8859-1\"})", BINARY_PROVIDER.createString("あ"), unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("binary::encode_text failed to encode the input using ISO-8859-1");
	}

	@Test
	public void rejectsBytesThatAreNotValidForEncoding() {
		byte[] invalidUtf8 = new byte[] { (byte) 0xff, (byte) 0xfe };
		assertThatThrownBy(() -> run(BINARY_PROVIDER, "binary::decode_text", BINARY_PROVIDER.createBinary(invalidUtf8), unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("binary::decode_text failed to decode the input using UTF-8");
	}

	@Test
	public void enforcesOutputLimits() {
		RuntimeOptions binaryLimit = RuntimeOptions.newBuilder().setMaxBinaryLength(4).build();
		assertThatThrownBy(() -> run(BINARY_PROVIDER, "binary::encode_text", BINARY_PROVIDER.createString("hello"), binaryLimit))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("maximum binary length of 4");
		assertThat(run(BINARY_PROVIDER, "binary::encode_text", BINARY_PROVIDER.createString("test"), binaryLimit))
				.hasSize(1);

		RuntimeOptions stringLimit7 = RuntimeOptions.newBuilder().setMaxStringLength(7).build();
		assertThatThrownBy(() -> run(BASE64_PROVIDER, "binary::encode_text", BASE64_PROVIDER.createString("hello"), stringLimit7))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("maximum string length of 7");
		assertThat(run(BASE64_PROVIDER, "binary::encode_text", BASE64_PROVIDER.createString("hello"), RuntimeOptions.newBuilder().setMaxStringLength(8).build()))
				.extracting(BASE64_PROVIDER::getString)
				.containsExactly(HELLO_BASE64);

		RuntimeOptions decodeLimit4 = RuntimeOptions.newBuilder().setMaxStringLength(4).build();
		assertThatThrownBy(() -> run(BINARY_PROVIDER, "binary::decode_text", BINARY_PROVIDER.createString(HELLO_BASE64), decodeLimit4))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("maximum string length of 4");
		assertThat(run(BINARY_PROVIDER, "binary::decode_text", BINARY_PROVIDER.createString(HELLO_BASE64), RuntimeOptions.newBuilder().setMaxStringLength(5).build()))
				.extracting(BINARY_PROVIDER::getString)
				.containsExactly("hello");
	}

	@Test
	public void rejectsInvalidInputs() {
		assertThatThrownBy(() -> run(BINARY_PROVIDER, "binary::encode_text", BINARY_PROVIDER.createNumber(1), unlimited()))
				.hasMessageContaining("binary::encode_text requires string input, but got NUMBER");
		assertThatThrownBy(() -> run(BINARY_PROVIDER, "binary::encode_text", BINARY_PROVIDER.createBinary(new byte[] { 1 }), unlimited()))
				.hasMessageContaining("binary::encode_text requires string input, but got BINARY");

		assertThatThrownBy(() -> run(BINARY_PROVIDER, "binary::decode_text", BINARY_PROVIDER.createNumber(1), unlimited()))
				.hasMessageContaining("binary::decode_text requires binary or Base64 string input, but got NUMBER");
		assertThatThrownBy(() -> run(BINARY_PROVIDER, "binary::decode_text", BINARY_PROVIDER.createString("%%%"), unlimited()))
				.hasMessageContaining("binary::decode_text input must be valid Base64");
	}

	@Test
	public void rejectsInvalidOptions() {
		assertThatThrownBy(() -> run(BINARY_PROVIDER, "binary::encode_text(\"UTF-8\")", BINARY_PROVIDER.createString("hello"), unlimited()))
				.hasMessageContaining("binary::encode_text options must be an object, but got STRING");
		assertThatThrownBy(() -> run(BINARY_PROVIDER, "binary::encode_text({charset: \"UTF-8\"})", BINARY_PROVIDER.createString("hello"), unlimited()))
				.hasMessageContaining("binary::encode_text options contains unknown member: charset");
		assertThatThrownBy(() -> run(BINARY_PROVIDER, "binary::encode_text({encoding: 1})", BINARY_PROVIDER.createString("hello"), unlimited()))
				.hasMessageContaining("binary::encode_text encoding must be a string");
		assertThatThrownBy(() -> run(BINARY_PROVIDER, "binary::encode_text({encoding: \"NO-SUCH-CHARSET\"})", BINARY_PROVIDER.createString("hello"), unlimited()))
				.hasMessageContaining("binary::encode_text unsupported encoding: NO-SUCH-CHARSET");

		assertThatThrownBy(() -> run(BINARY_PROVIDER, "binary::decode_text(\"UTF-8\")", BINARY_PROVIDER.createString(HELLO_BASE64), unlimited()))
				.hasMessageContaining("binary::decode_text options must be an object, but got STRING");
		assertThatThrownBy(() -> run(BINARY_PROVIDER, "binary::decode_text({charset: \"UTF-8\"})", BINARY_PROVIDER.createString(HELLO_BASE64), unlimited()))
				.hasMessageContaining("binary::decode_text options contains unknown member: charset");
		assertThatThrownBy(() -> run(BINARY_PROVIDER, "binary::decode_text({encoding: 1})", BINARY_PROVIDER.createString(HELLO_BASE64), unlimited()))
				.hasMessageContaining("binary::decode_text encoding must be a string");
		assertThatThrownBy(() -> run(BINARY_PROVIDER, "binary::decode_text({encoding: \"NO-SUCH-CHARSET\"})", BINARY_PROVIDER.createString(HELLO_BASE64), unlimited()))
				.hasMessageContaining("binary::decode_text unsupported encoding: NO-SUCH-CHARSET");
	}

	private static RuntimeOptions unlimited() {
		return RuntimeOptions.newBuilder().build();
	}

	private static <JsonNode> JsonNode one(JsonProvider<JsonNode> jsonProvider, String expression, JsonNode input, RuntimeOptions options) {
		List<JsonNode> results = run(jsonProvider, expression, input, options);
		assertThat(results).hasSize(1);
		return results.get(0);
	}

	private static <JsonNode> String text(JsonProvider<JsonNode> jsonProvider, String expression, JsonNode input) {
		return jsonProvider.getString(one(jsonProvider, expression, input, unlimited()));
	}

	private static <JsonNode> List<JsonNode> run(JsonProvider<JsonNode> jsonProvider, String expression, JsonNode input, RuntimeOptions options) {
		Environment<JsonNode> environment = EnvironmentBuilder.withDefaultLoaders(jsonProvider, Versions.JQ_1_8_2).build();
		JsonQuery<JsonNode> query = environment.compile(IMPORT + expression).withRuntimeOptions(options);
		List<JsonNode> results = new ArrayList<>();
		query.apply(input, results::add);
		return results;
	}
}
