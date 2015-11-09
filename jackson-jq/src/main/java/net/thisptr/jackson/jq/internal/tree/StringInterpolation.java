package net.thisptr.jackson.jq.internal.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Pair;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class StringInterpolation extends JsonQuery {
	private List<Pair<Integer, JsonQuery>> interpolations;
	private String template;
	private JsonQuery formatter;

	public StringInterpolation(final String template, final List<Pair<Integer, JsonQuery>> interpolations, final JsonQuery formatter) {
		this.template = template;
		this.interpolations = interpolations;
		this.formatter = formatter;
	}

	@Override
	public List<JsonNode> apply(final Scope scope, final JsonNode in) throws JsonQueryException {
		if (interpolations.isEmpty())
			return Collections.<JsonNode> singletonList(new TextNode(template));

		final Stack<Pair<Integer, List<JsonNode>>> values = new Stack<>();
		for (final Pair<Integer, JsonQuery> entry : interpolations) {
			List<JsonNode> tmp = entry._2.apply(scope, in);
			if (formatter != null)
				tmp = formatter.apply(scope, tmp);
			values.push(Pair.of(entry._1, tmp));
		}

		final List<JsonNode> out = new ArrayList<>();
		final Stack<Pair<Integer, JsonNode>> stack = new Stack<>();
		recurse(out, stack, values);
		return out;
	}

	private void recurse(final List<JsonNode> out, final Stack<Pair<Integer, JsonNode>> stack, final Stack<Pair<Integer, List<JsonNode>>> values) {
		if (values.isEmpty()) {
			final StringBuilder builder = new StringBuilder();

			int pos = 0;
			for (int index = stack.size() - 1; index >= 0; --index) {
				final Pair<Integer, JsonNode> head = stack.get(index);
				builder.append(template.substring(pos, head._1));
				pos = head._1;

				builder.append(head._2.isValueNode() ? head._2.asText() : head._2.toString());
			}
			builder.append(template.substring(pos));
			out.add(new TextNode(builder.toString()));
		} else {
			final Pair<Integer, List<JsonNode>> last = values.pop();
			for (final JsonNode value : last._2) {
				stack.push(Pair.of(last._1, value));
				recurse(out, stack, values);
				stack.pop();
			}
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
		for (final Pair<Integer, JsonQuery> interpolation : interpolations) {
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
