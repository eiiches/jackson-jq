package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.ast.TopLevelAstNode;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class TopLevelExpression<JsonNode> implements Expression {
	private final List<TopLevelAstNode.ImportStatement<JsonNode>> imports;
	private final Expression expr;
	private final TopLevelAstNode.ModuleDirective<JsonNode> moduleDirective;

	public TopLevelExpression(TopLevelAstNode.ModuleDirective<JsonNode> moduleDirective, List<TopLevelAstNode.ImportStatement<JsonNode>> imports, Expression expr) {
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

	public Expression expr() {
		return expr;
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public <N> void apply(JsonProvider<N> jsonProvider, ExecutionStack<N>.@Nullable Frame frame, N in, @Nullable Path<N> ipath, PathOutput<N> output, boolean requirePath) throws JsonQueryException {
		applyInternal((JsonProvider) jsonProvider, (ExecutionStack.Frame) frame, (JsonNode) in, (Path) ipath, (PathOutput) output, requirePath);
	}

	private void applyInternal(JsonProvider<JsonNode> jsonProvider, ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		expr.apply(jsonProvider, frame, in, ipath, output, requirePath);
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
