package net.thisptr.jackson.jq.v2.core.internal.ast;

import org.jspecify.annotations.Nullable;

public class StringKeyFieldConstructionAst implements FieldConstructionAst {
	public final AstNode key;
	public final @Nullable AstNode value;

	public StringKeyFieldConstructionAst(AstNode key, @Nullable AstNode value) {
		this.key = key;
		this.value = value;
	}

	public StringKeyFieldConstructionAst(AstNode key) {
		this(key, null);
	}

	@Override
	public String toString() {
		if (value == null) {
			return key.toString();
		} else {
			return key.toString() + ": " + value.toString();
		}
	}
}
