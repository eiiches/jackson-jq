package net.thisptr.jackson.jq.v2.cli;

import java.util.Iterator;
import java.util.Map;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;

/**
 * Unified JSON printer for jq-compatible output formatting.
 * <p>
 * Supports both pretty-printed and compact formatting, in monochrome or with ANSI colorization.
 * Traverses structure using only {@link JsonProvider} accessors so all JSON providers produce
 * identical output.
 */
final class JqPrinter {
	private JqPrinter() {
	}

	/**
	 * Renders a value as JSON text without a trailing newline.
	 *
	 * @param provider the provider owning {@code node}
	 * @param node the value to render
	 * @param indent the indentation per nesting level (null for compact output)
	 * @param colors the color palette to apply (null for monochrome output)
	 * @return the rendered JSON text
	 */
	static <N> String print(JsonProvider<N> provider, N node, @Nullable String indent, @Nullable JqColors colors) {
		StringBuilder out = new StringBuilder();
		append(provider, out, node, indent, colors, 0);
		return out.toString();
	}

	private static <N> void append(JsonProvider<N> provider, StringBuilder out, N node,
								   @Nullable String indent, @Nullable JqColors colors, int depth) {
		JsonNodeType type = provider.getNodeType(node);
		switch (type) {
			case ARRAY:
				appendArray(provider, out, node, indent, colors, depth);
				break;
			case OBJECT:
				appendObject(provider, out, node, indent, colors, depth);
				break;
			default:
				appendScalar(provider, out, node, colors);
				break;
		}
	}

	private static <N> void appendArray(JsonProvider<N> provider, StringBuilder out, N node,
										@Nullable String indent, @Nullable JqColors colors, int depth) {
		Iterator<N> it = provider.getArrayElements(node);
		if (!it.hasNext()) {
			if (colors != null) {
				out.append(colors.colorize(colors.arrayColor(), "[]"));
			} else {
				out.append("[]");
			}
			return;
		}

		if (colors != null) {
			out.append(colors.colorize(colors.arrayColor(), "["));
		} else {
			out.append('[');
		}

		@Var boolean first = true;
		while (it.hasNext()) {
			if (!first) {
				if (colors != null) {
					out.append(colors.colorize(colors.arrayColor(), ","));
				} else {
					out.append(',');
				}
			}
			first = false;
			if (indent != null) {
				appendNewLine(out, indent, depth + 1);
			}
			append(provider, out, it.next(), indent, colors, depth + 1);
		}

		if (indent != null) {
			appendNewLine(out, indent, depth);
		}
		if (colors != null) {
			out.append(colors.colorize(colors.arrayColor(), "]"));
		} else {
			out.append(']');
		}
	}

	private static <N> void appendObject(JsonProvider<N> provider, StringBuilder out, N node,
										 @Nullable String indent, @Nullable JqColors colors, int depth) {
		Iterator<Map.Entry<String, N>> it = provider.getObjectMembers(node);
		if (!it.hasNext()) {
			if (colors != null) {
				out.append(colors.colorize(colors.objectColor(), "{}"));
			} else {
				out.append("{}");
			}
			return;
		}

		if (colors != null) {
			out.append(colors.colorize(colors.objectColor(), "{"));
		} else {
			out.append('{');
		}

		@Var boolean first = true;
		while (it.hasNext()) {
			Map.Entry<String, N> entry = it.next();
			if (!first) {
				if (colors != null) {
					out.append(colors.colorize(colors.objectColor(), ","));
				} else {
					out.append(',');
				}
			}
			first = false;
			if (indent != null) {
				appendNewLine(out, indent, depth + 1);
			}

			// Format key using provider's string formatting
			String formattedKey = provider.format(provider.createString(entry.getKey()));
			if (colors != null) {
				out.append(colors.colorize(colors.keyColor(), formattedKey));
				out.append(colors.colorize(colors.objectColor(), ":"));
			} else {
				out.append(formattedKey).append(':');
			}
			if (indent != null) {
				out.append(' ');
			}

			append(provider, out, entry.getValue(), indent, colors, depth + 1);
		}

		if (indent != null) {
			appendNewLine(out, indent, depth);
		}
		if (colors != null) {
			out.append(colors.colorize(colors.objectColor(), "}"));
		} else {
			out.append('}');
		}
	}

	private static <N> void appendScalar(JsonProvider<N> provider, StringBuilder out, N node,
										 @Nullable JqColors colors) {
		if (colors == null) {
			out.append(provider.format(node));
			return;
		}

		switch (provider.getNodeType(node)) {
			case NULL:
				out.append(colors.colorize(colors.nullColor(), "null"));
				break;
			case BOOLEAN:
				boolean b = provider.getBoolean(node);
				out.append(colors.colorize(b ? colors.trueColor() : colors.falseColor(), b ? "true" : "false"));
				break;
			case NUMBER:
				String num = provider.format(node);
				String color = "null".equals(num) ? colors.nullColor() : colors.numberColor();
				out.append(colors.colorize(color, num));
				break;
			case STRING:
			case BINARY:
				out.append(colors.colorize(colors.stringColor(), provider.format(node)));
				break;
			default:
				out.append(provider.format(node));
				break;
		}
	}

	private static void appendNewLine(StringBuilder out, String indent, int depth) {
		out.append('\n');
		for (@Var int i = 0; i < depth; ++i) {
			out.append(indent);
		}
	}
}
