package net.thisptr.jackson.jq.v2.core.internal.ast;

public class VariableKeyFieldConstruction implements FieldConstructionAst {
	private final String name;

	public VariableKeyFieldConstruction(String name) {
		this.name = name;
	}

	public String name() {
		return name;
	}

	@Override
	public String toString() {
		return "$" + name;
	}
}
