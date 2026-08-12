package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.List;

import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.module.ModuleLoader;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class TopLevelExpression<JsonNode> implements Expression<JsonNode> {
	private final List<ImportStatement<JsonNode>> imports;
	private final Expression<JsonNode> expr;
	private final ModuleDirective<JsonNode> moduleDirective;

	public TopLevelExpression(ModuleDirective<JsonNode> moduleDirective, List<ImportStatement<JsonNode>> imports, Expression<JsonNode> expr) {
		this.moduleDirective = moduleDirective;
		this.imports = imports;
		this.expr = expr;
	}

	@Override
	public void apply(Scope<JsonNode> scope, JsonNode in, Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
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
