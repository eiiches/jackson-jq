package net.thisptr.jackson.jq.v2.ext.zstd;

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

public class ZstdModuleTest {
	private static final String IMPORT = "import \"jackson-jq/zstd\" as zstd; ";
	private static final String HELLO_BASE64 = "aGVsbG8=";
	private static final String HELLO_ZSTD_BASE64 = "KLUv/QRYKQAAaGVsbG+jbZ+I";
	private static final JsonProvider<JsonNode> BINARY_PROVIDER = Jackson2JsonProvider.getInstance();
	private static final JsonProvider<JsonElement> BASE64_PROVIDER = GsonJsonProvider.getInstance();

	@Test
	public void exposesFunctions() {
		assertThat(new ModuleImpl().getFunctions().keySet()).containsExactlyInAnyOrder(
				FunctionSignature.of("compress_binary", 0),
				FunctionSignature.of("compress_text", 0),
				FunctionSignature.of("compress_text", 1),
				FunctionSignature.of("decompress_binary", 0),
				FunctionSignature.of("decompress_text", 0),
				FunctionSignature.of("decompress_text", 1));
	}

	@Test
	public void roundTripsBinaryValues() {
		byte[] raw = { 0, (byte) 0xff, 127 };
		assertThat(bytes(BINARY_PROVIDER, "zstd::compress_binary | zstd::decompress_binary", BINARY_PROVIDER.createBinary(raw))).containsExactly(raw);
		assertThat(bytes(BINARY_PROVIDER, "zstd::compress_binary | zstd::decompress_binary", BINARY_PROVIDER.createString(HELLO_BASE64)))
				.containsExactly("hello".getBytes(StandardCharsets.UTF_8));
		assertThat(bytes(BINARY_PROVIDER, "zstd::compress_binary | zstd::decompress_binary", BINARY_PROVIDER.createBinary(new byte[0]))).isEmpty();
	}

	@Test
	public void roundTripsText() {
		assertThat(text(BINARY_PROVIDER, "zstd::compress_text | zstd::decompress_text", BINARY_PROVIDER.createString("hello"))).isEqualTo("hello");
		assertThat(text(BINARY_PROVIDER, "zstd::compress_text | zstd::decompress_text", BINARY_PROVIDER.createString(""))).isEqualTo("");
		assertThat(text(BINARY_PROVIDER, "zstd::compress_text | zstd::decompress_text", BINARY_PROVIDER.createString("こんにちは"))).isEqualTo("こんにちは");
	}

	@Test
	public void convertsBetweenTextAndBinary() {
		assertThat(bytes(BINARY_PROVIDER, "zstd::compress_text | zstd::decompress_binary", BINARY_PROVIDER.createString("hello")))
				.containsExactly("hello".getBytes(StandardCharsets.UTF_8));
		assertThat(text(BINARY_PROVIDER, "zstd::compress_binary | zstd::decompress_text", BINARY_PROVIDER.createString(HELLO_BASE64))).isEqualTo("hello");
	}

	@Test
	public void decompressesStandardZstdData() {
		assertThat(text(BINARY_PROVIDER, "zstd::decompress_text", BINARY_PROVIDER.createString(HELLO_ZSTD_BASE64))).isEqualTo("hello");
		assertThat(bytes(BINARY_PROVIDER, "zstd::decompress_binary", BINARY_PROVIDER.createString(HELLO_ZSTD_BASE64)))
				.containsExactly("hello".getBytes(StandardCharsets.UTF_8));
	}

	@Test
	public void fallsBackToBase64WithoutBinaryNodes() {
		JsonElement compressed = one(BASE64_PROVIDER, "zstd::compress_binary", BASE64_PROVIDER.createString(HELLO_BASE64), unlimited());
		assertThat(BASE64_PROVIDER.getNodeType(compressed)).isEqualTo(JsonNodeType.STRING);

		assertThat(text(BASE64_PROVIDER, "zstd::compress_binary | zstd::decompress_binary", BASE64_PROVIDER.createString(HELLO_BASE64))).isEqualTo(HELLO_BASE64);
		assertThat(text(BASE64_PROVIDER, "zstd::compress_text | zstd::decompress_text", BASE64_PROVIDER.createString("hello"))).isEqualTo("hello");
		assertThat(text(BASE64_PROVIDER, "zstd::decompress_text", BASE64_PROVIDER.createString(HELLO_ZSTD_BASE64))).isEqualTo("hello");
	}

	@Test
	public void honoursEncodingOption() {
		String latin1 = "{encoding: \"ISO-8859-1\"}";
		assertThat(text(BINARY_PROVIDER, "zstd::compress_text(" + latin1 + ") | zstd::decompress_text(" + latin1 + ")", BINARY_PROVIDER.createString("café")))
				.isEqualTo("café");
		assertThat(bytes(BINARY_PROVIDER, "zstd::compress_text(" + latin1 + ") | zstd::decompress_binary", BINARY_PROVIDER.createString("café")))
				.containsExactly("café".getBytes(StandardCharsets.ISO_8859_1));
	}

