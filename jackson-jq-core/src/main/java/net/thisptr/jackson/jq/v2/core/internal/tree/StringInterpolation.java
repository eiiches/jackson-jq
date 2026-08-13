package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.List;
import java.util.Stack;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.Pair;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class StringInterpolation implements Expression {
	private final List<Pair<Integer, Expression>> interpolations;
	private final String template;
	private final Expression formatter;

	public StringInterpolation(String template, List<Pair<Integer, Expression>> interpolations, Expression formatter) {
		this.template = template;
		this.interpolations = interpolations;
		this.formatter = formatter;
	}

	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		Stack<Pair<Integer, JsonNode>> stack = new Stack<>();
		recurse(scope, in, output, stack, interpolations);
	}

	private <JsonNode> void recurse(Scope<JsonNode> scope, JsonNode in, PathOutput<JsonNode> output, Stack<Pair<Integer, JsonNode>> stack, List<Pair<Integer, Expression>> interpolations) throws JsonQueryException {
		if (interpolations.isEmpty()) {
			StringBuilder builder = new StringBuilder();
			@Var int pos = 0;
			for (int index = stack.size() - 1; index >= 0; --index) {
				Pair<Integer, JsonNode> head = stack.get(index);
				builder.append(template.substring(pos, head._1));
				pos = head._1;

				JsonNodeType nodeType = scope.jsonProvider().getNodeType(head._2);
				boolean isValueNode = nodeType != JsonNodeType.ARRAY && nodeType != JsonNodeType.OBJECT;
				builder.append(isValueNode ? scope.jsonProvider().asText(head._2) : scope.jsonProvider().toString(head._2));
			}
			builder.append(template.substring(pos));
			output.emit(scope.jsonProvider().createString(builder.toString()), null);
		} else {
			Pair<Integer, Expression> rhead = interpolations.get(interpolations.size() - 1);
			List<Pair<Integer, Expression>> rtail = interpolations.subList(0, interpolations.size() - 1);
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
		@Var int pos = 0;
		StringBuilder builder = new StringBuilder();
		if (formatter != null) {
			builder.append(formatter);
			builder.append(" ");
		}
		builder.append("\"");
		for (Pair<Integer, Expression> interpolation : interpolations) {
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

	private static void copyEscaped(StringBuilder builder, String text, int begin, int end) {
		for (int i = begin; i < end; ++i) {
			char ch = text.charAt(i);
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
