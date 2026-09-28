package net.thisptr.jackson.jq.v2.json.internal.io;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.Maybe;

/**
 * Parses and formats JSON using a provider's node operations.
 */
public final class JsonCodec {
	private JsonCodec() {
	}

	/**
	 * Serializes a node using jq's JSON formatting semantics.
	 */
	public static <N> String format(JsonProvider<N> provider, N node) {
		return DefaultJsonFormatter.format(provider, node, FormatOptions.getDefaultInstance());
	}

	/**
	 * Serializes a node using the given number formatting options.
	 */
	public static <N> String format(JsonProvider<N> provider, N node, FormatOptions options) {
		return DefaultJsonFormatter.format(provider, node, options);
	}

	/**
	 * Creates a lazy parser for a sequence of JSON values from a UTF-8 stream.
	 * Closing the parser also closes the stream.
	 */
	public static <N> JsonParser<N> createParser(JsonProvider<N> provider, InputStream in) {
		return new DefaultJsonParser<>(provider, in);
	}

	/**
	 * Parses every JSON value in a string, in document order.
	 */
	public static <N> List<N> parseAll(JsonProvider<N> provider, String json) {
		List<N> result = new ArrayList<>();
		try (JsonParser<N> parser = createParser(provider, new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))) {
			for (@Var Maybe<N> value = parser.next(); value.isPresent(); value = parser.next())
				result.add(value.get());
		}
		return result;
	}

	/**
	 * Parses exactly one JSON value, rejecting empty input and trailing content.
	 */
	public static <N> N parse(JsonProvider<N> provider, String json) {
		try (JsonParser<N> parser = createParser(provider, new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))) {
			Maybe<N> value = parser.next();
			if (value.isAbsent())
				throw new JsonException("empty input");
			if (parser.next().isPresent())
				throw new JsonException("trailing content");
			return value.get();
		}
	}
}
