package net.thisptr.jackson.jq.v2.json;

import java.io.Closeable;

/**
 * Reads a sequence of JSON values from a stream, one value at a time.
 * <p>
 * Values are read lazily, so a parser can be used to process input larger than memory, or to react to
 * each value as it arrives rather than after the whole stream has been consumed. Instances are not
 * thread-safe.
 *
 * @param <JsonNode> the native JSON tree node type used by the underlying JSON library
 */
public interface JsonParser<JsonNode> extends Closeable {
	/**
	 * Reads the next JSON value from the stream.
	 * <p>
	 * End of input is reported as {@link Maybe#absent()} rather than as a {@code null} value, so
	 * that a JSON {@code null} document still reads as a value on a provider whose underlying
	 * library represents JSON {@code null} as Java {@code null}.
	 *
	 * @return the next value, or {@link Maybe#absent()} once the input is exhausted
	 * @throws JsonException if the input is not well-formed JSON, or cannot be read
	 */
	Maybe<JsonNode> next();

	/**
	 * Releases this parser and closes the underlying stream.
	 */
	@Override
	void close();
}
