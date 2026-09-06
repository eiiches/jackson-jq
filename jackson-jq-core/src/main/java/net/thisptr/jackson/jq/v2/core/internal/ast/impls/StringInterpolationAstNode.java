package net.thisptr.jackson.jq.v2.core.internal.ast.impls;

import java.util.List;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.misc.Pair;

public class StringInterpolationAstNode implements AstNode {
	private final List<Pair<Integer, AstNode>> interpolations;
	private final String template;
	private final @Nullable AstNode formatter;

	public StringInterpolationAstNode(String template, List<Pair<Integer, AstNode>> interpolations, @Nullable AstNode formatter) {
		this.template = template;
		this.interpolations = interpolations;
		this.formatter = formatter;
	}

	public String template() {
		return template;
	}

	public List<Pair<Integer, AstNode>> interpolations() {
		return interpolations;
	}

	public @Nullable AstNode formatter() {
		return formatter;
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
		for (Pair<Integer, AstNode> interpolation : interpolations) {
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
