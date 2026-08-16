package net.thisptr.jackson.jq.v2.core.internal.ast;

import java.util.List;

public class TopLevelExpression<JsonNode> implements AstNode {
	private final List<ImportStatement<JsonNode>> imports;
	private final AstNode expr;
	private final ModuleDirective<JsonNode> moduleDirective;

	public TopLevelExpression(ModuleDirective<JsonNode> moduleDirective, List<ImportStatement<JsonNode>> imports, AstNode expr) {
		this.moduleDirective = moduleDirective;
		this.imports = imports;
		this.expr = expr;
	}

	public ModuleDirective<JsonNode> moduleDirective() {
		return moduleDirective;
	}

	public List<ImportStatement<JsonNode>> imports() {
		return imports;
	}

	public AstNode expr() {
		return expr;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		if (moduleDirective != null) {
			s.append(moduleDirective);
			s.append("; ");
		}
		for (ImportStatement<JsonNode> imp : imports) {
			s.append(imp);
			s.append("; ");
		}
		s.append(expr);
		return s.toString();
	}
}
