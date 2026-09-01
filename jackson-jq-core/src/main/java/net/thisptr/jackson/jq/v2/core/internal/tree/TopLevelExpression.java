package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.List;
import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.ast.TopLevelAstNode;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class TopLevelExpression<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	private final List<TopLevelAstNode.ImportStatement<JsonNode>> imports;
	private final Expression<StackFrame, JsonNode> expr;
	private final TopLevelAstNode.ModuleDirective<JsonNode> moduleDirective;

	@Override
	public Cardinality getCardinality() {
		return expr.getCardinality();
	}

	public TopLevelExpression(TopLevelAstNode.ModuleDirective<JsonNode> moduleDirective, List<TopLevelAstNode.ImportStatement<JsonNode>> imports, Expression<StackFrame, JsonNode> expr) {
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

	public Expression<StackFrame, JsonNode> expr() {
		return expr;
	}

	@Override
	public boolean dependsOnInput() {
		return expr.dependsOnInput();
	}

	@Override
	public boolean dependsOnExternalState() {
		return expr.dependsOnExternalState();
	}

	@Override
	public Set<Integer> freeLocalSlots() {
		return FreeVariables.union(expr);
	}

	@Override
	public boolean hasOpaqueVariableReference() {
		return FreeVariables.anyOpaque(expr);
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
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
