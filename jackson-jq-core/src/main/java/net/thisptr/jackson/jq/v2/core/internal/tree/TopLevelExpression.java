package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.module.ModuleLoader;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class TopLevelExpression<JsonNode> implements Expression {
	private final List<ImportStatement<JsonNode>> imports;
	private final Expression expr;
	private final ModuleDirective<JsonNode> moduleDirective;

	public TopLevelExpression(ModuleDirective<JsonNode> moduleDirective, List<ImportStatement<JsonNode>> imports, Expression expr) {
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

	public Expression expr() {
		return expr;
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public <N> void apply(Scope<N> scope, N in, @Nullable Path<N> ipath, PathOutput<N> output, boolean requirePath) throws JsonQueryException {
		applyInternal((Scope) scope, (JsonNode) in, (Path) ipath, (PathOutput) output, requirePath);
	}

	private void applyInternal(Scope<JsonNode> scope, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		ModuleLoader<JsonNode> moduleLoader = scope.getModuleLoader();

		for (ImportStatement<JsonNode> imp : imports) {
			if (!imp.dollarImport) {
				Module module = moduleLoader.loadModule(scope.getCurrentModule(), imp.path, imp.getMetadata(scope.jsonProvider()));
				if (module == null)
					throw new JsonQueryException("module not found: " + imp.path);
				scope.addImportedModule(imp.name, module);
			} else {
				JsonNode data = moduleLoader.loadData(scope.getCurrentModule(), imp.path, imp.getMetadata(scope.jsonProvider()));
				if (data == null)
					throw new JsonQueryException("module not found: " + imp.path);
				scope.setImportedData(imp.name, data);
			}
		}

		expr.apply(scope, in, ipath, output, requirePath);
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
