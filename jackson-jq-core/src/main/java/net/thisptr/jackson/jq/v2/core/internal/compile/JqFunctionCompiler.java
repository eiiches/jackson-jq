package net.thisptr.jackson.jq.v2.core.internal.compile;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.internal.javacc.AstParser;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionParameter;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.JqFunction;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.version.Version;

final class JqFunctionCompiler {
	enum Origin {
		ENVIRONMENT,
		LOADER
	}

	static final class DefinitionKey {
		private final Version version;
		private final FunctionSignature signature;
		private final List<FunctionParameter> parameters;
		private final String body;
		private final Origin origin;

		DefinitionKey(Version version, FunctionSignature signature, JqFunction definition, Origin origin) {
			this.version = version;
			this.signature = signature;
			this.parameters = definition.parameters();
			this.body = definition.body();
			this.origin = origin;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof DefinitionKey))
				return false;
			DefinitionKey other = (DefinitionKey) obj;
			return version.equals(other.version) && signature.equals(other.signature) && parameters.equals(other.parameters) && body.equals(other.body) && origin == other.origin;
		}

		@Override
		public int hashCode() {
			return Objects.hash(version, signature, parameters, body, origin);
		}
	}

	static final class State {
		private final Map<DefinitionKey, CompiledDefinition> definitions = new HashMap<>();

		private synchronized CompiledDefinition get(Environment<?> env, FunctionSignature signature, JqFunction definition, Origin origin) {
			DefinitionKey key = new DefinitionKey(env.getJqVersion(), signature, definition, origin);
			@Var CompiledDefinition compiled = definitions.get(key);
			if (compiled != null)
				return compiled;
			compiled = new CompiledDefinition(key, definition, origin, env.getJqVersion());
			definitions.put(key, compiled);
			return compiled;
		}
	}

	private static final class ResolvedFunction<N> {
		final Expression<StackFrame, N> body;
		final int frameSize;
		// Absolute frame slot of the first param -- 0 for a dedicated-frame body (compileResolvedFunction),
		// or wherever the caller's frame had already grown to for an inlined body
		// (compileResolvedFunctionInline). bindAndApply needs this to translate a param's 0-based index
		// into the correct absolute slot for whichever kind of frame it's writing into.
		final int paramBaseSlot;

		ResolvedFunction(Expression<StackFrame, N> body, int frameSize) {
			this(body, frameSize, 0);
		}

		ResolvedFunction(Expression<StackFrame, N> body, int frameSize, int paramBaseSlot) {
			this.body = body;
			this.frameSize = frameSize;
			this.paramBaseSlot = paramBaseSlot;
		}
	}

	private static final class CompiledDefinition {
		private final DefinitionKey key;
		private final JqFunction definition;
		private final Origin origin;
		private final Version version;
		private final AstNode parsedAst;
		// Conservative, textual eligibility check for the frame-elided inline compile path: a body that
		// never mentions "def " anywhere in its source cannot possibly contain an internal nested def (the
		// parser can't manufacture one from nothing), so this can only ever wrongly *exclude* a body from
		// the fast path (e.g. a stray "def " inside a string literal), never wrongly include one -- internal
		// nested defs (recurse/until/while) are architecturally compatible with inlining too, but are left
		// on the slower, unmodified compileResolvedFunction/bindResolved path for now to keep this change's
		// review surface to the common leaf-shaped case (map/select/add/...).
		private final boolean eligibleForInlining;
		private final IdentityHashMap<JsonProvider<?>, ResolvedFunction<?>> genericFunctions = new IdentityHashMap<>();

		CompiledDefinition(DefinitionKey key, JqFunction definition, Origin origin, Version version) {
			this.key = key;
			this.definition = definition;
			this.origin = origin;
			this.version = version;
			this.parsedAst = AstParser.parse(definition.body(), version);
			this.eligibleForInlining = !definition.body().contains("def ");
		}

		@SuppressWarnings("unchecked")
		synchronized <N> ResolvedFunction<N> getGenericFunction(Environment<N> callingEnvironment, CompileContext context) throws JsonQueryException {
			JsonProvider<N> jsonProvider = callingEnvironment.getJsonProvider();
			ResolvedFunction<N> cached = (ResolvedFunction<N>) genericFunctions.get(jsonProvider);
			if (cached != null)
				return cached;
			ResolvedFunction<N> resolved = compileResolvedFunction(callingEnvironment, null, context);
			genericFunctions.put(jsonProvider, resolved);
			return resolved;
		}

		<N> ResolvedFunction<N> compileResolvedFunction(Environment<N> callingEnvironment, @Nullable List<Expression<StackFrame, N>> boundArguments, CompileContext context) throws JsonQueryException {
			context.pushFunctionScope();
			try {
				bindParamNames(context, boundArguments, 0);
				Environment<N> env = resolveEnvironment(callingEnvironment);
				Expression<StackFrame, N> body = Compiler.compileNonNull(env, context, parsedAst);
				return new ResolvedFunction<>(body, context.getSlotCount());
			} finally {
				context.popScope();
			}
		}

		<N> ResolvedFunction<N> compileResolvedFunctionInline(Environment<N> callingEnvironment, List<Expression<StackFrame, N>> boundArguments, CompileContext context) throws JsonQueryException {
			int baseSlot = context.pushInlinedFunctionScope();
			try {
				bindParamNames(context, boundArguments, baseSlot);
				Environment<N> env = resolveEnvironment(callingEnvironment);
				Expression<StackFrame, N> body = Compiler.compileNonNull(env, context, parsedAst);
				return new ResolvedFunction<>(body, context.getSlotCount(), baseSlot);
			} finally {
				context.popScope();
			}
		}

		private <N> Environment<N> resolveEnvironment(Environment<N> callingEnvironment) {
			return origin == Origin.ENVIRONMENT
					? callingEnvironment
					: new EnvironmentBuilder<>(callingEnvironment.getJsonProvider(), version)
					.setFunctionLoader(callingEnvironment.getFunctionLoader())
					.build();
		}

		// baseSlot is unused by name/slot resolution here -- CompileContext.addLocalVariable/addLocalFunction
		// always assign the *next* slot in the current top scope, which pushFunctionScope()/
		// pushInlinedFunctionScope() already seeded correctly (0, or the caller's high-water mark,
		// respectively) -- it's threaded through only so callers of compileResolvedFunctionInline can be
		// reminded params start there, matching what bindAndApply needs to reconstruct independently.
		private <N> void bindParamNames(CompileContext context, @Nullable List<Expression<StackFrame, N>> boundArguments, int baseSlot) {
			for (int i = 0; i < definition.parameters().size(); i++) {
				FunctionParameter arg = definition.parameters().get(i);
				BoundArgumentInfo info = boundArguments != null
						? new BoundArgumentInfo(boundArguments.get(i), arg.kind() == FunctionParameter.Kind.FILTER || boundArguments.get(i).getCardinality() == Cardinality.ONE)
						: null;
				if (arg.kind() == FunctionParameter.Kind.VALUE) {
					if (info != null)
						context.addLocalVariable(arg.name(), info);
					else
						context.addLocalVariable(arg.name());
				} else if (info != null) {
					context.addLocalFunction(arg.name(), 0, info);
				} else {
					context.addLocalFunction(arg.name(), 0);
				}
			}
		}

	}

	private JqFunctionCompiler() {
	}

	static <N> Expression<StackFrame, N> compile(Environment<N> env, CompileContext context, FunctionSignature signature, JqFunction definition, Origin origin, List<Expression<StackFrame, N>> args) {
		CompiledDefinition compiled = context.jqFunctionState().get(env, signature, definition, origin);
		if (context.isJqFunctionActive(compiled.key))
			return bindFallback(compiled, env, args, context, origin);
		try {
			if (compiled.eligibleForInlining) {
				// Frame-elided fast path: params/locals are spliced into slots of whatever real frame
				// already encloses this call site (see CompileContext#pushInlinedFunctionScope), so no
				// StackFrame is pushed at runtime -- see bindResolvedInline.
				CompileContext inlineContext = context.createInlinedJqFunctionContext(compiled.key, origin == Origin.ENVIRONMENT);
				ResolvedFunction<N> resolved = compiled.compileResolvedFunctionInline(env, args, inlineContext);
				return bindResolvedInline(compiled.definition.parameters(), args, resolved);
			}
			CompileContext functionContext = context.createJqFunctionContext(compiled.key, origin == Origin.ENVIRONMENT);
			ResolvedFunction<N> resolved = compiled.compileResolvedFunction(env, args, functionContext);
			return bindResolved(compiled.definition.parameters(), args, resolved);
		} catch (JsonQueryException ignored) {
			return bindFallback(compiled, env, args, context, origin);
		}
	}

	private static <N> Expression<StackFrame, N> bindFallback(CompiledDefinition definition, Environment<N> env, List<Expression<StackFrame, N>> args, CompileContext context, Origin origin) {
		CompileContext genericContext = context.createGenericJqFunctionContext(definition.key, origin == Origin.ENVIRONMENT);
		if (origin == Origin.ENVIRONMENT && !context.isGenericJqFunctionActive(definition.key))
			return bindResolved(definition.definition.parameters(), args, definition.getGenericFunction(env, genericContext));
		return bindGeneric(definition, env, args, genericContext);
	}

	private static <N> Expression<StackFrame, N> bindGeneric(CompiledDefinition definition, Environment<N> env, List<Expression<StackFrame, N>> args, CompileContext genericContext) {
		return (callerFrame, in, path, output) -> bindResolved(definition.definition.parameters(), args, definition.getGenericFunction(env, genericContext)).apply(callerFrame, in, path, output);
	}

	private static <N> Expression<StackFrame, N> bindResolved(List<FunctionParameter> paramNames, List<Expression<StackFrame, N>> args, ResolvedFunction<N> resolved) {
		return new Expression<StackFrame, N>() {
			@Override
			public Cardinality getCardinality() {
				return resolved.body.getCardinality();
			}

			@Override
			public boolean dependsOnInput() {
				return resolved.body.dependsOnInput();
			}

			@Override
			public boolean dependsOnExternalState() {
				return resolved.body.dependsOnExternalState();
			}

			@Override
			public void apply(StackFrame callerFrame, N in, Path<N> path, Output<N> output) throws JsonQueryException {
				StackFrame functionFrame = callerFrame.getEnclosingMemory().pushFrame(resolved.frameSize);
				try {
					bindAndApply(callerFrame, functionFrame, resolved.paramBaseSlot, paramNames, args, 0, in, path, output, execFrame -> resolved.body.apply(execFrame, in, path, output));
				} finally {
					functionFrame.getEnclosingMemory().popFrame();
				}
			}
		};
	}

	/**
	 * Like {@link #bindResolved}, but for a {@code resolved} body produced by
	 * {@code CompiledDefinition#compileResolvedFunctionInline} -- its params/locals already live at fixed,
	 * baked-in absolute slots in whatever real frame encloses this call site (see
	 * {@link CompileContext#pushInlinedFunctionScope}), so {@code apply()} binds params directly into
	 * {@code callerFrame} instead of pushing a dedicated one.
	 */
	private static <N> Expression<StackFrame, N> bindResolvedInline(List<FunctionParameter> paramNames, List<Expression<StackFrame, N>> args, ResolvedFunction<N> resolved) {
		return new Expression<StackFrame, N>() {
			@Override
			public Cardinality getCardinality() {
				return resolved.body.getCardinality();
			}

			@Override
			public boolean dependsOnInput() {
				return resolved.body.dependsOnInput();
			}

			@Override
			public boolean dependsOnExternalState() {
				return resolved.body.dependsOnExternalState();
			}

			@Override
			public void apply(StackFrame callerFrame, N in, Path<N> path, Output<N> output) throws JsonQueryException {
				bindAndApply(callerFrame, callerFrame, resolved.paramBaseSlot, paramNames, args, 0, in, path, output, execFrame -> resolved.body.apply(execFrame, in, path, output));
			}
		};
	}

	private static <N> void bindAndApply(StackFrame callerFrame, StackFrame functionFrame, int baseSlot, List<FunctionParameter> paramNames, List<Expression<StackFrame, N>> args, int valueParamIndex, N in, Path<N> path, Output<N> output, Consumer<StackFrame> bodyTask) throws JsonQueryException {
		if (valueParamIndex == 0) {
			for (int i = 0; i < paramNames.size(); i++) {
				FunctionParameter paramName = paramNames.get(i);
				Expression<StackFrame, N> paramExpression = args.get(i);
				if (paramName.kind() == FunctionParameter.Kind.FILTER)
					functionFrame.set(baseSlot + i, boundFilter(callerFrame, paramExpression));
			}
		}
		if (valueParamIndex >= paramNames.size()) {
			bodyTask.accept(functionFrame);
			return;
		}
		FunctionParameter paramName = paramNames.get(valueParamIndex);
		Expression<StackFrame, N> paramExpression = args.get(valueParamIndex);
		if (paramName.kind() == FunctionParameter.Kind.VALUE) {
			int slot = baseSlot + valueParamIndex;
			paramExpression.apply(callerFrame, in, path, (value, valuePath) -> {
				functionFrame.set(slot, value);
				bindAndApply(callerFrame, functionFrame, baseSlot, paramNames, args, valueParamIndex + 1, in, path, output, bodyTask);
			});
		} else {
			bindAndApply(callerFrame, functionFrame, baseSlot, paramNames, args, valueParamIndex + 1, in, path, output, bodyTask);
		}
	}

	private static <N> Function boundFilter(StackFrame callerFrame, Expression<StackFrame, N> expression) {
		return new Function() {
			@Override
			@SuppressWarnings("unchecked")
			public <Context, N1> Expression<Context, N1> bindArguments(JsonProvider<N1> jsonProvider, List<Expression<Context, N1>> args, Version version) {
				Expression<StackFrame, N1> effectiveExpression = (Expression<StackFrame, N1>) (Expression<?, ?>) expression;
				return (frame, in, path, output) -> effectiveExpression.apply(callerFrame, in, path, output);
			}
		};
	}
}
