package net.thisptr.jackson.jq.v2.core.internal.ast;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;

public class FormattingFilterAstNode extends AbstractAstNode {
	private final FunctionSignature signature;

	public FormattingFilterAstNode(SourceLocation location, FunctionSignature signature) {
		super(location);
		this.signature = signature;
	}

	public FunctionSignature signature() {
		return signature;
	}

	@Override
	public <R> R accept(AstVisitor<R> visitor) {
		return visitor.visit(this);
	}

	@Override
	public String toString() {
		return signature.name();
	}
}
