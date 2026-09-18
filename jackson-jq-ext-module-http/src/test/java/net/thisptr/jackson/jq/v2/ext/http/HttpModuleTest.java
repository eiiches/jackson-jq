package net.thisptr.jackson.jq.v2.ext.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonElement;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.RuntimeOptions;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.gson.GsonJsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class HttpModuleTest {
	private static final String IMPORT = "import \"jackson-jq/http\" as http; ";
	private static final JsonProvider<JsonNode> JSON_PROVIDER = Jackson2JsonProvider.getInstance();
	private static String baseUrl;
	private static HttpServer server;

	@BeforeAll
	public static void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		server.createContext("/json", exchange -> {
			exchange.getResponseHeaders().add("Content-Type", "application/problem+json; charset=UTF-8");
			exchange.getResponseHeaders().add("X-Repeat", "first");
			exchange.getResponseHeaders().add("X-Repeat", "second");
			send(exchange, 200, "{\"message\":\"ok\"}".getBytes(StandardCharsets.UTF_8));
		});
		server.createContext("/text", exchange -> {
			exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=ISO-8859-1");
			send(exchange, 200, "caf\u00e9".getBytes(StandardCharsets.ISO_8859_1));
		});
		server.createContext("/binary", exchange -> {
			exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
			send(exchange, 418, new byte[] { 0, 1, (byte) 0xff });
		});
		server.createContext("/gzip", exchange -> {
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.getResponseHeaders().add("Content-Encoding", "gzip");
			send(exchange, 200, compress("{\"compressed\":true}", true));
		});
		server.createContext("/deflate", exchange -> {
			exchange.getResponseHeaders().add("Content-Type", "text/plain");
			exchange.getResponseHeaders().add("Content-Encoding", "deflate");
			send(exchange, 200, compress("deflated", false));
		});
		server.createContext("/empty", exchange -> {
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			send(exchange, 204, new byte[0]);
		});
		server.createContext("/redirect", exchange -> {
			exchange.getResponseHeaders().add("Location", "/json");
			exchange.sendResponseHeaders(302, -1);
			exchange.close();
		});
		server.createContext("/invalid-json", exchange -> {
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			send(exchange, 200, "{".getBytes(StandardCharsets.UTF_8));
		});
		server.createContext("/unsupported-encoding", exchange -> {
			exchange.getResponseHeaders().add("Content-Encoding", "br");
			send(exchange, 200, "content".getBytes(StandardCharsets.UTF_8));
		});
		server.start();
		baseUrl = "http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort();
	}

	@AfterAll
	public static void stopServer() {
		server.stop(0);
	}

	@Test
	public void exposesGetFunction() {
		assertThat(new ModuleImpl().getFunctions().keySet()).containsExactlyInAnyOrder(FunctionSignature.of("get", 1), FunctionSignature.of("get", 2));
	}

	@Test
	public void returnsStatusHeadersParsedBodyAndRawBody() throws IOException {
		JsonNode response = run(url("/json"), unlimited()).get(0);
		assertThat(response.get("status").intValue()).isEqualTo(200);
		assertThat(response.get("body").get("message").textValue()).isEqualTo("ok");
		assertThat(response.get("raw_body").binaryValue()).isEqualTo("{\"message\":\"ok\"}".getBytes(StandardCharsets.UTF_8));
		assertThat(response.get("headers"))
				.anySatisfy(header -> {
					assertThat(header.get("name").textValue()).isEqualToIgnoringCase("X-Repeat");
					assertThat(header.get("value").textValue()).isIn("first", "second");
				});
	}

	@Test
	public void decodesTextAndContentEncodings() throws IOException {
		assertThat(run(url("/text"), unlimited()).get(0).get("body").textValue()).isEqualTo("caf\u00e9");
		JsonNode gzip = run(url("/gzip"), unlimited()).get(0);
		assertThat(gzip.get("body").get("compressed").booleanValue()).isTrue();
		assertThat(gzip.get("raw_body").binaryValue()).isEqualTo("{\"compressed\":true}".getBytes(StandardCharsets.UTF_8));
		assertThat(run(url("/deflate"), unlimited()).get(0).get("body").textValue()).isEqualTo("deflated");
	}

	@Test
	public void returnsNonSuccessAndEmptyResponses() throws IOException {
		JsonNode binary = run(url("/binary"), unlimited()).get(0);
		assertThat(binary.get("status").intValue()).isEqualTo(418);
		assertThat(binary.get("body").isNull()).isTrue();
		assertThat(binary.get("raw_body").binaryValue()).containsExactly(0, 1, (byte) 0xff);
		assertThat(run(url("/empty"), unlimited()).get(0).get("body").isNull()).isTrue();
	}

	@Test
	public void followsSameProtocolRedirectsAndAcceptsOptions() {
		String arguments = quotedUrl("/redirect") + "; {timeout: 0.5, expected_status: 200}";
		JsonNode response = run(arguments, unlimited()).get(0);
		assertThat(response.get("status").intValue()).isEqualTo(200);
		assertThat(response.get("body").get("message").textValue()).isEqualTo("ok");
	}

	@Test
	public void acceptsEmptyOptionsAndMultipleArgumentResults() {
		assertThat(run(quotedUrl("/json") + "; {}", unlimited())).hasSize(1);

		String urls = "(" + quotedUrl("/json") + "," + quotedUrl("/json") + ")";
		assertThat(run(urls + "; ({}, {expected_status: 200})", unlimited())).hasSize(4);
	}

	@Test
	public void acceptsExpectedStatusNumberOrArray() {
		JsonNode expectedNumber = run(quotedUrl("/json") + "; {expected_status:200}", unlimited()).get(0);
		assertThat(expectedNumber.get("status").intValue()).isEqualTo(200);

		JsonNode expectedArray = run(quotedUrl("/binary") + "; {expected_status:[200,418]}", unlimited()).get(0);
		assertThat(expectedArray.get("status").intValue()).isEqualTo(418);
	}

	@Test
	public void rejectsUnexpectedStatusBeforeReadingBody() {
		assertThatThrownBy(() -> run(quotedUrl("/invalid-json") + "; {expected_status:201}", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessage("http::get expected status 201 but got 200");
		assertThatThrownBy(() -> run(quotedUrl("/binary") + "; {expected_status:[200,204]}", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessage("http::get expected one of [200, 204] but got 418");
	}

	@Test
	public void fallsBackToBase64WhenBinaryIsUnsupported() {
		JsonProvider<JsonElement> provider = GsonJsonProvider.getInstance();
		Environment<JsonElement> environment = EnvironmentBuilder.withDefaultLoaders(provider, Versions.JQ_1_8_2).build();
		JsonQuery<JsonElement> query = environment.compile(IMPORT + "http::get(" + quotedUrl("/binary") + ")");
		JsonElement response = query.apply(provider.createNull()).get(0);
		JsonElement rawBody = provider.getObjectMemberOrThrow(response, "raw_body");
		assertThat(provider.getString(rawBody)).isEqualTo(Base64.getEncoder().encodeToString(new byte[] { 0, 1, (byte) 0xff }));
	}

	@Test
	public void rejectsInvalidInputsAndResponses() {
		assertThatThrownBy(() -> run("{}", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("URL must be a string");
		assertThatThrownBy(() -> run(quotedUrl("/json") + "; null", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("options must be an object");
		assertThatThrownBy(() -> run(quotedUrl("/json") + "; {url:" + quotedUrl("/json") + "}", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("unknown member: url");
		assertThatThrownBy(() -> run(quotedUrl("/json") + "; {extra:true}", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("unknown member");
		assertThatThrownBy(() -> run(quotedUrl("/json") + "; {timeout:0}", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("positive finite number");
		assertThatThrownBy(() -> run(quotedUrl("/json") + "; {expected_status:null}", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("HTTP status number or a nonempty array");
		assertThatThrownBy(() -> run(quotedUrl("/json") + "; {expected_status:200.5}", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("exact integers from 100 through 599");
		assertThatThrownBy(() -> run(quotedUrl("/json") + "; {expected_status:nan}", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("exact integers from 100 through 599");
		assertThatThrownBy(() -> run(quotedUrl("/json") + "; {expected_status:99}", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("exact integers from 100 through 599");
		assertThatThrownBy(() -> run(quotedUrl("/json") + "; {expected_status:600}", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("exact integers from 100 through 599");
		assertThatThrownBy(() -> run(quotedUrl("/json") + "; {expected_status:[]}", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("must not be empty");
		assertThatThrownBy(() -> run(quotedUrl("/json") + "; {expected_status:[200,\"ok\"]}", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("exact integers from 100 through 599");
		assertThatThrownBy(() -> run("\"file:///tmp/value\"", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("only supports http and https");
		assertThatThrownBy(() -> run(url("/invalid-json"), unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("not valid JSON");
		assertThatThrownBy(() -> run(url("/unsupported-encoding"), unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("does not support Content-Encoding");
	}

	@Test
	public void enforcesRawBodyLimit() {
		RuntimeOptions options = RuntimeOptions.newBuilder().setMaxBinaryLength(2).build();
		assertThatThrownBy(() -> run(url("/binary"), options))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("maximum binary length of 2");
	}

	private static String url(String path) {
		return quotedUrl(path);
	}

	private static String quotedUrl(String path) {
		return JSON_PROVIDER.format(JSON_PROVIDER.createString(baseUrl + path));
	}

	private static RuntimeOptions unlimited() {
		return RuntimeOptions.newBuilder().build();
	}

	private static List<JsonNode> run(String argument, RuntimeOptions options) {
		Environment<JsonNode> environment = EnvironmentBuilder.withDefaultLoaders(JSON_PROVIDER, Versions.JQ_1_8_2).build();
		JsonQuery<JsonNode> query = environment.compile(IMPORT + "http::get(" + argument + ")").withRuntimeOptions(options);
		return query.apply(JSON_PROVIDER.createNull());
	}

	private static byte[] compress(String value, boolean gzip) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		if (gzip) {
			try (GZIPOutputStream output = new GZIPOutputStream(bytes)) {
				output.write(value.getBytes(StandardCharsets.UTF_8));
			}
		} else {
			try (DeflaterOutputStream output = new DeflaterOutputStream(bytes)) {
				output.write(value.getBytes(StandardCharsets.UTF_8));
			}
		}
		return bytes.toByteArray();
	}

	private static void send(HttpExchange exchange, int status, byte[] body) throws IOException {
		exchange.sendResponseHeaders(status, body.length);
		exchange.getResponseBody().write(body);
		exchange.close();
	}
}
