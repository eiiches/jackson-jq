package net.thisptr.jackson.jq.v2.cli;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.internal.io.JsonCodec;

/**
 * Renders a JSON value the way jq's pretty printer does.
 * <p>
 * The shared {@link JsonCodec} supplies jq's number formatting rules for scalars. jq indents
 * structure and never scalars, so providers need only supply node accessors.
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
