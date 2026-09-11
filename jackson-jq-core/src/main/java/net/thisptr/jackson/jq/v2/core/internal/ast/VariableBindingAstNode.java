package net.thisptr.jackson.jq.v2.core.internal.ast;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;

public class VariableBindingAstNode extends AbstractAstNode {
	private final AstNode value;
	private final PatternMatcherAstNode matcher;
	private final AstNode body;

	public VariableBindingAstNode(SourceLocation location, AstNode value, PatternMatcherAstNode matcher, AstNode body) {
		super(location);
		this.value = value;
		this.matcher = matcher;
		this.body = body;
	}

	public AstNode value() {
		return value;
	}

	public PatternMatcherAstNode matcher() {
		return matcher;
	}

	public AstNode body() {
		return body;
	}

	@Override
	public <R> R accept(AstVisitor<R> visitor) {
		return visitor.visit(this);
	}

	@Override
	public String toString() {
		return value + " as " + matcher + " | " + body;
	}
}
