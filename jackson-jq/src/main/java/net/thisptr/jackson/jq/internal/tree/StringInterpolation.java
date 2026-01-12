package net.thisptr.jackson.jq.internal.tree;

import java.util.List;
import java.util.Stack;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Pair;
import net.thisptr.jackson.jq.path.Path;

public class StringInterpolation<JsonNode> implements Expression<JsonNode> {
	private final List<Pair<Integer, Expression<JsonNode>>> interpolations;
	private final String template;
	private final Expression<JsonNode> formatter;

	public StringInterpolation(final String template, final List<Pair<Integer, Expression<JsonNode>>> interpolations, final Expression<JsonNode> formatter) {
		this.template = template;
		this.interpolations = interpolations;
		this.formatter = formatter;
	}

	@Override
	public void apply(final Scope<JsonNode> scope, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final boolean requirePath) throws JsonQueryException {
		final Stack<Pair<Integer, JsonNode>> stack = new Stack<>();
		recurse(scope, in, output, stack, interpolations);
	}

	private void recurse(final Scope<JsonNode> scope, final JsonNode in, final PathOutput<JsonNode> output, final Stack<Pair<Integer, JsonNode>> stack, final List<Pair<Integer, Expression<JsonNode>>> interpolations) throws JsonQueryException {
		if (interpolations.isEmpty()) {
			final StringBuilder builder = new StringBuilder();
			int pos = 0;
			for (int index = stack.size() - 1; index >= 0; --index) {
				final Pair<Integer, JsonNode> head = stack.get(index);
				builder.append(template.substring(pos, head._1));
				pos = head._1;

				final JsonNodeType nodeType = scope.jsonProvider().getNodeType(head._2);
				final boolean isValueNode = nodeType != JsonNodeType.ARRAY && nodeType != JsonNodeType.OBJECT;
				builder.append(isValueNode ? scope.jsonProvider().asText(head._2) : scope.jsonProvider().toString(head._2));
			}
			builder.append(template.substring(pos));
			output.emit(scope.jsonProvider().createString(builder.toString()), null);
		} else {
			final Pair<Integer, Expression<JsonNode>> rhead = interpolations.get(interpolations.size() - 1);
			final List<Pair<Integer, Expression<JsonNode>>> rtail = interpolations.subList(0, interpolations.size() - 1);
			rhead._2.apply(scope, in, (interpolated) -> {
				if (formatter != null) {
					formatter.apply(scope, interpolated, (formatted) -> {
						stack.push(Pair.of(rhead._1, formatted));
						recurse(scope, in, output, stack, rtail);
						stack.pop();
					});
				} else {
					stack.push(Pair.of(rhead._1, interpolated));
					recurse(scope, in, output, stack, rtail);
					stack.pop();
				}
			});
		}
	}

	@Override
	public String toString() {
		int pos = 0;
		final StringBuilder builder = new StringBuilder();
		if (formatter != null) {
			builder.append(formatter);
			builder.append(" ");
		}
		builder.append("\"");
		for (final Pair<Integer, Expression<JsonNode>> interpolation : interpolations) {
			copyEscaped(builder, template, pos, interpolation._1);
			pos = interpolation._1;
			builder.append("\\(");
			builder.append(interpolation._2);
			builder.append(")");
		}
		copyEscaped(builder, template, pos, template.length());
		builder.append("\"");
		return builder.toString();
	}

	private static void copyEscaped(final StringBuilder builder, final String text, final int begin, final int end) {
		for (int i = begin; i < end; ++i) {
			final char ch = text.charAt(i);
			switch (ch) {
				case '\\':
					builder.append("\\\\");
					break;
				case '"':
					builder.append("\\\"");
					break;
				case '\b':
					builder.append("\\b");
					break;
				case '\f':
					builder.append("\\f");
					break;
				case '\r':
					builder.append("\\r");
					break;
				case '\t':
					builder.append("\\t");
					break;
				case '\n':
					builder.append("\\n");
					break;
				default:
					builder.append(ch);
			}
		}
	}
}
