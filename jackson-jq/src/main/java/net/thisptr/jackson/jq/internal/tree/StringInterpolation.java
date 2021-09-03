package net.thisptr.jackson.jq.internal.tree;

import java.util.Collections;
import java.util.List;
import java.util.Stack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Pair;
import net.thisptr.jackson.jq.path.Path;

public class StringInterpolation implements Expression {
	private List<Pair<Integer, Expression>> interpolations;
	private String template;
	private Expression formatter;

	public StringInterpolation(final String template, final List<Pair<Integer, Expression>> interpolations, final Expression formatter) {
		this.template = template;
		this.interpolations = interpolations;
		this.formatter = formatter;
	}

	public StringInterpolation() {}

	public Expression getFormatter() {
		return formatter;
	}

	public void setFormatter(Expression formatter) {
		this.formatter = formatter;
	}

	public List<Pair<Integer, Expression>> getInterpolations() {
		return Collections.unmodifiableList(interpolations);
	}

	public void setInterpolations(List<Pair<Integer, Expression>> interpolations) {
		this.interpolations = interpolations;
	}

	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Path ipath, final PathOutput output, final boolean requirePath) throws JsonQueryException {
		final Stack<Pair<Integer, JsonNode>> stack = new Stack<>();
		recurse(scope, in, output, stack, interpolations);
	}

	private void recurse(final Scope scope, final JsonNode in, final PathOutput output, final Stack<Pair<Integer, JsonNode>> stack, final List<Pair<Integer, Expression>> interpolations) throws JsonQueryException {
		if (interpolations.isEmpty()) {
			final StringBuilder builder = new StringBuilder();
			int pos = 0;
			for (int index = stack.size() - 1; index >= 0; --index) {
				final Pair<Integer, JsonNode> head = stack.get(index);
				builder.append(template.substring(pos, head._1));
				pos = head._1;

				builder.append(head._2.isValueNode() ? head._2.asText() : head._2.toString());
			}
			builder.append(template.substring(pos));
			output.emit(new TextNode(builder.toString()), null);
		} else {
			final Pair<Integer, Expression> rhead = interpolations.get(interpolations.size() - 1);
			final List<Pair<Integer, Expression>> rtail = interpolations.subList(0, interpolations.size() - 1);
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
		for (final Pair<Integer, Expression> interpolation : interpolations) {
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
