package net.thisptr.jackson.jq.v2.core.internal.ast;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;


public class VariableAccessAstNode extends AbstractAstNode {
	private final String name;
	private final @Nullable String moduleName;

	public VariableAccessAstNode(SourceLocation location, @Nullable String moduleName, String name) {
		super(location);
		this.moduleName = moduleName;
		this.name = name;
	}

	public VariableAccessAstNode(SourceLocation location, String name) {
		this(location, null, name);
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
