package net.thisptr.jackson.jq.v2.ext.http.functions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonException;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.RuntimeLimits;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.exception.RuntimeLimitExceededException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public class HttpGetFunction implements Function {
	private static final int DEFAULT_TIMEOUT_MILLIS = 30_000;
	private static final Pattern CHARSET_PARAMETER = Pattern.compile("(?:^|;)\\s*charset\\s*=\\s*(?:\"([^\"]*)\"|([^;\\s]*))", Pattern.CASE_INSENSITIVE);

	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindContext, List<Expression<Context, JsonNode>> arguments) {
		JsonProvider<JsonNode> jsonProvider = bindContext.getJsonProvider();
		Expression<Context, JsonNode> urlExpression = arguments.get(0);
		Expression<Context, JsonNode> optionsExpression = arguments.size() == 2 ? arguments.get(1) : null;
		boolean binarySupported = supportsBinary(jsonProvider);
		return new Expression<>() {
			@Override
			public Cardinality getCardinality() {
				Cardinality urlCardinality = urlExpression.getCardinality();
				if (optionsExpression == null || urlCardinality == Cardinality.ZERO)
					return urlCardinality;
				Cardinality optionsCardinality = optionsExpression.getCardinality();
				if (optionsCardinality == Cardinality.ZERO)
					return Cardinality.ZERO;
				return urlCardinality == Cardinality.ONE && optionsCardinality == Cardinality.ONE ? Cardinality.ONE : Cardinality.UNKNOWN;
			}

			@Override
			public boolean dependsOnInput() {
				return urlExpression.dependsOnInput() || (optionsExpression != null && optionsExpression.dependsOnInput());
			}

			@Override
			public boolean dependsOnExternalState() {
				return true;
			}

			@Override
			public void apply(Context context, JsonNode input, Path<JsonNode> inputPath, Output<JsonNode> output) throws JsonQueryException {
				urlExpression.apply(context, input, inputPath, (urlNode, urlPath) -> {
					String url = parseUrl(jsonProvider, urlNode);
					if (optionsExpression == null) {
						Request request = new Request(url, DEFAULT_TIMEOUT_MILLIS, Collections.emptyList());
						output.emit(execute(jsonProvider, context.getRuntimeLimits(), request, binarySupported), UntrackedPath.getInstance());
						return;
					}
					optionsExpression.apply(context, input, inputPath, (optionsNode, optionsPath) -> {
						Request request = parseRequest(jsonProvider, url, optionsNode);
						output.emit(execute(jsonProvider, context.getRuntimeLimits(), request, binarySupported), UntrackedPath.getInstance());
					});
				});
			}
		};
	}

	private static <JsonNode> boolean supportsBinary(JsonProvider<JsonNode> jsonProvider) {
		try {
			jsonProvider.createBinary(new byte[0]);
			return true;
		} catch (UnsupportedOperationException e) {
			return false;
		}
	}

	private static <JsonNode> String parseUrl(JsonProvider<JsonNode> jsonProvider, JsonNode node) {
		JsonNodeType type = jsonProvider.getNodeType(node);
		if (type != JsonNodeType.STRING)
			throw new JsonQueryException("http::get URL must be a string, but got " + type);
		return jsonProvider.getString(node);
	}

	private static <JsonNode> Request parseRequest(JsonProvider<JsonNode> jsonProvider, String url, JsonNode options) {
		JsonNodeType type = jsonProvider.getNodeType(options);
		if (type != JsonNodeType.OBJECT)
			throw new JsonQueryException("http::get options must be an object, but got " + type);

		Iterator<String> names = jsonProvider.getObjectMemberNames(options);
		while (names.hasNext()) {
			String name = names.next();
			if (!name.equals("timeout") && !name.equals("expected_status"))
				throw new JsonQueryException("http::get options contains unknown member: " + name);
		}

		@Var int timeoutMillis = DEFAULT_TIMEOUT_MILLIS;
		if (jsonProvider.hasObjectMember(options, "timeout"))
			timeoutMillis = parseTimeout(jsonProvider, jsonProvider.getObjectMemberOrThrow(options, "timeout"));
		@Var List<Integer> expectedStatuses = Collections.emptyList();
		if (jsonProvider.hasObjectMember(options, "expected_status"))
			expectedStatuses = parseExpectedStatuses(jsonProvider, jsonProvider.getObjectMemberOrThrow(options, "expected_status"));
		return new Request(url, timeoutMillis, expectedStatuses);
	}

	private static <JsonNode> List<Integer> parseExpectedStatuses(JsonProvider<JsonNode> jsonProvider, JsonNode node) {
		if (jsonProvider.isNumber(node))
			return Collections.singletonList(parseExpectedStatus(jsonProvider, node));
		if (!jsonProvider.isArray(node))
			throw new JsonQueryException("http::get expected_status must be an HTTP status number or a nonempty array of HTTP status numbers");
		if (jsonProvider.getArrayLength(node) == 0)
			throw new JsonQueryException("http::get expected_status array must not be empty");
		List<Integer> result = new ArrayList<>();
		Iterator<JsonNode> elements = jsonProvider.getArrayElements(node);
		while (elements.hasNext())
			result.add(parseExpectedStatus(jsonProvider, elements.next()));
		return result;
	}

	private static <JsonNode> int parseExpectedStatus(JsonProvider<JsonNode> jsonProvider, JsonNode node) {
		if (!jsonProvider.isNumber(node))
			throw new JsonQueryException("http::get expected_status values must be exact integers from 100 through 599");
		Integer status = jsonProvider.getNumberAsIntExact(node);
		if (status == null || status < 100 || status > 599)
			throw new JsonQueryException("http::get expected_status values must be exact integers from 100 through 599");
		return status;
	}

	private static <JsonNode> int parseTimeout(JsonProvider<JsonNode> jsonProvider, JsonNode node) {
		if (!jsonProvider.isNumber(node))
			throw new JsonQueryException("http::get timeout must be a positive finite number of seconds");
		BigDecimal seconds = jsonProvider.getNumberAsBigDecimalExact(node);
		if (seconds == null || seconds.signum() <= 0)
			throw new JsonQueryException("http::get timeout must be a positive finite number of seconds");
		BigDecimal millis = seconds.movePointRight(3).setScale(0, RoundingMode.CEILING);
		if (millis.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0)
			throw new JsonQueryException("http::get timeout is too large");
		return millis.intValueExact();
	}

	private static <JsonNode> JsonNode execute(JsonProvider<JsonNode> jsonProvider, RuntimeLimits limits, Request request, boolean binarySupported) {
		checkObjectLength(limits, 4);
		HttpURLConnection connection = open(request);
		try {
			int status = connection.getResponseCode();
			checkExpectedStatus(request.expectedStatuses(), status);
			List<JsonNode> headers = createHeaders(jsonProvider, limits, connection.getHeaderFields());
			byte[] rawBody = readBody(connection, limits, binarySupported);
			JsonNode body = createBody(jsonProvider, limits, rawBody, connection.getContentType());
			JsonNode rawBodyNode = createRawBody(jsonProvider, rawBody, binarySupported);

			Map<String, JsonNode> response = new LinkedHashMap<>();
			response.put("status", jsonProvider.createNumber(status));
			response.put("headers", jsonProvider.createArray(headers));
			response.put("body", body);
			response.put("raw_body", rawBodyNode);
			return jsonProvider.createObject(response);
		} catch (IOException e) {
			throw new JsonQueryException("http::get failed for " + request.url() + ": " + e.getMessage(), e);
		} finally {
			connection.disconnect();
		}
	}

	private static void checkExpectedStatus(List<Integer> expectedStatuses, int actualStatus) {
		if (expectedStatuses.isEmpty() || expectedStatuses.contains(actualStatus))
			return;
		if (expectedStatuses.size() == 1)
			throw new JsonQueryException("http::get expected status " + expectedStatuses.get(0) + " but got " + actualStatus);
		throw new JsonQueryException("http::get expected one of " + expectedStatuses + " but got " + actualStatus);
	}

	private static HttpURLConnection open(Request request) {
		try {
			URL url = new URL(request.url());
			String protocol = url.getProtocol();
			if (!protocol.equalsIgnoreCase("http") && !protocol.equalsIgnoreCase("https"))
				throw new JsonQueryException("http::get only supports http and https URLs");
			URLConnection rawConnection = url.openConnection();
			if (!(rawConnection instanceof HttpURLConnection connection))
				throw new JsonQueryException("http::get only supports HTTP connections");
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(request.timeoutMillis());
			connection.setReadTimeout(request.timeoutMillis());
			connection.setInstanceFollowRedirects(true);
			connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
			return connection;
		} catch (IOException | IllegalArgumentException | SecurityException e) {
			throw new JsonQueryException("http::get failed to open " + request.url() + ": " + e.getMessage(), e);
		}
	}

	private static <JsonNode> List<JsonNode> createHeaders(JsonProvider<JsonNode> jsonProvider, RuntimeLimits limits, Map<String, List<String>> fields) {
		List<JsonNode> result = new ArrayList<>();
		for (Map.Entry<String, List<String>> field : fields.entrySet()) {
			String name = field.getKey();
			if (name == null)
				continue;
			checkStringLength(limits, name);
			for (String value : field.getValue()) {
				checkArrayLength(limits, (long) result.size() + 1);
				checkObjectLength(limits, 2);
				checkStringLength(limits, value);
				Map<String, JsonNode> header = new LinkedHashMap<>();
				header.put("name", jsonProvider.createString(name));
				header.put("value", jsonProvider.createString(value));
				result.add(jsonProvider.createObject(header));
			}
		}
		return result;
	}

	private static byte[] readBody(HttpURLConnection connection, RuntimeLimits limits, boolean binarySupported) throws IOException {
		int maximumBytes = binarySupported ? limits.getMaxBinaryLength() : maximumBytesForBase64(limits.getMaxStringLength());
		LimitedByteArrayOutputStream result = new LimitedByteArrayOutputStream(maximumBytes, binarySupported, limits.getMaxStringLength());
		@Var InputStream responseStream;
		try {
			responseStream = connection.getInputStream();
		} catch (IOException e) {
			responseStream = connection.getErrorStream();
			if (responseStream == null)
				throw e;
		}
		try (InputStream decoded = decodeContent(responseStream, connection.getHeaderField("Content-Encoding"))) {
			byte[] buffer = new byte[8192];
			for (int count; (count = decoded.read(buffer)) != -1; )
				result.write(buffer, 0, count);
		}
		return result.toByteArray();
	}

	private static InputStream decodeContent(InputStream input, @Nullable String contentEncoding) throws IOException {
		if (contentEncoding == null || contentEncoding.trim().isEmpty())
			return input;
		String[] encodings = contentEncoding.split(",");
		@Var InputStream result = input;
		for (int i = encodings.length - 1; i >= 0; --i) {
			String encoding = encodings[i].trim().toLowerCase(Locale.ROOT);
			switch (encoding) {
				case "identity", "" -> {
				}
				case "gzip" -> result = new GZIPInputStream(result);
				case "deflate" -> result = new InflaterInputStream(result);
				default -> {
					try {
						result.close();
					} catch (IOException closeFailure) {
						// Preserve the actionable unsupported-encoding error.
					}
					throw new JsonQueryException("http::get does not support Content-Encoding: " + encoding);
				}
			}
		}
		return result;
	}

	private static <JsonNode> JsonNode createBody(JsonProvider<JsonNode> jsonProvider, RuntimeLimits limits, byte[] bytes, @Nullable String contentType) {
		MediaType mediaType = MediaType.parse(contentType);
		if (bytes.length == 0 && mediaType.json())
			return jsonProvider.createNull();
		if (!mediaType.json() && !mediaType.text())
			return jsonProvider.createNull();

		String text = new String(bytes, mediaType.charset());
		if (mediaType.text()) {
			checkStringLength(limits, text);
			return jsonProvider.createString(text);
		}
		try {
			JsonNode body = jsonProvider.parse(text);
			checkNodeLimits(jsonProvider, limits, body);
			return body;
		} catch (JsonException e) {
			throw new JsonQueryException("http::get response body is not valid JSON: " + e.getMessage(), e);
		}
	}

	private static <JsonNode> JsonNode createRawBody(JsonProvider<JsonNode> jsonProvider, byte[] bytes, boolean binarySupported) {
		if (binarySupported)
			return jsonProvider.createBinary(bytes);
		return jsonProvider.createString(Base64.getEncoder().encodeToString(bytes));
	}

	private static <JsonNode> void checkNodeLimits(JsonProvider<JsonNode> jsonProvider, RuntimeLimits limits, JsonNode node) {
		JsonNodeType type = jsonProvider.getNodeType(node);
		switch (type) {
			case STRING:
				checkStringLength(limits, jsonProvider.getString(node));
				break;
			case BINARY:
				checkBinaryLength(limits, jsonProvider.getBinaryAsByteArray(node).length);
				break;
			case ARRAY:
				checkArrayLength(limits, jsonProvider.getArrayLength(node));
				Iterator<JsonNode> elements = jsonProvider.getArrayElements(node);
				while (elements.hasNext())
					checkNodeLimits(jsonProvider, limits, elements.next());
				break;
			case OBJECT:
				checkObjectLength(limits, jsonProvider.getObjectMemberCount(node));
				Iterator<Map.Entry<String, JsonNode>> members = jsonProvider.getObjectMembers(node);
				while (members.hasNext()) {
					Map.Entry<String, JsonNode> member = members.next();
					checkStringLength(limits, member.getKey());
					checkNodeLimits(jsonProvider, limits, member.getValue());
				}
				break;
			case BOOLEAN:
			case NULL:
			case NUMBER:
				break;
		}
	}

	private static void checkArrayLength(RuntimeLimits limits, long length) {
		int maximum = limits.getMaxArrayLength();
		if (length > maximum)
			throw new RuntimeLimitExceededException("Array of " + length + " elements exceeds the maximum array length of " + maximum);
	}

	private static void checkObjectLength(RuntimeLimits limits, long length) {
		int maximum = limits.getMaxObjectMemberCount();
		if (length > maximum)
			throw new RuntimeLimitExceededException("Object of " + length + " members exceeds the maximum object member count of " + maximum);
	}

	private static void checkStringLength(RuntimeLimits limits, String value) {
		int maximum = limits.getMaxStringLength();
		if (value.length() > maximum)
			throw new RuntimeLimitExceededException("String of " + value.length() + " characters exceeds the maximum string length of " + maximum);
	}

	private static void checkBinaryLength(RuntimeLimits limits, long length) {
		int maximum = limits.getMaxBinaryLength();
		if (length > maximum)
			throw new RuntimeLimitExceededException("Binary value of " + length + " bytes exceeds the maximum binary length of " + maximum);
	}

	private static int maximumBytesForBase64(int maximumStringLength) {
		return maximumStringLength == Integer.MAX_VALUE ? Integer.MAX_VALUE : (maximumStringLength / 4) * 3;
	}

	private record Request(String url, int timeoutMillis, List<Integer> expectedStatuses) {
	}

	private record MediaType(boolean json, boolean text, Charset charset) {
		private static MediaType parse(@Nullable String contentType) {
			if (contentType == null)
				return new MediaType(false, false, StandardCharsets.UTF_8);
			int separator = contentType.indexOf(';');
			String type = (separator >= 0 ? contentType.substring(0, separator) : contentType).trim().toLowerCase(Locale.ROOT);
			boolean json = type.equals("application/json") || (type.startsWith("application/") && type.endsWith("+json"));
			boolean text = type.startsWith("text/");
			@Var Charset charset = StandardCharsets.UTF_8;
			Matcher matcher = CHARSET_PARAMETER.matcher(contentType);
			if (matcher.find()) {
				String name = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
				try {
					charset = Charset.forName(name);
				} catch (IllegalArgumentException e) {
					charset = StandardCharsets.UTF_8;
				}
			}
			return new MediaType(json, text, charset);
		}
	}

	private static final class LimitedByteArrayOutputStream extends ByteArrayOutputStream {
		private final boolean binary;
		private final int maximumBytes;
		private final int maximumStringLength;

		private LimitedByteArrayOutputStream(int maximumBytes, boolean binary, int maximumStringLength) {
			this.maximumBytes = maximumBytes;
			this.binary = binary;
			this.maximumStringLength = maximumStringLength;
		}

		@Override
		public synchronized void write(int value) {
			checkLength((long) count + 1);
			super.write(value);
		}

		@Override
		public synchronized void write(byte[] bytes, int offset, int length) {
			checkLength((long) count + length);
			super.write(bytes, offset, length);
		}

		private void checkLength(long length) {
			if (length <= maximumBytes)
				return;
			if (binary)
				throw new RuntimeLimitExceededException("Binary value of " + length + " bytes exceeds the maximum binary length of " + maximumBytes);
			long encodedLength = 4 * ((length + 2) / 3);
			throw new RuntimeLimitExceededException("String of " + encodedLength + " characters exceeds the maximum string length of " + maximumStringLength);
		}
	}
}
