package net.thisptr.jackson.jq.v2.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.json.JsonParser;
import net.thisptr.jackson.jq.v2.json.JsonProvider;

/**
 * Builds the {@link InputSource} selected by jq's input options.
 * <p>
 * Every source reads the input files as a single concatenated input, the way jq does, so a value or
 * a line may span a file boundary and {@code -s} collects the whole run of files into one array.
 */
final class InputSources {
	private static final int BUFFER_SIZE = 8192;

	private InputSources() {
	}

	/**
	 * Returns the source described by the given options.
	 * <p>
	 * {@code nullInput} takes precedence over the other two, exactly as in jq: {@code -n} feeds a
	 * single {@code null} and reads nothing.
	 *
	 * @param provider the provider creating the values
	 * @param streams the input files, in the order they were given
	 * @param nullInput whether {@code -n} was given
	 * @param rawInput whether {@code -R} was given
	 * @param slurp whether {@code -s} was given
	 * @return the source to read the input with
	 */
	static <N> InputSource<N> create(JsonProvider<N> provider, List<InputStream> streams, boolean nullInput, boolean rawInput, boolean slurp) {
		if (nullInput)
			return consumer -> consumer.accept(provider.createNull());
		if (rawInput && slurp)
			return consumer -> consumer.accept(provider.createString(readWholeText(streams)));
		if (rawInput)
			return consumer -> forEachLine(streams, line -> consumer.accept(provider.createString(line)));
		if (slurp)
			return consumer -> consumer.accept(provider.createArray(readValues(provider, streams)));
		return consumer -> {
			for (InputStream stream : streams) {
				try (JsonParser<N> parser = provider.createParser(stream)) {
					for (@Var N value = parser.next(); value != null; value = parser.next())
						consumer.accept(value);
				}
			}
		};
	}

	/**
	 * Passes each line of the UTF-8 encoded input, without its line terminator, to {@code consumer}.
	 * <p>
	 * Lines are separated by {@code \n} alone, so a {@code \r} of a CRLF terminator stays part of the
	 * line; this is what jq does, and is why {@link java.io.BufferedReader#readLine()}, which also
	 * breaks on {@code \r} and {@code \r\n}, is not used. A final line without a terminator is still
	 * passed on, while input ending in {@code \n} produces no extra empty line. A partial line is
	 * carried over to the next file, since the files are one concatenated input.
	 *
	 * @param streams the streams to read, in order
	 * @param consumer receives each line, in input order
	 */
	private static void forEachLine(List<InputStream> streams, Consumer<String> consumer) {
		StringBuilder line = new StringBuilder();
		char[] buffer = new char[BUFFER_SIZE];
		for (InputStream stream : streams) {
			try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
				for (@Var int count = reader.read(buffer); count >= 0; count = reader.read(buffer)) {
					@Var int start = 0;
					for (int i = 0; i < count; ++i) {
						if (buffer[i] != '\n')
							continue;
						line.append(buffer, start, i - start);
						consumer.accept(line.toString());
						line.setLength(0);
						start = i + 1;
					}
					line.append(buffer, start, count - start);
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		if (line.length() > 0)
			consumer.accept(line.toString());
	}

	/**
	 * Reads the UTF-8 encoded input in full, keeping every line terminator.
	 *
	 * @param streams the streams to read, in order
	 * @return the text the input holds
	 */
	private static String readWholeText(List<InputStream> streams) {
		StringBuilder text = new StringBuilder();
		char[] buffer = new char[BUFFER_SIZE];
		for (InputStream stream : streams) {
			try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
				for (@Var int count = reader.read(buffer); count >= 0; count = reader.read(buffer))
					text.append(buffer, 0, count);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		return text.toString();
	}

	/**
	 * Reads every JSON value the input holds.
	 *
	 * @param provider the provider parsing the values
	 * @param streams the streams to read, in order
	 * @return the values, in input order
	 */
	private static <N> List<N> readValues(JsonProvider<N> provider, List<InputStream> streams) {
		List<N> values = new ArrayList<>();
		for (InputStream stream : streams) {
			try (JsonParser<N> parser = provider.createParser(stream)) {
				for (@Var N value = parser.next(); value != null; value = parser.next())
					values.add(value);
			}
		}
		return values;
	}
}
