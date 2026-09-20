package net.thisptr.jackson.jq.v2.ext.binary;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Iterator;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.RuntimeLimits;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.exception.RuntimeLimitExceededException;

final class BinarySupport {
	private BinarySupport() {
	}

	static <JsonNode> boolean supportsBinary(JsonProvider<JsonNode> jsonProvider) {
		return jsonProvider.getSupportedNodeTypes().contains(JsonNodeType.BINARY);
	}

	static <JsonNode> byte[] getInputBytes(JsonProvider<JsonNode> jsonProvider, String function, JsonNode input) {
		JsonNodeType type = jsonProvider.getNodeType(input);
		if (type == JsonNodeType.BINARY)
			return jsonProvider.getBinaryAsByteArray(input);
		if (type != JsonNodeType.STRING)
			throw new JsonQueryException(function + " requires binary or Base64 string input, but got " + type);
		try {
			return Base64.getDecoder().decode(jsonProvider.getString(input));
		} catch (IllegalArgumentException e) {
			throw new JsonQueryException(function + " input must be valid Base64", e);
		}
	}

	static <JsonNode> String getInputText(JsonProvider<JsonNode> jsonProvider, String function, JsonNode input) {
		JsonNodeType type = jsonProvider.getNodeType(input);
		if (type != JsonNodeType.STRING)
			throw new JsonQueryException(function + " requires string input, but got " + type);
		return jsonProvider.getString(input);
	}

	static <JsonNode> Charset parseCharset(JsonProvider<JsonNode> jsonProvider, String function, JsonNode options) {
		JsonNodeType type = jsonProvider.getNodeType(options);
		if (type != JsonNodeType.OBJECT)
			throw new JsonQueryException(function + " options must be an object, but got " + type);
		Iterator<String> names = jsonProvider.getObjectMemberNames(options);
		while (names.hasNext()) {
			String name = names.next();
			if (!name.equals("encoding"))
				throw new JsonQueryException(function + " options contains unknown member: " + name);
		}
		if (!jsonProvider.hasObjectMember(options, "encoding"))
			return StandardCharsets.UTF_8;
		JsonNode encoding = jsonProvider.getObjectMemberOrThrow(options, "encoding");
		if (!jsonProvider.isString(encoding))
			throw new JsonQueryException(function + " encoding must be a string");
		String name = jsonProvider.getString(encoding);
		try {
			return Charset.forName(name);
		} catch (IllegalArgumentException e) {
			throw new JsonQueryException(function + " unsupported encoding: " + name, e);
		}
	}

	static byte[] encodeText(String function, String text, Charset charset) {
		try {
			ByteBuffer encoded = charset.newEncoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT)
					.encode(CharBuffer.wrap(text));
			byte[] bytes = new byte[encoded.remaining()];
			encoded.get(bytes);
			return bytes;
		} catch (CharacterCodingException e) {
			throw new JsonQueryException(function + " failed to encode the input using " + charset.name() + ": " + e.getMessage(), e);
		}
	}

	static String decodeText(String function, byte[] bytes, Charset charset, RuntimeLimits limits) {
		StringBuilder result = new StringBuilder();
		try (Reader reader = new InputStreamReader(new ByteArrayInputStream(bytes), charset.newDecoder()
				.onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT))) {
			char[] buffer = new char[8192];
			for (int count; (count = reader.read(buffer)) != -1; ) {
				checkStringLength(limits, (long) result.length() + count);
				result.append(buffer, 0, count);
			}
		} catch (CharacterCodingException e) {
			throw new JsonQueryException(function + " failed to decode the input using " + charset.name() + ": " + e.getMessage(), e);
		} catch (IOException e) {
			throw new JsonQueryException(function + " failed: " + e.getMessage(), e);
		}
		return result.toString();
	}

	static <JsonNode> JsonNode createBinaryValue(JsonProvider<JsonNode> jsonProvider, byte[] bytes, boolean binarySupported, RuntimeLimits limits) {
		if (binarySupported) {
			int maximum = limits.getMaxBinaryLength();
			if (bytes.length > maximum)
				throw new RuntimeLimitExceededException("Binary value of " + bytes.length + " bytes exceeds the maximum binary length of " + maximum);
			return jsonProvider.createBinary(bytes);
		}
		long encodedLength = bytes.length == 0 ? 0 : 4L * ((bytes.length + 2) / 3);
		checkStringLength(limits, encodedLength);
		return jsonProvider.createString(Base64.getEncoder().encodeToString(bytes));
	}

	static void checkStringLength(RuntimeLimits limits, long length) {
		int maximum = limits.getMaxStringLength();
		if (length > maximum)
			throw new RuntimeLimitExceededException("String of " + length + " characters exceeds the maximum string length of " + maximum);
	}
}
