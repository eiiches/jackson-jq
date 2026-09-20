package net.thisptr.jackson.jq.v2.cli;

import net.thisptr.jackson.jq.v2.json.JsonProvider;

/**
 * Renders a JSON value the way jq's pretty printer does.
 * <p>
 * Only {@link JsonProvider} accessors are used, so every provider produces identical output. jq
 * indents structure and never scalars, so scalars are handed straight to
 * {@link JsonProvider#format(Object)}: that keeps each provider's jq number rules -- NaN, Infinity,
 * whole doubles, exact decimals -- and needs no knowledge of how numbers are represented.
 */
final class JqPrettyPrinter {
	private JqPrettyPrinter() {
	}

	/**
	 * Renders a value as indented JSON text, without a trailing newline.
	 *
	 * @param provider the provider owning {@code node}
	 * @param node the value to render
	 * @param indent the indentation of a single nesting level
	 * @return the indented JSON text
	 */
	static <N> String print(JsonProvider<N> provider, N node, String indent) {
		return JqPrinter.print(provider, node, indent, null);
	}
}
