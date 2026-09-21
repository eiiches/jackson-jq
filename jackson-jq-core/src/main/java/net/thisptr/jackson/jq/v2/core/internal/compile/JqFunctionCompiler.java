package net.thisptr.jackson.jq.v2.core.internal.compile;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.compile.opt.FoldPlanner;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.tree.ExpressionRewriter;
import net.thisptr.jackson.jq.v2.core.internal.tree.RewritableExpression;
import net.thisptr.jackson.jq.v2.core.internal.utils.StackFrameValues;
import net.thisptr.jackson.jq.v2.internal.javacc.AstParser;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionParameter;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.JqFunction;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.version.Version;

final class JqFunctionCompiler {
	enum Origin {
		ENVIRONMENT,
		LOADER
	}

	record DefinitionKey(Version version, FunctionSignature signature, List<FunctionParameter> parameters, String body,
						 Origin origin) {
		DefinitionKey(Version version, FunctionSignature signature, JqFunction definition, Origin origin) {
			this(version, signature, definition.parameters(), definition.body(), origin);
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

		synchronized <N> void finalizeGenericFunctions(Environment<N> env, FoldPlanner planner) throws JsonQueryException {
			for (CompiledDefinition definition : definitions.values())
				definition.finalizeGenericFunction(env, planner);
		}
	}

	private static final class ResolvedFunction<N> {
		volatile AnalyzedExpression<N> body;
		final int frameSize;
		// Absolute frame slot of the first param -- 0 for a dedicated-frame body (compileResolvedFunction),
		// or wherever the caller's frame had already grown to for an inlined body
		// (compileResolvedFunctionInline). bindAndApply needs this to translate a param's 0-based index
		// into the correct absolute slot for whichever kind of frame it's writing into.
		final int paramBaseSlot;

		ResolvedFunction(AnalyzedExpression<N> body, int frameSize) {
			this(body, frameSize, 0);
		}

		ResolvedFunction(AnalyzedExpression<N> body, int frameSize, int paramBaseSlot) {
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

		@SuppressWarnings("unchecked")
		synchronized <N> void finalizeGenericFunction(Environment<N> env, FoldPlanner planner) throws JsonQueryException {
			ResolvedFunction<N> resolved = (ResolvedFunction<N>) genericFunctions.get(env.getJsonProvider());
			if (resolved != null)
				resolved.body = StaticCallFinalizer.run(env, planner, resolved.body);
		}

		<N> ResolvedFunction<N> compileResolvedFunction(Environment<N> callingEnvironment, @Nullable List<AnalyzedExpression<N>> boundArguments, CompileContext context) throws JsonQueryException {
			context.pushFunctionScope();
			try {
				bindParamNames(context, boundArguments, 0);
				Environment<N> env = resolveEnvironment(callingEnvironment);
				AnalyzedExpression<N> body = Compiler.compileNonNull(env, context, parsedAst);
				return new ResolvedFunction<>(body, context.getSlotCount());
			} finally {
				context.popScope();
			}
		}

		<N> ResolvedFunction<N> compileResolvedFunctionInline(Environment<N> callingEnvironment, List<AnalyzedExpression<N>> boundArguments, CompileContext context) throws JsonQueryException {
			int baseSlot = context.pushInlinedFunctionScope();
			try {
				bindParamNames(context, boundArguments, baseSlot);
				Environment<N> env = resolveEnvironment(callingEnvironment);
				AnalyzedExpression<N> body = Compiler.compileNonNull(env, context, parsedAst);
				return new ResolvedFunction<>(body, context.getSlotCount(), baseSlot);
			} finally {
				context.popScope();
			}
		}

		private <N> Environment<N> resolveEnvironment(Environment<N> callingEnvironment) {
			if (origin == Origin.ENVIRONMENT)
				return callingEnvironment;
			EnvironmentBuilder<N> builder = EnvironmentBuilder.withDefaultLoaders(callingEnvironment.getJsonProvider(), version)
					.clearFunctionLoaders();
			callingEnvironment.getFunctionLoaders().forEach(builder::addFunctionLoader);
			return builder.build();
		}

		// baseSlot is unused by name/slot resolution here -- CompileContext.addLocalVariable/addLocalFunction
		// always assign the *next* slot in the current top scope, which pushFunctionScope()/
		// pushInlinedFunctionScope() already seeded correctly (0, or the caller's high-water mark,
		// respectively) -- it's threaded through only so callers of compileResolvedFunctionInline can be
		// reminded params start there, matching what bindAndApply needs to reconstruct independently.
		private <N> void bindParamNames(CompileContext context, @Nullable List<AnalyzedExpression<N>> boundArguments, int baseSlot) {
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
				} else {
					FunctionSignature signature = FunctionSignature.of(arg.name(), 0);
					if (info != null)
						context.addLocalFunction(signature, info);
					else
						context.addLocalFunction(signature);
				}
			}
		}

	}

