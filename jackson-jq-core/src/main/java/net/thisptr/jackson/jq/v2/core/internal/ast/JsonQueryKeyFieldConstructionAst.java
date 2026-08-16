package net.thisptr.jackson.jq.v2.core.internal.ast;

public class JsonQueryKeyFieldConstructionAst implements FieldConstructionAst {
	private final AstNode key;
	private final AstNode value;

	public JsonQueryKeyFieldConstructionAst(AstNode key, AstNode value) {
		this.key = key;
		this.value = value;
	}

	public AstNode key() {
		return key;
	}

	public AstNode value() {
		return value;
	}

	@Override
	public String toString() {
		String result = "(" + key.toString() + ")";
		return result + ": " + value;
	}
}
