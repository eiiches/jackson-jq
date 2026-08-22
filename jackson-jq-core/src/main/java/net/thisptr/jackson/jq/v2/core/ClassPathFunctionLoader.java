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
import net.thisptr.jackson.jq.v2.internal.javacc.AstParser;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.JqLibrary;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.StackMemory;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.VersionRange;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

/**
 * Use {@code BuiltinFunctionLoader.getInstance()} to obtain the instance.
 */
public class ClassPathFunctionLoader implements FunctionLoader {
	private static final ClassPathFunctionLoader INSTANCE = new ClassPathFunctionLoader(ClassPathFunctionLoader.class.getClassLoader());

	private final ClassLoader classLoader;

	public static ClassPathFunctionLoader getInstance() {
		return INSTANCE;
	}

	public ClassPathFunctionLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * Load function definitions from the available providers
	 * from an arbitrary {@link ClassLoader}.
	 * E.g. in an OSGi context this may be the Bundle's {@link ClassLoader}.
	 */
	@Override
	public Map<FunctionSignature, Function> getFunctions(Version jqVersion) {
		Map<FunctionSignature, Function> result = new HashMap<>();

		for (Function factory : ServiceLoader.load(Function.class, classLoader)) {
			FunctionRegistration[] regs = factory.getClass().getAnnotationsByType(FunctionRegistration.class);
			for (FunctionRegistration reg : regs) {
				VersionRange versionRange = VersionRange.valueOf(reg.version());
				if (!versionRange.contains(jqVersion))
					continue;

				result.put(FunctionSignature.of(reg.name(), reg.nargs()), factory);
			}
		}

		for (JqLibrary library : ServiceLoader.load(JqLibrary.class, classLoader)) {
			for (JqLibrary.JqFunc def : library.getFunctions()) {
				if (def.version != null && !def.version.contains(jqVersion))
					continue;
				result.put(FunctionSignature.of(def.name, def.args.size()), createJqFunction(def, jqVersion));
			}
		}

		return result;
	}

	private static final class ResolvedFunction<N> {
		final Expression<N> body;
		final int fnSize;

		ResolvedFunction(Expression<N> body, int fnSize) {
			this.body = body;
			this.fnSize = fnSize;
		}
	}

	private Function createJqFunction(JqLibrary.JqFunc def, Version version) {
		AstNode parsedAst = AstParser.parse(def.body, version);
		return new Function() {
			private final IdentityHashMap<JsonProvider<?>, ResolvedFunction<?>> resolvedFunctions = new IdentityHashMap<>();

			@SuppressWarnings("unchecked")
			private synchronized <N> ResolvedFunction<N> getResolvedFunction(JsonProvider<N> jsonProvider) throws JsonQueryException {
				ResolvedFunction<N> cached = (ResolvedFunction<N>) resolvedFunctions.get(jsonProvider);
				if (cached != null)
					return cached;
				CompileContext context = new CompileContext();
				context.pushFunctionScope();
				for (String arg : def.args) {
					if (arg.startsWith("$")) {
						context.addLocalVariable(arg.substring(1));
					} else {
						context.addLocalFunction(arg, 0);
					}
				}
				Environment<N> env = new EnvironmentBuilder<>(jsonProvider, version)
						.setFunctionLoader(ClassPathFunctionLoader.this)
						.build();
				Expression<N> resolvedBody = Compiler.compileNonNull(env, context, parsedAst);
				// fnSize must be read after the body is compiled, not before -- otherwise it misses any
				// locals (`as`/`reduce`/`foreach` bindings, nested `def`s) the body itself introduces.
				int fnSize = context.getSlotCount();
				ResolvedFunction<N> resolved = new ResolvedFunction<>(resolvedBody, fnSize);
				resolvedFunctions.put(jsonProvider, resolved);
				return resolved;
			}

			@Override
			public <N> Expression<N> bindArguments(JsonProvider<N> jsonProvider, List<Expression<N>> args, Version v) {
				return (callerFrame, in, path, output) -> {
					ResolvedFunction<N> resolved = getResolvedFunction(jsonProvider);
					StackFrame fnFrame = callerFrame != null
							? callerFrame.getEnclosingMemory().pushFrame(resolved.fnSize)
							: new StackMemory().pushFrame(resolved.fnSize);
					try {
						bindAndApply(callerFrame, fnFrame, def.args, args, in, path, output, (execFrame) -> {
							resolved.body.apply(execFrame, in, path, output);
						});
					} finally {
						fnFrame.getEnclosingMemory().popFrame();
					}
				};
			}
		};
	}

	private <N> void bindAndApply(@Nullable StackFrame callerFrame, StackFrame currentFrame, List<String> paramNames, List<Expression<N>> args, N in, @Nullable Path<N> path, Output<N> output, Consumer<StackFrame> bodyTask) throws JsonQueryException {
		for (int i = 0; i < paramNames.size(); i++) {
			String pName = paramNames.get(i);
			Expression<N> pExpr = args.get(i);
			if (!pName.startsWith("$")) {
				currentFrame.set(i, new Function() {
					@Override
					@SuppressWarnings("unchecked")
					public <N1> Expression<N1> bindArguments(JsonProvider<N1> jp, List<Expression<N1>> emptyArgs, Version ver) {
						Expression<N1> effectiveExpr = (Expression<N1>) (Expression<?>) pExpr;
						StackFrame effectiveCallerFrame = (StackFrame) (Object) callerFrame;
						return (sFrame, inVal, pVal, outVal) -> effectiveExpr.apply(effectiveCallerFrame, inVal, pVal, outVal);
					}
				});
			}
		}
		bindValueParams(callerFrame, currentFrame, paramNames, args, 0, in, path, output, bodyTask);
	}

	private <N> void bindValueParams(@Nullable StackFrame callerFrame, StackFrame currentFrame, List<String> paramNames, List<Expression<N>> args, int index, N in, @Nullable Path<N> path, Output<N> output, Consumer<StackFrame> bodyTask) throws JsonQueryException {
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
			});
		} else {
			bindValueParams(callerFrame, currentFrame, paramNames, args, index + 1, in, path, output, bodyTask);
		}
	}
}
