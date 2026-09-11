package net.thisptr.jackson.jq.v2.core.internal.ast;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;

/**
 * A numeric literal, holding the source text exactly as it was written. The parser makes no
 * attempt to decide what number the text denotes; that happens when the node is turned into an
 * {@link net.thisptr.jackson.jq.v2.spi.Expression}, or in
 * {@link net.thisptr.jackson.jq.v2.core.internal.utils.ExpressionUtils#evaluateLiteralExpression}.
 */
public class NumericLiteralAstNode extends AbstractValueLiteralAstNode {
	private final String text;

	public NumericLiteralAstNode(SourceLocation location, String text) {
		super(location);
		this.text = text;
	}

	/**
	 * Returns the literal exactly as it appeared in the query.
	 */
	public String text() {
		return text;
	}

	@Override
	public <R> R accept(AstVisitor<R> visitor) {
		return visitor.visit(this);
	}

	@Override
	public String toString() {
		return text;
	}
}
