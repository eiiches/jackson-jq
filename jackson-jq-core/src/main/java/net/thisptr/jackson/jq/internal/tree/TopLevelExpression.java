package net.thisptr.jackson.jq.internal.tree;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.module.Module;
import net.thisptr.jackson.jq.module.ModuleLoader;
import net.thisptr.jackson.jq.path.Path;

public class TopLevelExpression implements Expression {
	private final List<ImportStatement> imports;
	private final Expression expr;
	private final ModuleDirective moduleDirective;

	public TopLevelExpression(final ModuleDirective moduleDirective, final List<ImportStatement> imports, final Expression expr) {
		this.moduleDirective = moduleDirective;
		this.imports = imports;
		this.expr = expr;
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Path ipath, final PathOutput output, final boolean requirePath) throws JsonQueryException {
		final ModuleLoader moduleLoader = scope.getModuleLoader();

		for (final ImportStatement imp : imports) {
			if (!imp.dollarImport) {
				final Module module = moduleLoader.loadModule(scope.getCurrentModule(), imp.path, imp.metadata);
				if (module == null)
					throw new JsonQueryException("module not found: " + imp.path);
				scope.addImportedModule(imp.name, module);
			} else {
				final JsonNode data = moduleLoader.loadData(scope.getCurrentModule(), imp.path, imp.metadata);
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
		for (final ImportStatement imp : imports) {
			s.append(imp);
			s.append("; ");
		}
		s.append(expr);
		return s.toString();
	}
}
