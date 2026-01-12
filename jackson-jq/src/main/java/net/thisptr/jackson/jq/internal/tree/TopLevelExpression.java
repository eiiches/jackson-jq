package net.thisptr.jackson.jq.internal.tree;

import java.util.List;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.module.Module;
import net.thisptr.jackson.jq.module.ModuleLoader;
import net.thisptr.jackson.jq.path.Path;

public class TopLevelExpression<JsonNode> implements Expression<JsonNode> {
	private final List<ImportStatement<JsonNode>> imports;
	private final Expression<JsonNode> expr;
	private final ModuleDirective<JsonNode> moduleDirective;

	public TopLevelExpression(final ModuleDirective<JsonNode> moduleDirective, final List<ImportStatement<JsonNode>> imports, final Expression<JsonNode> expr) {
		this.moduleDirective = moduleDirective;
		this.imports = imports;
		this.expr = expr;
	}

	@Override
	public void apply(final Scope<JsonNode> scope, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final boolean requirePath) throws JsonQueryException {
		final ModuleLoader<JsonNode> moduleLoader = scope.getModuleLoader();

		for (final ImportStatement<JsonNode> imp : imports) {
			if (!imp.dollarImport) {
				final Module<JsonNode> module = moduleLoader.loadModule(scope.getCurrentModule(), imp.path, imp.getMetadata(scope.jsonProvider()));
				if (module == null)
					throw new JsonQueryException("module not found: " + imp.path);
				scope.addImportedModule(imp.name, module);
			} else {
				final JsonNode data = moduleLoader.loadData(scope.getCurrentModule(), imp.path, imp.getMetadata(scope.jsonProvider()));
				if (data == null)
					throw new JsonQueryException("module not found: " + imp.path);
				scope.setImportedData(imp.name, data);
			}
		}

		expr.apply(scope, in, ipath, output, requirePath);
	}

	@Override
	public String toString() {
		final StringBuilder s = new StringBuilder();
		if (moduleDirective != null) {
			s.append(moduleDirective);
			s.append("; ");
		}
		for (final ImportStatement<JsonNode> imp : imports) {
			s.append(imp);
			s.append("; ");
		}
		s.append(expr);
		return s.toString();
	}
}
