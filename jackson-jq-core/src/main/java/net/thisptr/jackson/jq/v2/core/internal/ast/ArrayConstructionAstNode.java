package net.thisptr.jackson.jq.v2.core.internal.ast;

import org.jspecify.annotations.Nullable;


public class ArrayConstructionAstNode implements AstNode {
	public final @Nullable AstNode q;

	public ArrayConstructionAstNode() {
		this(null);
	}

	public ArrayConstructionAstNode(@Nullable AstNode q) {
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
