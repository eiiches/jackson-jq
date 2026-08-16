package net.thisptr.jackson.jq.v2.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.internal.javacc.ExpressionParser;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.FunctionLoader;
import net.thisptr.jackson.jq.v2.spi.FunctionNameAndArity;
import net.thisptr.jackson.jq.v2.spi.JqLibrary;
import net.thisptr.jackson.jq.v2.spi.JqLibrary.JqFunc;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.VersionRange;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

/**
 * Use {@code BuiltinFunctionLoader.getInstance()} to obtain the instance.
 */
public class BuiltinFunctionLoader implements FunctionLoader {
	private static final BuiltinFunctionLoader INSTANCE = new BuiltinFunctionLoader();

	public static BuiltinFunctionLoader getInstance() {
		return INSTANCE;
	}

	/**
	 * Load function definitions from the available providers
	 * from an arbitrary {@link ClassLoader}.
	 * E.g. in an OSGi context this may be the Bundle's {@link ClassLoader}.
	 */
	@Override
	public Map<FunctionNameAndArity, FunctionFactory> listFunctionFactories(Version version) {
		Map<FunctionNameAndArity, FunctionFactory> result = new HashMap<>();

		for (FunctionFactory factory : ServiceLoader.load(FunctionFactory.class, BuiltinFunctionLoader.class.getClassLoader())) {
			FunctionRegistration[] regs = factory.getClass().getAnnotationsByType(FunctionRegistration.class);
			for (FunctionRegistration reg : regs) {
				VersionRange versionRange = VersionRange.valueOf(reg.version());
				if (!versionRange.contains(version))
					continue;

				result.put(FunctionNameAndArity.of(reg.name(), reg.nargs()), factory);
			}
		}

		for (JqLibrary library : ServiceLoader.load(JqLibrary.class, BuiltinFunctionLoader.class.getClassLoader())) {
			for (JqFunc def : library.getFunctions()) {
				if (def.version != null && !def.version.contains(version))
					continue;
				result.put(FunctionNameAndArity.of(def.name, def.args.size()), createJqFunctionFactory(def, version));
			}
		}

		return result;
	}

	@Deprecated
	public void loadFunctions(Version version, Scope<?> scope) {
	}

	private FunctionFactory createJqFunctionFactory(JqFunc def, Version version) {
		Expression parsedBody = ExpressionParser.compile(def.body, version);
		return new FunctionFactory() {
			private @Nullable Expression resolvedBody;

			@SuppressWarnings({"unchecked", "rawtypes"})
			private synchronized Expression getResolvedBody(Scope<?> scope) {
				if (resolvedBody != null)
					return resolvedBody;
				try {
					net.thisptr.jackson.jq.v2.core.internal.compile.CompileContext context = new net.thisptr.jackson.jq.v2.core.internal.compile.CompileContext();
					context.pushFunctionScope();
					for (String arg : def.args) {
						if (arg.startsWith("$")) {
							context.addLocalVariable(arg.substring(1));
						} else {
							context.addLocalFunction(arg, 0);
						}
					}
					Environment env = new Environment((net.thisptr.jackson.jq.v2.json.JsonProvider) scope.jsonProvider(), version);
					listFunctionFactories(version).forEach(env::addFunctionFactory);
					resolvedBody = net.thisptr.jackson.jq.v2.core.internal.compile.AstResolver.resolveNonNull(env, context, parsedBody);
				} catch (Exception e) {
					e.printStackTrace();
					resolvedBody = parsedBody;
				}
				return resolvedBody;
			}

			@Override
			@SuppressWarnings({"unchecked", "rawtypes"})
			public <N> Function<N> createFunction(net.thisptr.jackson.jq.v2.json.JsonProvider<N> jsonProvider, List<Expression> args, Version v) {
				return (runtimeScope, in, path, output) -> {
					Expression body = getResolvedBody(runtimeScope);
					int fnSize = def.args.size();
					ExecutionStack<N>.Frame parentFrame = runtimeScope.getExecutionFrame();
					ExecutionStack<N>.Frame fnFrame = parentFrame != null
							? parentFrame.getStack().pushFrame(parentFrame, fnSize)
							: new ExecutionStack<N>().pushFrame(parentFrame, fnSize);
					Scope<N> fnScope = Scope.newChildScopeWithFrame(runtimeScope, fnFrame);
					try {
						bindAndApply(runtimeScope, fnScope, def.args, args, 0, in, path, output, (execScope) -> {
							body.apply(execScope, in, path, output, false);
						});
					} finally {
						fnFrame.getStack().popFrame();
					}
				};
			}
		};
	}

	private <N> void bindAndApply(Scope<N> scope, Scope<N> currentScope, List<String> paramNames, List<Expression> args, int index, N in, net.thisptr.jackson.jq.v2.spi.path.@Nullable Path<N> path, net.thisptr.jackson.jq.v2.spi.PathOutput<N> output, java.util.function.Consumer<Scope<N>> bodyTask) throws net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException {
		for (int i = 0; i < paramNames.size(); i++) {
			String pName = paramNames.get(i);
			Expression pExpr = args.get(i);
			if (!pName.startsWith("$")) {
				currentScope.setFunctionFactory(i, new FunctionFactory() {
					@Override
					@SuppressWarnings({"unchecked", "rawtypes"})
					public <N1> Function<N1> createFunction(net.thisptr.jackson.jq.v2.json.JsonProvider<N1> jp, List<Expression> emptyArgs, Version ver) {
						return (s, inVal, pVal, outVal) -> pExpr.apply((Scope) scope, inVal, pVal, outVal, false);
					}
				});
			}
		}
		bindValueParams(scope, currentScope, paramNames, args, 0, in, path, output, bodyTask);
	}

	private <N> void bindValueParams(Scope<N> callerScope, Scope<N> currentScope, List<String> paramNames, List<Expression> args, int index, N in, net.thisptr.jackson.jq.v2.spi.path.@Nullable Path<N> path, net.thisptr.jackson.jq.v2.spi.PathOutput<N> output, java.util.function.Consumer<Scope<N>> bodyTask) throws net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException {
		if (index >= paramNames.size()) {
			bodyTask.accept(currentScope);
			return;
		}
		String argName = paramNames.get(index);
		Expression argExpr = args.get(index);
		if (argName.startsWith("$")) {
			int slot = index;
			argExpr.apply(callerScope, in, path, (val, p) -> {
				currentScope.setValue(slot, val, paramNames.size());
				bindValueParams(callerScope, currentScope, paramNames, args, index + 1, in, path, output, bodyTask);
			}, false);
		} else {
			bindValueParams(callerScope, currentScope, paramNames, args, index + 1, in, path, output, bodyTask);
		}
	}
}
