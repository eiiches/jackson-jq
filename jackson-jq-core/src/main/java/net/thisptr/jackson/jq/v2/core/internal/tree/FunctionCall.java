package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.List;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
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

	public String name() {
		return name;
	}

	public List<Expression> args() {
		return args;
	}

	public @Nullable String moduleName() {
		return moduleName;
	}

	private <JsonNode> Function<JsonNode> lookupFunction(Scope<JsonNode> scope) throws JsonQueryException {
		if (moduleName != null) {
			for (Module module : scope.getImportedModules(moduleName)) {
				FunctionFactory f = module.getFunction(name, args.size());
				if (f != null)
					return f.createFunction(scope.jsonProvider(), args, version);
			}
			throw new JsonQueryException(String.format("Function %s::%s/%s does not exist", moduleName, name, args.size()));
		} else {
			FunctionFactory f = scope.getFunctionFactory(name, args.size());
			if (f != null)
				return f.createFunction(scope.jsonProvider(), args, version);

			// search functions loaded by "include" statement
			for (Module module : scope.getImportedModules(null)) {
				FunctionFactory g = module.getFunction(name, args.size());
				if (g != null)
					return g.createFunction(scope.jsonProvider(), args, version);
			}

			throw new JsonQueryException(String.format("Function %s/%s does not exist", name, args.size()));
		}
	}

	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		Function<JsonNode> f = lookupFunction(scope);
		f.apply(scope, in, path, output);
	}

	@Override
	public <JsonNode> void apply(JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		throw new UnsupportedOperationException("FunctionCall requires symbol resolution");
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
