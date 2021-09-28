package net.thisptr.jackson.jq.internal.tree;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.module.Module;
import net.thisptr.jackson.jq.path.Path;

public class FunctionCall implements Expression {
	private final String name;
	private final List<Expression> args;
	private final Version version;
	private final String moduleName;

	public FunctionCall(final String moduleName, final String name, final List<Expression> args, final Version version) {
		this.moduleName = moduleName;
		this.name = name;
		this.args = args;
		this.version = version;
	}

	private Function lookupFunction(final Scope scope) throws JsonQueryException {
		if (moduleName != null) {
			for (final Module module : scope.getImportedModules(moduleName)) {
				final Function f = module.getFunction(name, args.size());
				if (f != null)
					return f;
			}
			throw new JsonQueryException(String.format("Function %s::%s/%s does not exist", moduleName, name, args.size()));
		} else {
			final Function f = scope.getFunction(name, args.size());
			if (f != null)
				return f;

			// search functions loaded by "include" statement
			for (final Module module : scope.getImportedModules(null)) {
				final Function g = module.getFunction(name, args.size());
				if (g != null)
					return g;
			}

			throw new JsonQueryException(String.format("Function %s/%s does not exist", name, args.size()));
		}
	}

	@Override
	public void apply(Scope scope, JsonNode in, Path path, PathOutput output, final boolean requirePath) throws JsonQueryException {
		final Function f = lookupFunction(scope);
		f.apply(scope, args, in, path, output, version);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		if (moduleName != null) {
			builder.append(moduleName);
			builder.append("::");
		}
		builder.append(name);
		if (!args.isEmpty()) {
			builder.append("(");
			String sep = "";
			for (final Expression arg : args) {
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
