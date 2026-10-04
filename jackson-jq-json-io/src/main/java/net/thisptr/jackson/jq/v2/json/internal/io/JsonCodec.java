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
	 * Serializes a node using the given formatting options.
	 *
	 * @throws JsonSizeExceededException if the output would grow past {@link FormatOptions#getMaxLength()}
	 */
	public static <N> String format(JsonProvider<N> provider, N node, FormatOptions options) {
		return DefaultJsonFormatter.format(provider, node, options);
	}

	/**
	 * Creates a lazy parser for a sequence of JSON values from a UTF-8 stream.
	 * Closing the parser also closes the stream.
	 */
	public static <N> JsonParser<N> createParser(JsonProvider<N> provider, InputStream in) {
		return createParser(provider, in, ParseOptions.getDefaultInstance());
	}

	/**
	 * Creates a lazy parser that holds the numbers it reads as the given options ask.
	 * Closing the parser also closes the stream.
	 */
	public static <N> JsonParser<N> createParser(JsonProvider<N> provider, InputStream in, ParseOptions options) {
		return new DefaultJsonParser<>(provider, in, options);
	}

	/**
	 * Parses every JSON value in a string, in document order.
	 */
	public static <N> List<N> parseAll(JsonProvider<N> provider, String json) {
		return parseAll(provider, json, ParseOptions.getDefaultInstance());
	}

	/**
	 * Parses every JSON value in a string, in document order, with the given number options.
	 */
	public static <N> List<N> parseAll(JsonProvider<N> provider, String json, ParseOptions options) {
		List<N> result = new ArrayList<>();
		try (JsonParser<N> parser = createParser(provider, asStream(json), options)) {
			for (@Var Maybe<N> value = parser.next(); value.isPresent(); value = parser.next())
				result.add(value.get());
		}
		return result;
	}

	/**
	 * Parses exactly one JSON value, rejecting empty input and trailing content.
	 */
	public static <N> N parse(JsonProvider<N> provider, String json) {
		return parse(provider, json, ParseOptions.getDefaultInstance());
	}

	/**
	 * Parses exactly one JSON value with the given number options, rejecting empty input and trailing
	 * content.
	 */
	public static <N> N parse(JsonProvider<N> provider, String json, ParseOptions options) {
		try (JsonParser<N> parser = createParser(provider, asStream(json), options)) {
			Maybe<N> value = parser.next();
			if (value.isAbsent())
				throw new JsonException("empty input");
			if (parser.next().isPresent())
				throw new JsonException("trailing content");
			return value.get();
		}
	}

	private static InputStream asStream(String json) {
		return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
	}
}