	@Test
	public void rejectsTextThatDoesNotFitTheEncoding() {
		assertThatThrownBy(() -> run(BINARY_PROVIDER, "zstd::compress_text({encoding: \"ISO-8859-1\"})", BINARY_PROVIDER.createString("あ"), unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("zstd::compress_text failed to encode the input using ISO-8859-1");
	}

	@Test
	public void rejectsResultsThatAreNotValidText() {
		assertThatThrownBy(() -> run(BINARY_PROVIDER, "zstd::compress_text({encoding: \"ISO-8859-1\"}) | zstd::decompress_text", BINARY_PROVIDER.createString("café"), unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("zstd::decompress_text failed to decode the result using UTF-8");
	}

	@Test
	public void enforcesOutputLimits() {
		RuntimeOptions binaryLimit = RuntimeOptions.newBuilder().setMaxBinaryLength(4).build();
		assertThatThrownBy(() -> run(BINARY_PROVIDER, "zstd::decompress_binary", BINARY_PROVIDER.createString(HELLO_ZSTD_BASE64), binaryLimit))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("maximum binary length of 4");

		assertThatThrownBy(() -> run(BASE64_PROVIDER, "zstd::decompress_binary", BASE64_PROVIDER.createString(HELLO_ZSTD_BASE64), RuntimeOptions.newBuilder().setMaxStringLength(7).build()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("maximum string length of 7");
		assertThat(run(BASE64_PROVIDER, "zstd::decompress_binary", BASE64_PROVIDER.createString(HELLO_ZSTD_BASE64), RuntimeOptions.newBuilder().setMaxStringLength(8).build()))
				.extracting(BASE64_PROVIDER::getString)
				.containsExactly(HELLO_BASE64);

		assertThatThrownBy(() -> run(BINARY_PROVIDER, "zstd::decompress_text", BINARY_PROVIDER.createString(HELLO_ZSTD_BASE64), RuntimeOptions.newBuilder().setMaxStringLength(4).build()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("maximum string length of 4");
		assertThat(run(BINARY_PROVIDER, "zstd::decompress_text", BINARY_PROVIDER.createString(HELLO_ZSTD_BASE64), RuntimeOptions.newBuilder().setMaxStringLength(5).build()))
				.extracting(BINARY_PROVIDER::getString)
				.containsExactly("hello");
	}

	@Test
	public void rejectsInvalidInputs() {
		assertThatThrownBy(() -> run(BINARY_PROVIDER, "zstd::compress_binary", BINARY_PROVIDER.createNumber(1), unlimited()))
				.hasMessageContaining("zstd::compress_binary requires binary or Base64 string input, but got NUMBER");
		assertThatThrownBy(() -> run(BINARY_PROVIDER, "zstd::compress_binary", BINARY_PROVIDER.createString("%%%"), unlimited()))
				.hasMessageContaining("zstd::compress_binary input must be valid Base64");
		assertThatThrownBy(() -> run(BINARY_PROVIDER, "zstd::compress_text", BINARY_PROVIDER.createBinary(new byte[] { 1 }), unlimited()))
				.hasMessageContaining("zstd::compress_text requires string input, but got BINARY");
		assertThatThrownBy(() -> run(BINARY_PROVIDER, "zstd::decompress_binary", BINARY_PROVIDER.createString(HELLO_BASE64), unlimited()))
				.hasMessageContaining("zstd::decompress_binary failed");
	}

	@Test
	public void rejectsInvalidOptions() {
		assertThatThrownBy(() -> run(BINARY_PROVIDER, "zstd::compress_text(\"UTF-8\")", BINARY_PROVIDER.createString("hello"), unlimited()))
				.hasMessageContaining("zstd::compress_text options must be an object, but got STRING");
		assertThatThrownBy(() -> run(BINARY_PROVIDER, "zstd::compress_text({charset: \"UTF-8\"})", BINARY_PROVIDER.createString("hello"), unlimited()))
				.hasMessageContaining("zstd::compress_text options contains unknown member: charset");
		assertThatThrownBy(() -> run(BINARY_PROVIDER, "zstd::compress_text({encoding: 1})", BINARY_PROVIDER.createString("hello"), unlimited()))
				.hasMessageContaining("zstd::compress_text encoding must be a string");
		assertThatThrownBy(() -> run(BINARY_PROVIDER, "zstd::decompress_text({encoding: \"NO-SUCH-CHARSET\"})", BINARY_PROVIDER.createString(HELLO_ZSTD_BASE64), unlimited()))
				.hasMessageContaining("zstd::decompress_text unsupported encoding: NO-SUCH-CHARSET");
	}

	private static RuntimeOptions unlimited() {
		return RuntimeOptions.newBuilder().build();
	}

	private static <JsonNode> JsonNode one(JsonProvider<JsonNode> jsonProvider, String expression, JsonNode input) {
		return one(jsonProvider, expression, input, unlimited());
	}

	private static <JsonNode> JsonNode one(JsonProvider<JsonNode> jsonProvider, String expression, JsonNode input, RuntimeOptions options) {
		List<JsonNode> results = run(jsonProvider, expression, input, options);
		assertThat(results).hasSize(1);
		return results.get(0);
	}

	private static <JsonNode> byte[] bytes(JsonProvider<JsonNode> jsonProvider, String expression, JsonNode input) {
		return jsonProvider.getBinaryAsByteArray(one(jsonProvider, expression, input));
	}

	private static <JsonNode> String text(JsonProvider<JsonNode> jsonProvider, String expression, JsonNode input) {
		return jsonProvider.getString(one(jsonProvider, expression, input));
	}

	private static <JsonNode> List<JsonNode> run(JsonProvider<JsonNode> jsonProvider, String expression, JsonNode input, RuntimeOptions options) {
		Environment<JsonNode> environment = EnvironmentBuilder.withDefaultLoaders(jsonProvider, Versions.JQ_1_8_2).build();
		JsonQuery<JsonNode> query = environment.compile(IMPORT + expression).withRuntimeOptions(options);
		List<JsonNode> results = new ArrayList<>();
		query.apply(input, results::add);
		return results;
	}
}
