package net.thisptr.jackson.jq.v2.core.internal.ast.matcher.matchers;

import java.util.List;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.literal.StringLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.matcher.PatternMatcherAstNode;

public class ObjectMatcherAstNode implements PatternMatcherAstNode {
	private List<FieldMatcher> matchers;

	public ObjectMatcherAstNode(List<FieldMatcher> matchers) {
		this.matchers = matchers;
	}

	public List<FieldMatcher> matchers() {
		return matchers;
	}

	public static class FieldMatcher {
		// e.g.
		// {$x} : dollar = true, name = "x", matcher = null
		// {$x: [$a]} : dollar = true, name = "x", matcher = [$a]
		// {x: [$a]} : dollar = false, name = "x", matcher = [$a]

		private boolean dollar;
		private AstNode name;
		private @Nullable PatternMatcherAstNode matcher;
		private boolean parenthesizedName;

		public FieldMatcher(boolean dollar, AstNode name, @Nullable PatternMatcherAstNode matcher) {
			this(dollar, name, matcher, false);
		}

		public FieldMatcher(boolean dollar, AstNode name, @Nullable PatternMatcherAstNode matcher, boolean parenthesizedName) {
			if (dollar && !(name instanceof StringLiteralAstNode))
				throw new IllegalArgumentException("BUG: name must be instance of StringLiteralAstNode when dollar = true");
			if (!dollar && matcher == null)
				throw new IllegalArgumentException("BUG: matcher must not be null when dollar = false");
			this.dollar = dollar;
			this.name = name;
			this.matcher = matcher;
			this.parenthesizedName = parenthesizedName;
		}

		public boolean dollar() {
			return dollar;
		}

		public AstNode name() {
			return name;
		}

		public @Nullable PatternMatcherAstNode rawMatcher() {
			return matcher;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (parenthesizedName)
				sb.append('(');
			sb.append(name);
			if (parenthesizedName)
				sb.append(')');
			if (matcher != null) {
				sb.append(": ");
				sb.append(matcher);
			}
			return sb.toString();
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("{");
		@Var String sep = "";
		for (FieldMatcher entry : matchers) {
			sb.append(sep);
			sb.append(entry.toString());
			sep = ", ";
		}
		sb.append("}");
		return sb.toString();
	}
}
