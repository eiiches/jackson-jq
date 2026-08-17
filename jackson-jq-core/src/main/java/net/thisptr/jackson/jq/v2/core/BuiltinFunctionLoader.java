package net.thisptr.jackson.jq.v2.core;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.compile.CompileContext;
import net.thisptr.jackson.jq.v2.core.internal.compile.Compiler;
import net.thisptr.jackson.jq.v2.internal.javacc.ExpressionParser;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.FunctionLoader;
import net.thisptr.jackson.jq.v2.spi.FunctionNameAndArity;
import net.thisptr.jackson.jq.v2.spi.JqLibrary;
import net.thisptr.jackson.jq.v2.spi.JqLibrary.JqFunc;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.VersionRange;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

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

	private FunctionFactory createJqFunctionFactory(JqFunc def, Version version) {
		AstNode parsedAst = ExpressionParser.compile(def.body, version);
		return new FunctionFactory() {
			private final IdentityHashMap<JsonProvider<?>, Expression<?>> resolvedBodies = new IdentityHashMap<>();

			@SuppressWarnings("unchecked")
			private synchronized <N> Expression<N> getResolvedBody(JsonProvider<N> jsonProvider) throws JsonQueryException {
				Expression<N> cachedBody = (Expression<N>) resolvedBodies.get(jsonProvider);
				if (cachedBody != null)
					return cachedBody;
				CompileContext context = new CompileContext();
				context.pushFunctionScope();
				for (String arg : def.args) {
					if (arg.startsWith("$")) {
						context.addLocalVariable(arg.substring(1));
					} else {
						context.addLocalFunction(arg, 0);
					}
				}
				Environment<N> env = new Environment<>(jsonProvider, version);
				listFunctionFactories(version).forEach(env::addFunctionFactory);
				Expression<N> resolvedBody = Compiler.compileNonNull(env, context, parsedAst);
				resolvedBodies.put(jsonProvider, resolvedBody);
				return resolvedBody;
			}

			@Override
			public <N> Function<N> createFunction(JsonProvider<N> jsonProvider, List<Expression<N>> args, Version v) {
				return (callerFrame, in, path, output) -> {
					Expression<N> body = getResolvedBody(jsonProvider);
					int fnSize = def.args.size();
					StackFrame<N> fnFrame = callerFrame != null
							? callerFrame.getStack().pushFrame(callerFrame, fnSize)
							: new ExecutionStack<N>().pushFrame(callerFrame, fnSize);
					try {
						bindAndApply(callerFrame, fnFrame, def.args, args, in, path, output, (execFrame) -> {
							body.apply(execFrame, in, path, output, false);
						});
					} finally {
						fnFrame.getStack().popFrame();
					}
				};
			}
		};
	}

	private <N> void bindAndApply(@Nullable StackFrame<N> callerFrame, StackFrame<N> currentFrame, List<String> paramNames, List<Expression<N>> args, N in, @Nullable Path<N> path, PathOutput<N> output, Consumer<StackFrame<N>> bodyTask) throws JsonQueryException {
		for (int i = 0; i < paramNames.size(); i++) {
			String pName = paramNames.get(i);
			Expression<N> pExpr = args.get(i);
			if (!pName.startsWith("$")) {
				currentFrame.set(i, new FunctionFactory() {
					@Override
					@SuppressWarnings("unchecked")
					public <N1> Function<N1> createFunction(JsonProvider<N1> jp, List<Expression<N1>> emptyArgs, Version ver) {
						Expression<N1> effectiveExpr = (Expression<N1>) (Expression<?>) pExpr;
						StackFrame<N1> effectiveCallerFrame = (StackFrame<N1>) (Object) callerFrame;
						return (sFrame, inVal, pVal, outVal) -> effectiveExpr.apply(effectiveCallerFrame, inVal, pVal, outVal, false);
					}
				});
			}
		}
		bindValueParams(callerFrame, currentFrame, paramNames, args, 0, in, path, output, bodyTask);
	}

	private <N> void bindValueParams(@Nullable StackFrame<N> callerFrame, StackFrame<N> currentFrame, List<String> paramNames, List<Expression<N>> args, int index, N in, @Nullable Path<N> path, PathOutput<N> output, Consumer<StackFrame<N>> bodyTask) throws JsonQueryException {
		if (index >= paramNames.size()) {
			bodyTask.accept(currentFrame);
			return;
		}
		String argName = paramNames.get(index);
		Expression<N> argExpr = args.get(index);
		if (argName.startsWith("$")) {
			int slot = index;
			argExpr.apply(callerFrame, in, path, (val, p) -> {
				currentFrame.set(slot, val);
				bindValueParams(callerFrame, currentFrame, paramNames, args, index + 1, in, path, output, bodyTask);
			}, false);
		} else {
			bindValueParams(callerFrame, currentFrame, paramNames, args, index + 1, in, path, output, bodyTask);
		}
	}
}
