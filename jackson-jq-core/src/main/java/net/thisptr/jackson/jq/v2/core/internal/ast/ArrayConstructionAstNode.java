package net.thisptr.jackson.jq.v2.core.internal.ast;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;


public class ArrayConstructionAstNode extends AbstractAstNode {
	public final @Nullable AstNode q;

	public ArrayConstructionAstNode(SourceLocation location) {
		this(location, null);
	}

	public ArrayConstructionAstNode(SourceLocation location, @Nullable AstNode q) {
		super(location);
		this.q = q;
	}

	@Override
	public <R> R accept(AstVisitor<R> visitor) {
		return visitor.visit(this);
	}

	@Override
	public String toString() {
		if (q == null)
			return "[]";
		return String.format("[%s]", q);
	}
}
