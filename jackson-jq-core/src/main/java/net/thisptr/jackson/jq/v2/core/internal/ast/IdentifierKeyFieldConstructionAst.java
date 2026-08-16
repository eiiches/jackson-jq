package net.thisptr.jackson.jq.v2.core.internal.ast;

import org.jspecify.annotations.Nullable;

public class IdentifierKeyFieldConstructionAst implements FieldConstructionAst {
	public final String key;
	public final @Nullable AstNode value;

	public IdentifierKeyFieldConstructionAst(String key, @Nullable AstNode value) {
		this.key = key;
		this.value = value;
	}

	public IdentifierKeyFieldConstructionAst(String key) {
		this(key, null);
	}

	@Override
	public String toString() {
		if (value == null) {
			return key;
		} else {
			return key + ": " + value.toString();
		}
	}
}
