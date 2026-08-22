package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.ast.TopLevelAstNode;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class TopLevelExpression<JsonNode> implements Expression<JsonNode> {
	private final List<TopLevelAstNode.ImportStatement<JsonNode>> imports;
	private final Expression<JsonNode> expr;
	private final TopLevelAstNode.ModuleDirective<JsonNode> moduleDirective;

	public TopLevelExpression(TopLevelAstNode.ModuleDirective<JsonNode> moduleDirective, List<TopLevelAstNode.ImportStatement<JsonNode>> imports, Expression<JsonNode> expr) {
		this.moduleDirective = moduleDirective;
		this.imports = imports;
		this.expr = expr;
	}

	public TopLevelAstNode.ModuleDirective<JsonNode> moduleDirective() {
		return moduleDirective;
	}

	public List<TopLevelAstNode.ImportStatement<JsonNode>> imports() {
		return imports;
	}

	public Expression<JsonNode> expr() {
		return expr;
	}

	@Override
	public void apply(@Nullable StackFrame frame, JsonNode in, @Nullable Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
		expr.apply(frame, in, ipath, output);
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		if (moduleDirective != null) {
			s.append(moduleDirective);
			s.append("; ");
		}
		for (TopLevelAstNode.ImportStatement<JsonNode> imp : imports) {
			s.append(imp);
			s.append("; ");
		}
		s.append(expr);
		return s.toString();
	}
}
