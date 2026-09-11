package net.thisptr.jackson.jq.v2.core.internal.ast;

import org.jspecify.annotations.Nullable;


public class VariableAccessAstNode implements AstNode {
	private final String name;
	private final @Nullable String moduleName;

	public VariableAccessAstNode(@Nullable String moduleName, String name) {
		this.moduleName = moduleName;
		this.name = name;
	}

	public VariableAccessAstNode(String name) {
		this(null, name);
	}

	public String name() {
		return name;
	}

	public @Nullable String moduleName() {
		return moduleName;
	}

	@Override
	public <R> R accept(AstVisitor<R> visitor) {
		return visitor.visit(this);
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
