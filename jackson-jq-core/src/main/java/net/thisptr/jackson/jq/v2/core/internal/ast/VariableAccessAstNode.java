package net.thisptr.jackson.jq.v2.core.internal.ast;

public class VariableAccessAstNode implements AstNode {
	private final String name;
	private final String moduleName;

	public VariableAccessAstNode(String moduleName, String name) {
		this.moduleName = moduleName;
		this.name = name;
	}

	public String name() {
		return name;
	}

	public String moduleName() {
		return moduleName;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append('$');
		if (moduleName != null) {
			s.append(moduleName);
			s.append("::");
		}
		s.append(name);
		return s.toString();
	}
}
