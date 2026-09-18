package net.thisptr.jackson.jq.v2.ext.fs.functions;

import java.util.Iterator;
import java.util.Map;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;

final class JsonPrettyPrinter {
	private JsonPrettyPrinter() {
	}

	static <N> String print(JsonProvider<N> provider, N node, @Nullable String indent) {
		if (indent == null)
			return provider.format(node);
		StringBuilder out = new StringBuilder();
		append(provider, out, node, indent, 0);
		return out.toString();
	}

	private static <N> void append(JsonProvider<N> provider, StringBuilder out, N node, String indent, int depth) {
		if (provider.getNodeType(node) == JsonNodeType.ARRAY) {
			appendArray(provider, out, node, indent, depth);
		} else if (provider.getNodeType(node) == JsonNodeType.OBJECT) {
			appendObject(provider, out, node, indent, depth);
		} else {
			out.append(provider.format(node));
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
			out.append(provider.format(provider.createString(entry.getKey()))).append(": ");
			append(provider, out, entry.getValue(), indent, depth + 1);
		}
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
