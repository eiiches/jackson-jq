package net.thisptr.jackson.jq.v2.core.internal.ast.impls;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;

public class ArrayConstructionAstNode implements AstNode {
	public final @Nullable AstNode q;

	public ArrayConstructionAstNode() {
		this(null);
	}

	public ArrayConstructionAstNode(@Nullable AstNode q) {
		this.q = q;
	}

	@Override
	public String toString() {
		if (q == null)
			return "[]";
		return String.format("[%s]", q);
	}
}
