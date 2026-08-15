package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.List;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class FunctionDefinition implements Expression {
	private Expression body;
	private String fname;
	private List<String> args;

	public FunctionDefinition(String fname, List<String> args, Expression body) {
		this.fname = fname;
		this.args = args;
		this.body = body;
	}

	public String fname() {
		return fname;
	}

	public List<String> args() {
		return args;
	}

	public Expression body() {
		return body;
	}

	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		scope.addFunctionFactory(fname, args.size(), new net.thisptr.jackson.jq.v2.spi.FunctionFactory() {
			@Override
			public <N> net.thisptr.jackson.jq.v2.spi.Function<N> createFunction(net.thisptr.jackson.jq.v2.json.JsonProvider<N> jsonProvider, List<Expression> fnArgs, net.thisptr.jackson.jq.v2.spi.Version version) {
				return (runtimeScope, input, path, out) -> {
					Scope<N> fnScope = Scope.newChildScope((Scope) scope);
					bindAndApply(runtimeScope, fnScope, args, fnArgs, 0, input, path, out, (execScope) -> {
						body.apply(execScope, input, path, out, false);
					});
				};
			}
		});
	}

	private static <N> void bindAndApply(Scope<N> callerScope, Scope<N> currentScope, List<String> paramNames, List<Expression> fnArgs, int index, N in, @Nullable Path<N> path, PathOutput<N> output, java.util.function.Consumer<Scope<N>> bodyTask) throws JsonQueryException {
		for (int i = 0; i < paramNames.size(); i++) {
			String pName = paramNames.get(i);
			Expression pExpr = fnArgs.get(i);
			if (!pName.startsWith("$")) {
				currentScope.addFunctionFactory(pName, 0, new net.thisptr.jackson.jq.v2.spi.FunctionFactory() {
					@Override
					@SuppressWarnings({"unchecked", "rawtypes"})
					public <N1> net.thisptr.jackson.jq.v2.spi.Function<N1> createFunction(net.thisptr.jackson.jq.v2.json.JsonProvider<N1> jp, List<Expression> emptyArgs, net.thisptr.jackson.jq.v2.spi.Version v) {
						return (s, inVal, pVal, outVal) -> pExpr.apply((Scope) callerScope, inVal, pVal, outVal, false);
					}
				});
			}
		}
		bindValueParams(callerScope, currentScope, paramNames, fnArgs, 0, in, path, output, bodyTask);
	}

	private static <N> void bindValueParams(Scope<N> callerScope, Scope<N> currentScope, List<String> paramNames, List<Expression> fnArgs, int index, N in, @Nullable Path<N> path, PathOutput<N> output, java.util.function.Consumer<Scope<N>> bodyTask) throws JsonQueryException {
		if (index >= paramNames.size()) {
			bodyTask.accept(currentScope);
			return;
		}
		String argName = paramNames.get(index);
		Expression argExpr = fnArgs.get(index);
		if (argName.startsWith("$")) {
			String varName = argName.substring(1);
			int slot = index;
			argExpr.apply(callerScope, in, path, (val, p) -> {
				Scope<N> valScope = Scope.newChildScope(currentScope);
				valScope.setValue(slot, val);
				bindValueParams(callerScope, valScope, paramNames, fnArgs, index + 1, in, path, output, bodyTask);
			}, false);
		} else {
			bindValueParams(callerScope, currentScope, paramNames, fnArgs, index + 1, in, path, output, bodyTask);
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("def ");
		builder.append(fname);
		if (!args.isEmpty()) {
			builder.append("(");
			@Var String sep = "";
			for (String arg : args) {
				builder.append(sep);
				builder.append(arg);
				sep = "; ";
			}
			builder.append(")");
		}
		builder.append(": ");
		builder.append(body);
		return builder.toString();
	}
}
