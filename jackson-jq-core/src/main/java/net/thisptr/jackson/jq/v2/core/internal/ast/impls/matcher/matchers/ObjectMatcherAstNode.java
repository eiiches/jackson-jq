package net.thisptr.jackson.jq.v2.core.internal.ast.impls.matcher.matchers;

import java.util.List;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.matcher.PatternMatcherAstNode;

public class ObjectMatcherAstNode implements PatternMatcherAstNode {
	private final List<FieldMatcher> matchers;

	public ObjectMatcherAstNode(List<FieldMatcher> matchers) {
		this.matchers = matchers;
	}

	public List<FieldMatcher> matchers() {
		return matchers;
	}

	/**
	 * A single field of an object pattern. The implementation reflects how the field name was
	 * written in the source: {@link ConstantKeyFieldMatcher} for a name that is a constant string,
	 * {@link ExpressionKeyFieldMatcher} for a name that is computed by an expression.
	 */
	public interface FieldMatcher {
		/**
		 * Returns the pattern the field value is matched against, or null for the {@code {$x}}
		 * shorthand, which binds the field value to {@code $x} without matching it any further.
		 */
		@Nullable
		PatternMatcherAstNode matcher();
	}

	/**
	 * A field whose name is a constant string, i.e. an identifier or a keyword:
	 *
	 * <ul>
	 * <li>{@code {$x}}: shorthand variable match (dollar = true, matcher = null)</li>
	 * <li>{@code {$x: matcher}}: variable match with an explicit pattern</li>
	 * <li>{@code {x: matcher}}: identifier / keyword key with a pattern</li>
	 * </ul>
	 */
	public static class ConstantKeyFieldMatcher implements FieldMatcher {
		private final boolean dollar;
		private final String name;
		private final @Nullable PatternMatcherAstNode matcher;

		public ConstantKeyFieldMatcher(boolean dollar, String name, @Nullable PatternMatcherAstNode matcher) {
			if (!dollar && matcher == null)
				throw new IllegalArgumentException("BUG: matcher must not be null when dollar = false");
			this.dollar = dollar;
			this.name = name;
			this.matcher = matcher;
		}

		/**
		 * Returns whether the name was written with a leading {@code $}, in which case the field
		 * value is also bound to a variable of the same name.
		 */
		public boolean dollar() {
			return dollar;
		}

		/**
		 * Returns the field name, as written in the source, without the leading {@code $}.
		 */
		public String name() {
			return name;
		}

		@Override
		public @Nullable PatternMatcherAstNode matcher() {
			return matcher;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (dollar)
				sb.append('$');
			sb.append(name);
			if (matcher != null) {
				sb.append(": ");
				sb.append(matcher);
			}
			return sb.toString();
		}
	}

	/**
	 * A field whose name is computed by an expression:
	 *
	 * <ul>
	 * <li>{@code {"str": matcher}}: string literal key with a pattern</li>
	 * <li>{@code {"\(interp)": matcher}}: string interpolation key with a pattern</li>
	 * <li>{@code {(expr): matcher}}: parenthesized key expression with a pattern; the parentheses
	 * are part of the name expression, which is a {@code ParenAstNode}</li>
	 * </ul>
	 */
	public static class ExpressionKeyFieldMatcher implements FieldMatcher {
		private final AstNode name;
		private final PatternMatcherAstNode matcher;

		public ExpressionKeyFieldMatcher(AstNode name, PatternMatcherAstNode matcher) {
			this.name = name;
			this.matcher = matcher;
		}

		/**
		 * Returns the expression that evaluates to the field name.
		 */
		public AstNode name() {
			return name;
		}

		@Override
		public PatternMatcherAstNode matcher() {
			return matcher;
		}

		@Override
		public String toString() {
			return name + ": " + matcher;
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
