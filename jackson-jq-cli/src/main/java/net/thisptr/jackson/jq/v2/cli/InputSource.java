package net.thisptr.jackson.jq.v2.cli;

import java.util.function.Consumer;

/**
 * Supplies the sequence of values a query is applied to.
 * <p>
 * jq's input options ({@code -n}, {@code -R}, {@code -s}) do not change how a query runs, only which
 * values reach it: a stream of parsed JSON values, one string per input line, a single array holding
 * everything, and so on. Each of those is a source, so the option handling is confined to
 * {@link InputSources#create}.
 *
 * @param <N> the native JSON tree node type used by the underlying JSON library
 */
interface InputSource<N> {
	/**
	 * Reads the whole input, passing each value to {@code consumer} as it is read.
	 * <p>
	 * Values are produced lazily where the mode allows it, so input larger than memory is streamed
	 * rather than buffered. The streams the source reads are closed before returning.
	 *
	 * @param consumer receives each input value, in input order
	 */
	void readAll(Consumer<N> consumer);
}
