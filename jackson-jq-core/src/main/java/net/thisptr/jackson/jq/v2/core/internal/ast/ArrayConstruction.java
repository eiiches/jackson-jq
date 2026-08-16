package net.thisptr.jackson.jq.v2.core.internal.ast;

import org.jspecify.annotations.Nullable;

public class ArrayConstruction implements AstNode {
	public final @Nullable AstNode q;

	public ArrayConstruction() {
		this(null);
	}

	public ArrayConstruction(@Nullable AstNode q) {
		this.q = q;
	}

	@Override
	public String toString() {
		if (q == null)
			return "[]";
		return String.format("[%s]", q);
	}
}
