package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.ast.impls.TopLevelAstNode;
import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class TopLevelExpression<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	private final List<TopLevelAstNode.ImportStatement> imports;
	private final Expression<StackFrame, JsonNode> expr;
	private final TopLevelAstNode.@Nullable ModuleDirective moduleDirective;

	@Override
	public Cardinality getCardinality() {
		return expr.getCardinality();
	}

	public TopLevelExpression(TopLevelAstNode.@Nullable ModuleDirective moduleDirective, List<TopLevelAstNode.ImportStatement> imports, Expression<StackFrame, JsonNode> expr) {
		this.moduleDirective = moduleDirective;
		this.imports = imports;
		this.expr = expr;
	}

	public TopLevelAstNode.@Nullable ModuleDirective moduleDirective() {
		return moduleDirective;
	}

	public List<TopLevelAstNode.ImportStatement> imports() {
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
		for (TopLevelAstNode.ImportStatement imp : imports) {
			s.append(imp);
			s.append("; ");
		}
		s.append(expr);
		return s.toString();
	}
}