	private JqFunctionCompiler() {
	}

	static <N> AnalyzedExpression<N> compile(Environment<N> env, CompileContext context, FunctionSignature signature, JqFunction definition, Origin origin, List<AnalyzedExpression<N>> args) {
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
				return bindResolvedInline(signature.name(), compiled.definition, args, resolved);
			}
			CompileContext functionContext = context.createJqFunctionContext(compiled.key, origin == Origin.ENVIRONMENT);
			ResolvedFunction<N> resolved = compiled.compileResolvedFunction(env, args, functionContext);
			return bindResolved(signature.name(), compiled.definition, args, resolved);
		} catch (JsonQueryException ignored) {
			return bindFallback(compiled, env, args, context, origin);
		}
	}

	private static <N> AnalyzedExpression<N> bindFallback(CompiledDefinition definition, Environment<N> env, List<AnalyzedExpression<N>> args, CompileContext context, Origin origin) {
		CompileContext genericContext = context.createGenericJqFunctionContext(definition.key, origin == Origin.ENVIRONMENT);
		if (!context.isGenericJqFunctionActive(definition.key))
			return bindResolved(definition.key.signature().name(), definition.definition, args, definition.getGenericFunction(env, genericContext));
		return bindGeneric(definition, env, args, genericContext);
	}

	private static <N> AnalyzedExpression<N> bindGeneric(CompiledDefinition definition, Environment<N> env, List<AnalyzedExpression<N>> args, CompileContext genericContext) {
		return new DeferredJqFunction<>(definition, env, args, genericContext);
	}

	/**
	 * A call made while the callee's own body is still being compiled -- a jq function calling itself. The
	 * body is fetched when the call runs, by which time it exists.
	 * <p>
	 * A named class rather than a lambda, because an analysis walking the compiled tree has to be able to
	 * tell what it has reached: a lambda is the one node no {@code instanceof} can name.
	 */
	private static final class DeferredJqFunction<N> implements DeferredJqFunctionCall<N> {
		private final CompiledDefinition definition;
		private final Environment<N> env;
		private final List<AnalyzedExpression<N>> args;
		private final CompileContext genericContext;

		DeferredJqFunction(CompiledDefinition definition, Environment<N> env, List<AnalyzedExpression<N>> args, CompileContext genericContext) {
			this.definition = definition;
			this.env = env;
			this.args = args;
			this.genericContext = genericContext;
		}

		@Override
		public List<AnalyzedExpression<N>> arguments() {
			return args;
		}

		@Override
		public void apply(StackFrame callerFrame, N in, Path<N> path, Output<N> output) throws JsonQueryException {
			bindResolved(definition.key.signature().name(), definition.definition, args, definition.getGenericFunction(env, genericContext))
					.apply(callerFrame, in, path, output);
		}
	}

	/**
	 * A jq function body bound to one call site's arguments.
	 * <p>
	 * It is rewritable so that folding, which runs after type checking, can still reach the body and the
	 * arguments. A constant argument matters most: every reference to its parameter compiled as the
	 * caller's own expression (see {@link BoundArgumentInfo#precomputedExpression()}), and folding those
	 * references is what lets a constant reach a native function through a chain of jq wrappers --
	 * {@code capture} to {@code match} to {@code _match_impl}. A body subtree that reads a parameter out
	 * of a slot names that slot among its free variables, so it is never eligible to fold.
	 */
	private abstract static class AbstractBoundJqFunction<N> implements RewritableExpression<N>, BoundJqFunctionCall<N> {
		final String name;
		final List<FunctionParameter> paramNames;
		final List<TypeScheme<FunctionType>> typeSchemes;
		final List<AnalyzedExpression<N>> args;
		final ResolvedFunction<N> resolved;

		AbstractBoundJqFunction(String name, List<FunctionParameter> paramNames, List<TypeScheme<FunctionType>> typeSchemes,
								List<AnalyzedExpression<N>> args, ResolvedFunction<N> resolved) {
			this.name = name;
			this.paramNames = paramNames;
			this.typeSchemes = typeSchemes;
			this.args = args;
			this.resolved = resolved;
		}

		abstract AbstractBoundJqFunction<N> recreate(List<AnalyzedExpression<N>> rewrittenArgs, ResolvedFunction<N> rewrittenResolved);

		@Override
		public final String name() {
			return name;
		}

		@Override
		public final List<FunctionParameter> parameters() {
			return paramNames;
		}

		@Override
		public final List<TypeScheme<FunctionType>> typeSchemes() {
			return typeSchemes;
		}

		@Override
		public final List<AnalyzedExpression<N>> arguments() {
			return args;
		}

		@Override
		public final AnalyzedExpression<N> body() {
			return resolved.body;
		}

		@Override
		public final int parameterBaseSlot() {
			return resolved.paramBaseSlot;
		}

		@Override
		public final AnalyzedExpression<N> rewriteChildren(ExpressionRewriter<N> rewriter) {
			List<AnalyzedExpression<N>> rewrittenArgs = ExpressionRewriter.rewriteAll(args, rewriter);
			AnalyzedExpression<N> rewrittenBody = rewriter.rewrite(resolved.body);
			if (rewrittenArgs == args && rewrittenBody == resolved.body)
				return this;
			return recreate(rewrittenArgs, rewrittenBody == resolved.body
					? resolved
					: new ResolvedFunction<>(rewrittenBody, resolved.frameSize, resolved.paramBaseSlot));
		}

		@Override
		public final Cardinality getCardinality() {
			return resolved.body.getCardinality();
		}

		@Override
		public final boolean dependsOnInput() {
			return resolved.body.dependsOnInput();
		}

		@Override
		public final boolean dependsOnExternalState() {
			return resolved.body.dependsOnExternalState();
		}
	}

	private static final class FramedJqFunction<N> extends AbstractBoundJqFunction<N> {
		FramedJqFunction(String name, List<FunctionParameter> paramNames, List<TypeScheme<FunctionType>> typeSchemes,
						 List<AnalyzedExpression<N>> args, ResolvedFunction<N> resolved) {
			super(name, paramNames, typeSchemes, args, resolved);
		}

		@Override
		AbstractBoundJqFunction<N> recreate(List<AnalyzedExpression<N>> rewrittenArgs, ResolvedFunction<N> rewrittenResolved) {
			return new FramedJqFunction<>(name, paramNames, typeSchemes, rewrittenArgs, rewrittenResolved);
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
	}

	/**
	 * Like {@link FramedJqFunction}, but for a {@code resolved} body produced by
	 * {@code CompiledDefinition#compileResolvedFunctionInline} -- its params/locals already live at fixed,
	 * baked-in absolute slots in whatever real frame encloses this call site (see
	 * {@link CompileContext#pushInlinedFunctionScope}), so {@code apply()} binds params directly into
	 * {@code callerFrame} instead of pushing a dedicated one.
	 */
	private static final class InlinedJqFunction<N> extends AbstractBoundJqFunction<N> {
		InlinedJqFunction(String name, List<FunctionParameter> paramNames, List<TypeScheme<FunctionType>> typeSchemes,
						  List<AnalyzedExpression<N>> args, ResolvedFunction<N> resolved) {
			super(name, paramNames, typeSchemes, args, resolved);
		}

		@Override
		AbstractBoundJqFunction<N> recreate(List<AnalyzedExpression<N>> rewrittenArgs, ResolvedFunction<N> rewrittenResolved) {
			return new InlinedJqFunction<>(name, paramNames, typeSchemes, rewrittenArgs, rewrittenResolved);
		}

		@Override
		public void apply(StackFrame callerFrame, N in, Path<N> path, Output<N> output) throws JsonQueryException {
			bindAndApply(callerFrame, callerFrame, resolved.paramBaseSlot, paramNames, args, 0, in, path, output, execFrame -> resolved.body.apply(execFrame, in, path, output));
		}
	}

	private static <N> AnalyzedExpression<N> bindResolved(String name, JqFunction definition, List<AnalyzedExpression<N>> args, ResolvedFunction<N> resolved) {
		return new FramedJqFunction<>(name, definition.parameters(), definition.typeSchemes(), args, resolved);
	}

	private static <N> AnalyzedExpression<N> bindResolvedInline(String name, JqFunction definition, List<AnalyzedExpression<N>> args, ResolvedFunction<N> resolved) {
		return new InlinedJqFunction<>(name, definition.parameters(), definition.typeSchemes(), args, resolved);
	}

	private static <N> void bindAndApply(StackFrame callerFrame, StackFrame functionFrame, int baseSlot, List<FunctionParameter> paramNames, List<AnalyzedExpression<N>> args, int valueParamIndex, N in, Path<N> path, Output<N> output, Consumer<StackFrame> bodyTask) throws JsonQueryException {
		if (valueParamIndex == 0) {
			for (int i = 0; i < paramNames.size(); i++) {
				FunctionParameter paramName = paramNames.get(i);
				AnalyzedExpression<N> paramExpression = args.get(i);
				if (paramName.kind() == FunctionParameter.Kind.FILTER)
					functionFrame.set(baseSlot + i, boundFilter(callerFrame, paramExpression));
			}
		}
		if (valueParamIndex >= paramNames.size()) {
			bodyTask.accept(functionFrame);
			return;
		}
		FunctionParameter paramName = paramNames.get(valueParamIndex);
		AnalyzedExpression<N> paramExpression = args.get(valueParamIndex);
		if (paramName.kind() == FunctionParameter.Kind.VALUE) {
			int slot = baseSlot + valueParamIndex;
			paramExpression.apply(callerFrame, in, path, (value, valuePath) -> {
				functionFrame.set(slot, StackFrameValues.toSlot(value));
				bindAndApply(callerFrame, functionFrame, baseSlot, paramNames, args, valueParamIndex + 1, in, path, output, bodyTask);
			});
		} else {
			bindAndApply(callerFrame, functionFrame, baseSlot, paramNames, args, valueParamIndex + 1, in, path, output, bodyTask);
		}
	}

	private static <N> Function boundFilter(StackFrame callerFrame, AnalyzedExpression<N> expression) {
		return new Function() {
			@Override
			@SuppressWarnings("unchecked")
			public <Context extends RuntimeContext, N1> Expression<Context, N1> bind(BindContext<N1> bindCtx, List<Expression<Context, N1>> args) {
				AnalyzedExpression<N1> effectiveExpression = (AnalyzedExpression<N1>) expression;
				return (frame, in, path, output) -> effectiveExpression.apply(callerFrame, in, path, output);
			}
		};
	}
}
