package net.thisptr.jackson.jq.v2.core.internal.ast;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;

/**
 * The {@code f as $x} head of a pipe. The body it scopes is the right-hand side of the
 * {@link PipeAstNode} it heads, not a child of its own.
 */
public class AsBindingAstNode extends AbstractAstNode {
	private final AstNode value;
	private final PatternMatcherAstNode matcher;

	public AsBindingAstNode(SourceLocation location, AstNode value, PatternMatcherAstNode matcher) {
		super(location);
		this.value = value;
		this.matcher = matcher;
	}

	public AstNode value() {
		return value;
	}

	public PatternMatcherAstNode matcher() {
		return matcher;
	}

	@Override
	public <R> R accept(AstVisitor<R> visitor) {
		return visitor.visit(this);
	}

	@Override
	public String toString() {
		return value + " as " + matcher;
	}
}
