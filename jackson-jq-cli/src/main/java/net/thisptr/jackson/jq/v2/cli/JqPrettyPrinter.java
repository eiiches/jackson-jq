package net.thisptr.jackson.jq.v2.cli;

import java.util.Iterator;
import java.util.Map;

import com.google.errorprone.annotations.Var;

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
		StringBuilder out = new StringBuilder();
		append(provider, out, node, indent, 0);
		return out.toString();
	}

	private static <N> void append(JsonProvider<N> provider, StringBuilder out, N node, String indent, int depth) {
		switch (provider.getNodeType(node)) {
			case ARRAY:
				appendArray(provider, out, node, indent, depth);
				break;
			case OBJECT:
				appendObject(provider, out, node, indent, depth);
				break;
			default:
				out.append(provider.format(node));
				break;
		}
	}

	private static <N> void appendArray(JsonProvider<N> provider, StringBuilder out, N node, String indent, int depth) {
		out.append('[');
		@Var boolean empty = true;
		for (Iterator<N> it = provider.getArrayElements(node); it.hasNext(); ) {
			if (!empty)
				out.append(',');
			empty = false;
			appendNewLine(out, indent, depth + 1);
			append(provider, out, it.next(), indent, depth + 1);
		}
		// jq keeps an empty array on one line.
		if (!empty)
			appendNewLine(out, indent, depth);
		out.append(']');
	}

	private static <N> void appendObject(JsonProvider<N> provider, StringBuilder out, N node, String indent, int depth) {
		out.append('{');
		@Var boolean empty = true;
		for (Iterator<Map.Entry<String, N>> it = provider.getObjectMembers(node); it.hasNext(); ) {
			Map.Entry<String, N> entry = it.next();
			if (!empty)
				out.append(',');
			empty = false;
			appendNewLine(out, indent, depth + 1);
			// Escaping a name through the provider keeps it identical to how it escapes string values.
			out.append(provider.format(provider.createString(entry.getKey()))).append(": ");
			append(provider, out, entry.getValue(), indent, depth + 1);
		}
		// jq keeps an empty object on one line.
		if (!empty)
			appendNewLine(out, indent, depth);
		out.append('}');
	}

	private static void appendNewLine(StringBuilder out, String indent, int depth) {
		out.append('\n');
		for (int i = 0; i < depth; ++i)
			out.append(indent);
	}
}
