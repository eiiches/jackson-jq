package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.List;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class FunctionCall implements Expression {
	private final String name;
	private final List<Expression> args;
	private final Version version;
	private final @Nullable String moduleName;

	public FunctionCall(@Nullable String moduleName, String name, List<Expression> args, Version version) {
		this.moduleName = moduleName;
		this.name = name;
		this.args = args;
		this.version = version;
	}

	private <JsonNode> Function lookupFunction(Scope<JsonNode> scope) throws JsonQueryException {
		if (moduleName != null) {
			for (Module module : scope.getImportedModules(moduleName)) {
				Function f = module.getFunction(name, args.size());
				if (f != null)
					return f;
			}
			throw new JsonQueryException(String.format("Function %s::%s/%s does not exist", moduleName, name, args.size()));
		} else {
			Function f = scope.getFunction(name, args.size());
			if (f != null)
				return f;

			// search functions loaded by "include" statement
			for (Module module : scope.getImportedModules(null)) {
				Function g = module.getFunction(name, args.size());
				if (g != null)
					return g;
			}

			throw new JsonQueryException(String.format("Function %s/%s does not exist", name, args.size()));
		}
	}

	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		Function f = lookupFunction(scope);
		f.apply(scope, args, in, path, output, version);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (moduleName != null) {
			builder.append(moduleName);
			builder.append("::");
		}
		builder.append(name);
		if (!args.isEmpty()) {
			builder.append("(");
			@Var String sep = "";
			for (Expression arg : args) {
				builder.append(sep);
				if (arg == null) {
					builder.append("null");
				} else {
					builder.append(arg.toString());
				}
				sep = "; ";
			}
			builder.append(")");
		}
		return builder.toString();
	}
}
