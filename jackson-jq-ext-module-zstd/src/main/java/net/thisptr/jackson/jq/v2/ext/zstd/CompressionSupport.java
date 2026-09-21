package net.thisptr.jackson.jq.v2.ext.zstd;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.UndefinedType;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;

final class CompressionSupport {
	/**
	 * The charset option the text conversions accept.
	 */
	static final Type OPTIONS = ObjectType.of("encoding", UnionType.of(StringType.getInstance(), UndefinedType.getInstance()));

	private CompressionSupport() {
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

	static String readText(String function, InputStream input, Charset charset, RuntimeLimits limits) {
		StringBuilder result = new StringBuilder();
		try (Reader reader = new InputStreamReader(input, charset.newDecoder()
				.onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT))) {
			char[] buffer = new char[8192];
			for (int count; (count = reader.read(buffer)) != -1; ) {
				checkStringLength(limits, (long) result.length() + count);
				result.append(buffer, 0, count);
			}
		} catch (CharacterCodingException e) {
			throw new JsonQueryException(function + " failed to decode the result using " + charset.name() + ": " + e.getMessage(), e);
		} catch (IOException e) {
			throw new JsonQueryException(function + " failed: " + e.getMessage(), e);
		}
		return result.toString();
	}

	static <JsonNode> JsonNode createBinaryValue(JsonProvider<JsonNode> jsonProvider, byte[] bytes, boolean binarySupported) {
		if (binarySupported)
			return jsonProvider.createBinary(bytes);
		return jsonProvider.createString(Base64.getEncoder().encodeToString(bytes));
	}

	static void copy(InputStream input, OutputStream output) throws IOException {
		byte[] buffer = new byte[8192];
		for (int count; (count = input.read(buffer)) != -1; )
			output.write(buffer, 0, count);
	}

	static void checkStringLength(RuntimeLimits limits, long length) {
		int maximum = limits.getMaxStringLength();
		if (length > maximum)
			throw new RuntimeLimitExceededException("String of " + length + " characters exceeds the maximum string length of " + maximum);
	}

	static int maximumBytesForBase64(int maximumStringLength) {
		return maximumStringLength == Integer.MAX_VALUE ? Integer.MAX_VALUE : (maximumStringLength / 4) * 3;
	}

	static final class LimitedByteArrayOutputStream extends ByteArrayOutputStream {
		private final boolean binary;
		private final int maximumBytes;
		private final int maximumStringLength;

		LimitedByteArrayOutputStream(int maximumBytes, boolean binary, int maximumStringLength) {
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
