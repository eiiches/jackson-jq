package net.thisptr.jackson.jq.v2.ext.binary;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Iterator;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.RuntimeLimits;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.exception.RuntimeLimitExceededException;
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.UndefinedType;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;

final class BinarySupport {
	/**
	 * The charset option the text conversions accept.
	 */
	static final Type OPTIONS = ObjectType.of("encoding", UnionType.of(StringType.getInstance(), UndefinedType.getInstance()));

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
		return decodeBase64(function, jsonProvider.getString(input));
	}

	static byte[] decodeBase64(String function, String text) {
		try {
			return Base64.getDecoder().decode(text);
		} catch (IllegalArgumentException e) {
			throw new JsonQueryException(function + " input must be valid Base64", e);
		}
	}

	static String encodeBase64(byte[] bytes, boolean urlSafe, RuntimeLimits limits) {
		long encodedLength = urlSafe
				? 4L * (bytes.length / 3L) + (bytes.length % 3 == 0 ? 0 : bytes.length % 3 + 1)
				: 4L * ((bytes.length + 2L) / 3L);
		checkStringLength(limits, encodedLength);
		return (urlSafe ? Base64.getUrlEncoder().withoutPadding() : Base64.getEncoder()).encodeToString(bytes);
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
