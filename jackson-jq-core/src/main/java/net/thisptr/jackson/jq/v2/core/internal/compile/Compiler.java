package net.thisptr.jackson.jq.v2.core.internal.compile;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.CompileOptions;
import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.OptimizationOptions;
import net.thisptr.jackson.jq.v2.core.diagnostic.DiagnosticListener;
import net.thisptr.jackson.jq.v2.core.function.FunctionLoader;
import net.thisptr.jackson.jq.v2.core.internal.ast.ArrayConstructionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ArrayMatcherAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.AsBindingAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.AstVisitor;
import net.thisptr.jackson.jq.v2.core.internal.ast.BinaryOpAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.BooleanLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.BracketExtractFieldAccessAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.BracketFieldAccessAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.BreakExpressionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ConditionalAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ForeachExpressionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.FormattingFilterAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.FunctionCallAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.FunctionDefinitionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.IdentifierFieldAccessAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.LabelAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.NegativeExpressionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.NullLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.NumericLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ObjectConstructionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ObjectMatcherAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ParenAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.PatternMatcherAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.RecursionOperatorAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ReduceExpressionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.SemicolonOperatorAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.StringFieldAccessAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.StringInterpolationAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.StringLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ThisObjectAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.TopLevelAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.TryCatchAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ValueMatcherAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.VariableAccessAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.operator.BinaryOperator;
import net.thisptr.jackson.jq.v2.core.internal.commons.pair.Pair;
import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.compile.opt.FoldPlanner;
import net.thisptr.jackson.jq.v2.core.internal.compile.resolved.ResolvedCapturedFunctionAccess;
import net.thisptr.jackson.jq.v2.core.internal.compile.resolved.ResolvedCapturedFunctionBoundArgumentAccess;
import net.thisptr.jackson.jq.v2.core.internal.compile.resolved.ResolvedCapturedVariableAccess;
import net.thisptr.jackson.jq.v2.core.internal.compile.resolved.ResolvedCapturedVariableBoundArgumentAccess;
import net.thisptr.jackson.jq.v2.core.internal.compile.resolved.ResolvedFixedVariableAccess;
import net.thisptr.jackson.jq.v2.core.internal.compile.resolved.ResolvedFunctionCall;
import net.thisptr.jackson.jq.v2.core.internal.compile.resolved.ResolvedFunctionDefinition;
import net.thisptr.jackson.jq.v2.core.internal.compile.resolved.ResolvedGlobalFunctionAccess;
import net.thisptr.jackson.jq.v2.core.internal.compile.resolved.ResolvedGlobalVariableAccess;
import net.thisptr.jackson.jq.v2.core.internal.compile.resolved.ResolvedLocalFunctionAccess;
import net.thisptr.jackson.jq.v2.core.internal.compile.resolved.ResolvedLocalFunctionBoundArgumentAccess;
import net.thisptr.jackson.jq.v2.core.internal.compile.resolved.ResolvedLocalVariableAccess;
import net.thisptr.jackson.jq.v2.core.internal.compile.resolved.ResolvedLocalVariableBoundArgumentAccess;
import net.thisptr.jackson.jq.v2.core.internal.compile.resolved.ResolvedTailCall;
import net.thisptr.jackson.jq.v2.core.internal.compile.resolved.TailCallArgument;
import net.thisptr.jackson.jq.v2.core.internal.diagnostics.PipeParenthesesCheck;
import net.thisptr.jackson.jq.v2.core.internal.memory.Memory;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.tree.ArrayConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.BreakExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.Comma;
import net.thisptr.jackson.jq.v2.core.internal.tree.Conditional;
import net.thisptr.jackson.jq.v2.core.internal.tree.FieldConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.ForeachExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.IdentifierKeyFieldConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.JsonQueryKeyFieldConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.Label;
import net.thisptr.jackson.jq.v2.core.internal.tree.MeteredOutputExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.NegativeExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.ObjectConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.PipedQuery;
import net.thisptr.jackson.jq.v2.core.internal.tree.RecursionOperator;
import net.thisptr.jackson.jq.v2.core.internal.tree.ReduceExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.SemicolonOperator;
import net.thisptr.jackson.jq.v2.core.internal.tree.StringInterpolation;
import net.thisptr.jackson.jq.v2.core.internal.tree.StringKeyFieldConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.ThisObject;
import net.thisptr.jackson.jq.v2.core.internal.tree.TopLevelExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.TryCatch;
import net.thisptr.jackson.jq.v2.core.internal.tree.VariableBinding;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.AlternativeOperatorExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.BooleanAndExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.BooleanOrExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.DivideExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.MinusExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.ModuloExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.MultiplyExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.PlusExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment.Assignment;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment.ComplexAlternativeAssignment;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment.ComplexDivideAssignment;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment.ComplexMinusAssignment;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment.ComplexModuloAssignment;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment.ComplexMultiplyAssignment;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment.ComplexPlusAssignment;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment.UpdateAssignment;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.comparison.CompareEqualTest;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.comparison.CompareGreaterEqualTest;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.comparison.CompareGreaterTest;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.comparison.CompareLessEqualTest;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.comparison.CompareLessTest;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.comparison.CompareNotEqualTest;
import net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess.BracketExtractFieldAccess;
import net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess.BracketFieldAccess;
import net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess.IdentifierFieldAccess;
import net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess.StringFieldAccess;
import net.thisptr.jackson.jq.v2.core.internal.tree.literal.ValueLiteral;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.SlotResolver;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.matchers.ArrayMatcher;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.matchers.ObjectMatcher;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.matchers.ValueMatcher;
import net.thisptr.jackson.jq.v2.core.internal.utils.ExpressionUtils;
import net.thisptr.jackson.jq.v2.core.internal.utils.StackFrameValues;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.Maybe;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.JqFunction;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.JavaModule;
import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class Compiler {
	@SuppressWarnings("unchecked")
	private static <N> @Nullable Expression<StackFrame, N> precomputedBoundArgument(BoundArgumentInfo info) {
		Expression<?, ?> expression = info.precomputedExpression();
		return expression == null ? null : (Expression<StackFrame, N>) expression;
	}

	public static <JsonNode> Expression<StackFrame, JsonNode> compile(Environment<JsonNode> env, AstNode ast) throws JsonQueryException {
		return compile(env, CompileOptions.newBuilder().build(), ModuleScope.root(env), ast);
	}

	public static <JsonNode> Expression<StackFrame, JsonNode> compile(Environment<JsonNode> env, CompileOptions options, ModuleScope<JsonNode> scope, AstNode ast) throws JsonQueryException {
		return compileRoot(env, options, scope, ast, /* exportTopLevelFunctions */ false, /* meterRuntimeBudgets */ true);
	}

	/**
	 * Compiles a module's own defining source. Unlike {@link #compile(Environment, Module, AstNode)}, this
	 * tracks the module's genuinely top-level {@code def}s (see {@link CompileContext#exportsTopLevelFunctions()}
	 * / {@link CompileContext#isRootScope()}) so {@link RootExpression#applyForModuleExports} can harvest their
	 * real, correctly closure-bound {@link Function} values after running the module body once. This is how
	 * {@link ModuleResolver#compileSource} populates an imported module's exported functions.
	 * Ordinary query compilation must never do this -- use {@link #compile(Environment, CompileOptions, ModuleScope, AstNode)}.
	 */
	public static <JsonNode> Expression<StackFrame, JsonNode> compileModule(Environment<JsonNode> env, CompileOptions options, ModuleScope<JsonNode> scope, AstNode ast) throws JsonQueryException {
		return compileRoot(env, options, scope, ast, /* exportTopLevelFunctions */ true, /* meterRuntimeBudgets */ false);
	}

	private static <JsonNode> Expression<StackFrame, JsonNode> compileRoot(Environment<JsonNode> env, CompileOptions options, ModuleScope<JsonNode> scope, AstNode ast, boolean exportTopLevelFunctions, boolean meterRuntimeBudgets) throws JsonQueryException {
		// Only whole queries and module sources are diagnosed. Function bodies that jq libraries
		// bring along are compiled through the inner compile() below, never through here, so a
		// caller never sees warnings about jq's own builtins.
		DiagnosticListener diagnosticListener = options.getDiagnosticListener();
		if (diagnosticListener != null)
			PipeParenthesesCheck.run(ast, diagnosticListener);

		OptimizationOptions optimizationOptions = options.getOptimizationOptions();
		CompileContext context = new CompileContext(exportTopLevelFunctions, meterRuntimeBudgets, optimizationOptions);
		FoldPlanner foldPlanner = context.foldPlanner();
		boolean planFolds = meterRuntimeBudgets && foldPlanner.isEnabled();
		if (planFolds)
			foldPlanner.beginRegion();
		@Var Expression<StackFrame, JsonNode> compiled;
		try {
			compiled = compile(env, context, scope, ast);
			if (planFolds && compiled != null)
				compiled = foldPlanner.finishRegion(env, compiled);
		} catch (RuntimeException | Error e) {
			if (planFolds)
				foldPlanner.cancelRegion();
			throw e;
		}
		if (compiled == null)
			throw new JsonQueryException("Cannot resolve null expression");
		Set<String> definedVariables = new HashSet<>(env.getVariables().keySet());
		definedVariables.addAll(env.getConstants().keySet());
		definedVariables.addAll(context.importedVariableDefaults().keySet());
		Set<FunctionSignature> definedFunctions = new HashSet<>(env.getFunctions().keySet());
		definedFunctions.addAll(env.getJqFunctions().keySet());
		// Allocated before getOutputCounterCount() is read, so the root's own counter is included in the total.
		int innerOutputIndex = context.outputCounterOf(compiled);
		return new RootExpression<>(context.getSlotCount(), context.getGlobalCount(), context.getOutputCounterCount(), innerOutputIndex, compiled,
				definedVariables, definedFunctions,
				context.globalVariableIndices(), context.globalFunctionIndices(),
				env.getDeclaredVariables(), env.getDeclaredFunctions(), context.rootFunctionSlots());
	}

	public static <JsonNode> @Nullable Expression<StackFrame, JsonNode> compile(Environment<JsonNode> env, CompileContext context, @Nullable AstNode ast) throws JsonQueryException {
		return compile(env, context, ModuleScope.root(env), ast);
	}

	public static <JsonNode> @Nullable Expression<StackFrame, JsonNode> compile(Environment<JsonNode> env, CompileContext context, ModuleScope<JsonNode> scope, @Nullable AstNode ast) throws JsonQueryException {
		if (ast == null)
			return null;
		return new CompilationVisitor<>(env, context, scope).compileExpression(ast);
	}

	private static final class CompiledMatcher<N> {
		final PatternMatcher<N> matcher;
		final Set<String> variableNames;

		CompiledMatcher(PatternMatcher<N> matcher, Set<String> variableNames) {
			this.matcher = matcher;
			this.variableNames = variableNames;
		}
	}

	private static final class CompiledFieldMatcher<N> {
		final ObjectMatcher.FieldMatcher<N> matcher;
		final Set<String> variableNames;

		CompiledFieldMatcher(ObjectMatcher.FieldMatcher<N> matcher, Set<String> variableNames) {
			this.matcher = matcher;
			this.variableNames = variableNames;
		}
	}

	private static final class CompilationVisitor<N> implements AstVisitor<Object> {
		private final Environment<N> env;
		private final CompileContext context;
		private final ModuleScope<N> scope;

		// Whether the node this visitor lowers stands in tail position of the def body it belongs to. Read off
		// the context on the way in and cleared there, so a child this visitor does not deliberately re-arm is
		// lowered as not in tail position -- the right default for every node kind but the handful below.
		private boolean inTailPosition;

		CompilationVisitor(Environment<N> env, CompileContext context, ModuleScope<N> scope) {
			this.env = env;
			this.context = context;
			this.scope = scope;
		}

		/**
		 * The single point every AST node becomes an {@link Expression} at, and therefore where the
		 * compiler's uniform rewrites run. It records the lowered hierarchy for the top-down folding pass;
		 * speculative evaluation starts only after the complete region has been lowered.
		 */
		private Expression<StackFrame, N> compileExpression(AstNode ast) throws JsonQueryException {
			inTailPosition = context.setTailPosition(false);
			FoldPlanner planner = context.foldPlanner();
			if (!context.metersRuntimeBudgets() || !planner.isPlanning())
				return accept(ast, Expression.class);
			// An abandoned node is one whose endNode() is never reached, so a failing compile needs no cleanup.
			int mark = planner.beginNode();
			Expression<StackFrame, N> compiled = accept(ast, Expression.class);
			return planner.endNode(mark, compiled, context.getSlotCount(), context.getGlobalCount(), context.getOutputCounterCount());
		}

		private CompiledMatcher<N> compileMatcher(PatternMatcherAstNode ast) throws JsonQueryException {
			return accept(ast, CompiledMatcher.class);
		}

		private CompiledFieldMatcher<N> compileFieldMatcher(ObjectMatcherAstNode.FieldMatcher ast) throws JsonQueryException {
			return accept(ast, CompiledFieldMatcher.class);
		}

		private FieldConstruction<N> compileField(ObjectConstructionAstNode.FieldConstructionAst ast) throws JsonQueryException {
			return accept(ast, FieldConstruction.class);
		}

		private <T> T accept(AstNode ast, Class<?> expectedType) throws JsonQueryException {
			Object result = ast.accept(this);
			if (!expectedType.isInstance(result)) {
				throw new IllegalStateException(String.format("Expected %s when visiting %s, but got %s", expectedType.getSimpleName(), ast.getClass().getSimpleName(), result == null ? "null" : result.getClass().getSimpleName()));
			}
			// The runtime type check above guarantees that the visitor returned the requested result type.
			@SuppressWarnings("unchecked")
			T castResult = (T) result;
			return castResult;
		}

		@Override
		public Expression<StackFrame, N> visit(ParenAstNode paren) throws JsonQueryException {
			context.setTailPosition(inTailPosition);
			return compileNonNull(env, context, scope, paren.value());
		}

		@Override
		public Expression<StackFrame, N> visit(FunctionCallAstNode call) throws JsonQueryException {
			List<Expression<StackFrame, N>> compiledArgs = new ArrayList<>();
			for (AstNode arg : call.args())
				compiledArgs.add(compileArgument(arg));
			List<Expression<StackFrame, N>> meteredArgs = Collections.unmodifiableList(meterArguments(compiledArgs));

			if (call.moduleName() != null) {
				@Var JavaModule mod = context.getImportedModule(call.moduleName());
				if (mod == null) {
					// An environment may have been handed either kind of module; jq source is
					// compiled here, the first time a query actually calls into it.
					Module imported = env.getImportedModules().get(call.moduleName());
					mod = imported != null ? scope.materialize(imported) : null;
				}
				Function factory = mod != null ? lookupFunction(mod.getFunctions(), call.signature()) : null;
				if (factory == null) {
					throw new JsonQueryException(String.format("Function %s::%s does not exist", call.moduleName(), call.signature()));
				}
				return bindFunctionCall(bindContextOf(env), factory, meteredArgs);
			}

			SymbolLocation location = context.getFunctionLocation(call.signature());
			Expression<StackFrame, N> compiled = compileFunctionCall(env, context, call.signature(), location, meteredArgs);
			if (inTailPosition && context.tailCallsEnabled() && context.isInsideDefinitionBody())
				return asTailCall(call.signature(), location, compiled, compiledArgs, meteredArgs);
			return compiled;
		}

		/**
		 * Turns a call standing in tail position into a {@link ResolvedTailCall}, or leaves it alone.
		 * <p>
		 * Two things have to hold beyond the position itself. The callee must be a {@code def} -- a
		 * {@code ResolvedFunctionDefinition} installs the {@code TailCallTarget} the loop needs, and a builtin,
		 * a declared global or a filter parameter has nothing of the kind. And every argument must survive the
		 * caller's frame being popped, which {@link #tailCallArgument} decides one at a time.
		 */
		private Expression<StackFrame, N> asTailCall(FunctionSignature signature, @Nullable SymbolLocation location, Expression<StackFrame, N> compiled, List<Expression<StackFrame, N>> compiledArgs, List<Expression<StackFrame, N>> meteredArgs) {
			int frameClosureSlot;
			int slot;
			if (compiled instanceof ResolvedLocalFunctionAccess) {
				frameClosureSlot = ResolvedTailCall.LOCAL;
				slot = ((ResolvedLocalFunctionAccess<N>) compiled).slot();
			} else if (compiled instanceof ResolvedCapturedFunctionAccess) {
				// The usual case for recursion: a def referring to itself has crossed its own function
				// boundary, so it reads itself out of its own closure rather than out of a frame slot.
				frameClosureSlot = ((ResolvedCapturedFunctionAccess<N>) compiled).frameClosureSlot();
				slot = ((ResolvedCapturedFunctionAccess<N>) compiled).closureSlot();
			} else {
				return compiled;
			}
			List<String> parameterNames = location != null ? location.parameterNames : null;
			if (parameterNames == null || parameterNames.size() != meteredArgs.size())
				return compiled;
			List<TailCallArgument<N>> arguments = new ArrayList<>(parameterNames.size());
			for (int i = 0; i < parameterNames.size(); i++) {
				TailCallArgument<N> argument = tailCallArgument(parameterNames.get(i), compiledArgs.get(i), meteredArgs.get(i));
				if (argument == null)
					return compiled;
				arguments.add(argument);
			}
			// A call back into the def this body belongs to needs no new frame -- it writes its arguments into
			// the parameter slots that frame already has. Matching signatures is necessary but not sufficient,
			// so the node checks the callee's identity at run time and takes the general path if it differs.
			int[] selfParameterSlots = signature.equals(context.enclosingDefinitionSignature())
					? toIntArray(context.enclosingDefinitionParameterSlots())
					: null;
			return new ResolvedTailCall<>(compiled, context.getOrAssignTailCallSlot(), frameClosureSlot, slot, arguments, selfParameterSlots);
		}

		private static int @Nullable [] toIntArray(@Nullable List<Integer> slots) {
			if (slots == null)
				return null;
			int[] array = new int[slots.size()];
			for (int i = 0; i < array.length; i++)
				array[i] = slots.get(i);
			return array;
		}

		/**
		 * Reduces one argument to something the callee's frame can hold once the caller's is gone, or returns
		 * {@code null} when it cannot be reduced and the call stays an ordinary one.
		 */
		private @Nullable TailCallArgument<N> tailCallArgument(String parameterName, @Nullable Expression<StackFrame, N> argument, @Nullable Expression<StackFrame, N> meteredArgument) {
			if (argument == null || meteredArgument == null)
				return null;
			if (parameterName.startsWith("$")) {
				// An argument emitting several values would need the body run once per value, and that is a
				// loop of its own, not a jump.
				return meteredArgument.getCardinality() == Cardinality.ONE ? new TailCallArgument.Value<>(meteredArgument) : null;
			}
			// A filter argument is otherwise a closure over the caller's frame, which the loop pops. A bare
			// reference to a function already sitting in a slot is the one shape that outlives it.
			if (argument instanceof ResolvedLocalFunctionAccess && ((ResolvedLocalFunctionAccess<N>) argument).args().isEmpty())
				return new TailCallArgument.Filter<>(ResolvedTailCall.LOCAL, ((ResolvedLocalFunctionAccess<N>) argument).slot());
			if (argument instanceof ResolvedCapturedFunctionAccess && ((ResolvedCapturedFunctionAccess<N>) argument).args().isEmpty())
				return new TailCallArgument.Filter<>(((ResolvedCapturedFunctionAccess<N>) argument).frameClosureSlot(), ((ResolvedCapturedFunctionAccess<N>) argument).closureSlot());
			return null;
		}

		/**
		 * Gives each argument its own output counter, so that everything it emits is charged against
		 * {@code RuntimeOptions.Builder#setMaxOutputsPerExpression(long)}.
		 * <p>
		 * An argument is the one position the consumer-side counters cannot reach: what drains it is a Java
		 * builtin, a third-party {@code Function}, or a jq-library body reaching it through a parameter
		 * access -- none of which the engine can instrument, and the last two not even in principle.
		 * Wrapping the producer covers all three at once. Every other expression is counted by whatever
		 * consumes it, in a sink the consumer already builds, with the counter index handed to it at
		 * construction time by {@link CompileContext#outputCounterOf}.
		 * <p>
		 * Unlike the consumer-side counters this wraps regardless of cardinality, because an argument that
		 * emits a single value per call is exactly the runaway shape here: {@code until(false; .)} loops
		 * forever on one value per iteration. It also runs <em>after</em> {@code ConstantFolder}, so that
		 * folding {@code until(false; 1)}'s arguments into constants cannot take their counters with them.
		 *
		 * @param args the compiled arguments, which may contain {@code null} for an absent one
		 * @return the arguments, each wrapped in a counter unless nothing is being metered
		 */
		private List<Expression<StackFrame, N>> meterArguments(List<Expression<StackFrame, N>> args) {
			if (!context.metersRuntimeBudgets() || args.isEmpty())
				return args;
			List<Expression<StackFrame, N>> metered = new ArrayList<>(args.size());
			for (Expression<StackFrame, N> arg : args)
				metered.add(arg == null ? arg : MeteredOutputExpression.of(arg, context.allocateOutputCounter()));
			return metered;
		}

		private @Nullable Expression<StackFrame, N> compileArgument(@Nullable AstNode arg) throws JsonQueryException {
			if (arg == null)
				return null;
			FoldPlanner planner = context.foldPlanner();
			boolean planFold = context.metersRuntimeBudgets() && planner.isPlanning();
			if (!planFold)
				return compile(env, context, scope, arg);
			planner.beginRegion();
			try {
				return planner.finishRegion(env, compileNonNull(env, context, scope, arg));
			} catch (RuntimeException | Error e) {
				planner.cancelRegion();
				throw e;
			}
		}

		@Override
		public Expression<StackFrame, N> visit(VariableAccessAstNode varAccess) throws JsonQueryException {
			return compileVariableRef(env, context, varAccess.moduleName(), varAccess.name());
		}

		@Override
		public Expression<StackFrame, N> visit(TopLevelAstNode top) throws JsonQueryException {
			for (TopLevelAstNode.ImportStatement imp : top.imports()) {
				Maybe<N> metadata = evaluateMetadata(env.getJsonProvider(), imp);
				if (imp.dollarImport) {
					N data = scope.resolveData(imp.path, metadata);
					if (imp.name != null) {
						context.addImportedVariableDefault(imp.name, data);
					}
				} else {
					JavaModule mod = scope.resolveModule(imp.path, metadata);
					if (imp.name != null) {
						context.addImportedModule(imp.name, mod);
					}
				}
			}
			context.setTailPosition(inTailPosition);
			Expression<StackFrame, N> compiledInner = compileNonNull(env, context, scope, top.expr());
			return new TopLevelExpression<>(compiledInner);
		}

		/**
		 * A {@code |}. The AST mirrors the syntax, so an {@code as} binding or a {@code label} is the
		 * pipe's left side rather than a node owning the rest of the query; the tree nodes they compile
		 * into do own their body, and this is where the two shapes meet. The fold has to be explicit:
		 * a head is not an expression feeding values into the right side, so the input-fixing rule for
		 * an ordinary left operand -- and {@link PipedQuery}'s path shielding -- do not apply to it.
		 */
		private Expression<StackFrame, N> compilePipe(BinaryOpAstNode piped) throws JsonQueryException {
			AstNode left = piped.lhs;
			if (left instanceof AsBindingAstNode)
				return compileAsBinding((AsBindingAstNode) left, piped.rhs);
			if (left instanceof LabelAstNode)
				return compileLabel((LabelAstNode) left, piped.rhs);

			Expression<StackFrame, N> compiledLeft = compileNonNull(env, context, scope, left);
			// The pipe's own values are the right side's, so the right side inherits tail position -- but only
			// once the left side is known to emit at most one value. Otherwise the left side is still iterating
			// while the right side runs, and unwinding out of it to a tail-call loop would drop what it has left.
			context.setTailPosition(inTailPosition && compiledLeft.getCardinality() != Cardinality.UNKNOWN);
			Expression<StackFrame, N> right = compileNonNull(env, context, scope, piped.rhs);
			return new PipedQuery<>(compiledLeft, right, context.outputCounterOf(compiledLeft));
		}

		private Expression<StackFrame, N> compileAsBinding(AsBindingAstNode binding, AstNode bodyAst) throws JsonQueryException {
			Expression<StackFrame, N> value = compileNonNull(env, context, scope, binding.value());
			CompiledMatcher<N> matcherResult = compileMatcher(binding.matcher());

			context.pushLocalScope();
			Map<String, Integer> slots = new HashMap<>();
			Expression<StackFrame, N> body;
			try {
				for (String varName : matcherResult.variableNames) {
					context.addLocalVariable(varName);
					slots.put(varName, context.getVariableSlot(varName));
				}
				body = compileNonNull(env, context, scope, bodyAst);
			} finally {
				context.popScope();
			}
			PatternMatcher<N> compiledMatcher = matcherResult.matcher.resolveSlots(new SlotResolver(slots));
			return new VariableBinding<>(value, compiledMatcher, new HashSet<>(slots.values()), body, context.outputCounterOf(value));
		}

		private Expression<StackFrame, N> compileLabel(LabelAstNode label, AstNode bodyAst) throws JsonQueryException {
			return new Label<>(label.name(), compileNonNull(env, context, scope, bodyAst));
		}

		// Reached only for an AST that the parser cannot produce: the grammar rejects a pipe head that
		// no `|` follows, so a bare head here means a hand-built tree.
		@Override
		public Expression<StackFrame, N> visit(AsBindingAstNode binding) throws JsonQueryException {
			throw new JsonQueryException(String.format("`%s` must be followed by `|`", binding));
		}

		@Override
		public Expression<StackFrame, N> visit(LabelAstNode label) throws JsonQueryException {
			throw new JsonQueryException(String.format("`%s` must be followed by `|`", label));
		}

		@Override
		public Expression<StackFrame, N> visit(SemicolonOperatorAstNode semi) throws JsonQueryException {
			List<Expression<StackFrame, N>> newExpressions = new ArrayList<>();
			Set<Integer> definedFunctionSlots = new HashSet<>();
			List<AstNode> expressions = semi.expressions();
			for (int i = 0; i < expressions.size(); i++) {
				AstNode q = expressions.get(i);
				// Everything but the last is evaluated for its effect and discarded -- a `def` statement, most
				// of the time -- so only the last carries this expression's own values, and tail position.
				context.setTailPosition(inTailPosition && i == expressions.size() - 1);
				Expression<StackFrame, N> expression = compileNonNull(env, context, q);
				newExpressions.add(expression);
				if (expression instanceof ResolvedFunctionDefinition<?>)
					definedFunctionSlots.add(((ResolvedFunctionDefinition<?>) expression).slot());
			}
			return new SemicolonOperator<>(newExpressions, definedFunctionSlots, context.outputCountersOf(newExpressions.subList(0, Math.max(0, newExpressions.size() - 1))));
		}

		@Override
		public Expression<StackFrame, N> visit(ObjectConstructionAstNode obj) throws JsonQueryException {
			List<FieldConstruction<N>> fields = new ArrayList<>(obj.fields.size());
			for (ObjectConstructionAstNode.FieldConstructionAst fc : obj.fields)
				fields.add(compileField(fc));
			return new ObjectConstruction<>(env.getJsonProvider(), fields);
		}

		@Override
		public FieldConstruction<N> visit(ObjectConstructionAstNode.IdentifierKeyFieldConstructionAst field) throws JsonQueryException {
			Expression<StackFrame, N> value = compile(env, context, field.value);
			return new IdentifierKeyFieldConstruction<>(env.getJsonProvider(), field.key, value, env.getJqVersion(), context.outputCounterOf(value));
		}

		@Override
		public FieldConstruction<N> visit(ObjectConstructionAstNode.JsonQueryKeyFieldConstructionAst field) throws JsonQueryException {
			Expression<StackFrame, N> key = compileNonNull(env, context, field.key());
			Expression<StackFrame, N> value = compileNonNull(env, context, field.value());
			return new JsonQueryKeyFieldConstruction<>(env.getJsonProvider(), key, value, env.getJqVersion(), context.outputCounterOf(key), context.outputCounterOf(value));
		}

		@Override
		public FieldConstruction<N> visit(ObjectConstructionAstNode.StringKeyFieldConstructionAst field) throws JsonQueryException {
			Expression<StackFrame, N> key = compileNonNull(env, context, field.key);
			Expression<StackFrame, N> value = compile(env, context, field.value);
			return new StringKeyFieldConstruction<>(env.getJsonProvider(), key, value, env.getJqVersion(), context.outputCounterOf(key), context.outputCounterOf(value));
		}

		@Override
		public FieldConstruction<N> visit(ObjectConstructionAstNode.VariableKeyFieldConstruction field) throws JsonQueryException {
			// Desugar `{ $x }` into the same shape as `{ x: $x }` -- no dedicated resolved class needed.
			Expression<StackFrame, N> value = compileVariableRef(env, context, null, field.name());
			return new IdentifierKeyFieldConstruction<>(env.getJsonProvider(), field.name(), value, env.getJqVersion(), context.outputCounterOf(value));
		}

		@Override
		public Expression<StackFrame, N> visit(ArrayConstructionAstNode arr) throws JsonQueryException {
			Expression<StackFrame, N> compiledArrayItems = compile(env, context, arr.q);
			return new ArrayConstruction<>(env.getJsonProvider(), compiledArrayItems, context.outputCounterOf(compiledArrayItems));
		}

		@Override
		public Expression<StackFrame, N> visit(BinaryOpAstNode bin) throws JsonQueryException {
			if (bin.operator == BinaryOperator.PIPE || bin.operator == BinaryOperator.BINDING_PIPE)
				return compilePipe(bin);
			if (bin.operator == BinaryOperator.COMMA) {
				List<Expression<StackFrame, N>> operands = new ArrayList<>();
				compileCommaOperands(bin, operands);
				return new Comma<>(operands);
			}

			Expression<StackFrame, N> lhs = compileNonNull(env, context, bin.lhs);
			Expression<StackFrame, N> rhs = compileNonNull(env, context, bin.rhs);
			return compileBinaryOperator(bin.operator, lhs, rhs, context.outputCounterOf(lhs), context.outputCounterOf(rhs), env.getJqVersion(), env.getJsonProvider());
		}

		@Override
		public Expression<StackFrame, N> visit(NegativeExpressionAstNode neg) throws JsonQueryException {
			Expression<StackFrame, N> compiledNegated = compileNonNull(env, context, neg.value());
			return new NegativeExpression<>(env.getJsonProvider(), compiledNegated, env.getJqVersion(), context.outputCounterOf(compiledNegated));
		}

		@Override
		public Expression<StackFrame, N> visit(ConditionalAstNode cond) throws JsonQueryException {
			List<Pair<Expression<StackFrame, N>, Expression<StackFrame, N>>> newSwitches = new ArrayList<>();
			// A branch runs inside the loop over its own condition's values, and inside the loops over every
			// earlier condition's; so it only inherits tail position once all of those are known to emit at
			// most one value. A condition itself never does -- the branch still has to run after it.
			@Var boolean conditionsEmitAtMostOne = true;
			for (Pair<AstNode, AstNode> sw : cond.switches()) {
				Expression<StackFrame, N> newIf = compileNonNull(env, context, sw._1);
				conditionsEmitAtMostOne = conditionsEmitAtMostOne && newIf.getCardinality() != Cardinality.UNKNOWN;
				context.setTailPosition(inTailPosition && conditionsEmitAtMostOne);
				Expression<StackFrame, N> newThen = compileNonNull(env, context, sw._2);
				newSwitches.add(Pair.of(newIf, newThen));
			}
			context.setTailPosition(inTailPosition && conditionsEmitAtMostOne);
			Expression<StackFrame, N> newElse = compileNonNull(env, context, cond.otherwise());
			int[] conditionOutputIndices = new int[newSwitches.size()];
			for (int i = 0; i < conditionOutputIndices.length; ++i)
				conditionOutputIndices[i] = context.outputCounterOf(newSwitches.get(i)._1);
			return new Conditional<>(env.getJsonProvider(), newSwitches, newElse, conditionOutputIndices);
		}

		@Override
		public Expression<StackFrame, N> visit(TryCatchAstNode tc) throws JsonQueryException {
			Expression<StackFrame, N> newTry = compileNonNull(env, context, tc.tryExpr());
			Expression<StackFrame, N> newCatch = compile(env, context, tc.catchExpr());
			countLegacyTryBarrier();
			return new TryCatch<>(env.getJsonProvider(), newTry, newCatch, env.getJqVersion());
		}

		@Override
		public Expression<StackFrame, N> visit(TryCatchAstNode.Question question) throws JsonQueryException {
			Expression<StackFrame, N> expression = compileNonNull(env, context, question.tryExpr());
			countLegacyTryBarrier();
			return new TryCatch<>(env.getJsonProvider(), expression, env.getJqVersion());
		}

		/**
		 * Marks a {@code try}/{@code ?} compiled in a jq version where it also catches what its consumer
		 * throws, which makes every subtree containing it unfoldable -- see
		 * {@link CompileContext#markFoldBarrier()}. From 1.7 on, {@code TryCatch} tunnels a downstream error
		 * past itself, so a {@code try} is a function of its own subtree and folds like anything else.
		 */
		private void countLegacyTryBarrier() {
			if (env.getJqVersion().compareTo(TryCatch.DOWNSTREAM_ERRORS_ESCAPE_SINCE) < 0)
				context.markFoldBarrier();
		}

		/**
		 * A {@code ,}. The AST mirrors the syntax, so {@code a, b, c} is a left-nested chain of binary
		 * nodes, but the nesting means nothing at evaluation time -- every operand sees the same input
		 * and the same path. The whole chain is flattened into one {@link Comma} so that evaluating it
		 * is a loop rather than a stack frame per comma. Parentheses are transparent here, just as they
		 * are when compiled normally.
		 */
		// Walked with an explicit stack rather than by recursion: the chain is as long as the query has
		// commas, and compiling `[1, 2, ..., n]` must not cost n frames either.
		private void compileCommaOperands(BinaryOpAstNode comma, List<Expression<StackFrame, N>> operands) throws JsonQueryException {
			Deque<AstNode> pending = new ArrayDeque<>();
			pending.push(comma);
			while (!pending.isEmpty()) {
				AstNode operand = pending.pop();
				if (operand instanceof BinaryOpAstNode && ((BinaryOpAstNode) operand).operator == BinaryOperator.COMMA) {
					pending.push(((BinaryOpAstNode) operand).rhs);
					pending.push(((BinaryOpAstNode) operand).lhs);
					continue;
				}
				if (operand instanceof ParenAstNode) {
					pending.push(((ParenAstNode) operand).value());
					continue;
				}
				// Only the last operand inherits tail position: an earlier one is followed by operands the
				// enclosing Comma still has to run, so unwinding past them would lose their values. Earlier
				// operands have already emitted and finished by then, which is why they place no condition
				// on the last one.
				context.setTailPosition(inTailPosition && pending.isEmpty());
				operands.add(compileNonNull(env, context, operand));
			}
		}

		@Override
		public Expression<StackFrame, N> visit(ReduceExpressionAstNode red) throws JsonQueryException {
			Expression<StackFrame, N> compiledIter = compileNonNull(env, context, red.iterExpr());
			Expression<StackFrame, N> compiledInit = compileNonNull(env, context, red.initExpr());
			CompiledMatcher<N> matcherResult = compileMatcher(red.matcher());
			@Var PatternMatcher<N> compiledMatcher = matcherResult.matcher;

			Set<String> varNames = matcherResult.variableNames;
			Map<String, Integer> slots = new HashMap<>();
			context.pushLocalScope();
			try {
				for (String varName : varNames) {
					context.addLocalVariable(varName);
					slots.put(varName, context.getVariableSlot(varName));
				}
				compiledMatcher = compiledMatcher.resolveSlots(new SlotResolver(slots));
				Expression<StackFrame, N> compiledReduce = compileNonNull(env, context, red.reduceExpr());
				return new ReduceExpression<>(env.getJsonProvider(), compiledMatcher, compiledInit, compiledReduce, compiledIter, new HashSet<>(slots.values()), context.outputCounterOf(compiledInit), context.outputCounterOf(compiledReduce), context.outputCounterOf(compiledIter));
			} finally {
				context.popScope();
			}
		}

		@Override
		public Expression<StackFrame, N> visit(ForeachExpressionAstNode fe) throws JsonQueryException {
			Expression<StackFrame, N> compiledIter = compileNonNull(env, context, fe.iterExpr());
			Expression<StackFrame, N> compiledInit = compileNonNull(env, context, fe.initExpr());
			CompiledMatcher<N> matcherResult = compileMatcher(fe.matcher());
			@Var PatternMatcher<N> compiledMatcher = matcherResult.matcher;

			Set<String> varNames = matcherResult.variableNames;
			Map<String, Integer> slots = new HashMap<>();
			context.pushLocalScope();
			try {
				for (String varName : varNames) {
					context.addLocalVariable(varName);
					slots.put(varName, context.getVariableSlot(varName));
				}
				compiledMatcher = compiledMatcher.resolveSlots(new SlotResolver(slots));
				Expression<StackFrame, N> compiledUpdate = compileNonNull(env, context, fe.updateExpr());
				Expression<StackFrame, N> compiledExtract = compile(env, context, fe.extractExpr());
				return new ForeachExpression<>(compiledMatcher, compiledInit, compiledUpdate, compiledExtract, compiledIter, new HashSet<>(slots.values()), context.outputCounterOf(compiledInit), context.outputCounterOf(compiledUpdate), context.outputCounterOf(compiledIter));
			} finally {
				context.popScope();
			}
		}

		@Override
		public Expression<StackFrame, N> visit(FormattingFilterAstNode ff) throws JsonQueryException {
			return compileFunctionCall(env, context, ff.signature(), Collections.emptyList());
		}

		@Override
		public Expression<StackFrame, N> visit(StringInterpolationAstNode si) throws JsonQueryException {
			List<Pair<Integer, Expression<StackFrame, N>>> compiledInterpolations = new ArrayList<>();
			for (Pair<Integer, AstNode> pair : si.interpolations())
				compiledInterpolations.add(Pair.of(pair._1, compileNonNull(env, context, pair._2)));
			Expression<StackFrame, N> compiledFormatter = compile(env, context, si.formatter());
			int[] interpolationOutputIndices = new int[compiledInterpolations.size()];
			for (int i = 0; i < interpolationOutputIndices.length; ++i)
				interpolationOutputIndices[i] = context.outputCounterOf(compiledInterpolations.get(i)._2);
			return new StringInterpolation<>(env.getJsonProvider(), si.template(), compiledInterpolations, compiledFormatter, env.getJqVersion(), interpolationOutputIndices, context.outputCounterOf(compiledFormatter));
		}

		@Override
		public Expression<StackFrame, N> visit(BracketFieldAccessAstNode bfa) throws JsonQueryException {
			Expression<StackFrame, N> target = compileNonNull(env, context, bfa.target());
			@Var Expression<StackFrame, N> start = compile(env, context, bfa.startExpr());
			@Var Expression<StackFrame, N> end = compile(env, context, bfa.endExpr());
			if (start == null)
				start = new ValueLiteral<>(env.getJsonProvider().createNull());
			if (end == null)
				end = new ValueLiteral<>(env.getJsonProvider().createNull());
			if (bfa.isRange()) {
				return new BracketFieldAccess<>(env.getJsonProvider(), target, start, end, bfa.permissive(), env.getJqVersion(), context.outputCounterOf(target), context.outputCounterOf(start), context.outputCounterOf(end));
			} else {
				return new BracketFieldAccess<>(env.getJsonProvider(), target, start, bfa.permissive(), env.getJqVersion(), context.outputCounterOf(target), context.outputCounterOf(start));
			}
		}

		@Override
		public Expression<StackFrame, N> visit(IdentifierFieldAccessAstNode ifa) throws JsonQueryException {
			Expression<StackFrame, N> target = compileNonNull(env, context, ifa.target());
			return new IdentifierFieldAccess<>(env.getJsonProvider(), target, ifa.field(), ifa.permissive(), env.getJqVersion(), context.outputCounterOf(target));
		}

		@Override
		public Expression<StackFrame, N> visit(StringFieldAccessAstNode sfa) throws JsonQueryException {
			Expression<StackFrame, N> target = compileNonNull(env, context, sfa.target());
			Expression<StackFrame, N> key = compileNonNull(env, context, sfa.key());
			return new StringFieldAccess<>(env.getJsonProvider(), target, key, sfa.permissive(), env.getJqVersion(), context.outputCounterOf(target), context.outputCounterOf(key));
		}

		@Override
		public Expression<StackFrame, N> visit(BracketExtractFieldAccessAstNode befa) throws JsonQueryException {
			Expression<StackFrame, N> target = compileNonNull(env, context, befa.target());
			return new BracketExtractFieldAccess<>(env.getJsonProvider(), target, befa.permissive(), env.getJqVersion(), context.outputCounterOf(target));
		}

		@Override
		public Expression<StackFrame, N> visit(BooleanLiteralAstNode ast) throws JsonQueryException {
			return new ValueLiteral<>(env.getJsonProvider().createBoolean(ast.value()));
		}

		@Override
		public Expression<StackFrame, N> visit(NumericLiteralAstNode ast) throws JsonQueryException {
			return new ValueLiteral<>(env.getJsonProvider().createNumber(new BigDecimal(ast.text())));
		}

		@Override
		public Expression<StackFrame, N> visit(NullLiteralAstNode ast) throws JsonQueryException {
			return new ValueLiteral<>(env.getJsonProvider().createNull());
		}

		@Override
		public Expression<StackFrame, N> visit(StringLiteralAstNode ast) throws JsonQueryException {
			return new ValueLiteral<>(env.getJsonProvider().createString(ast.value()));
		}

		@Override
		public Expression<StackFrame, N> visit(ThisObjectAstNode ast) throws JsonQueryException {
			return new ThisObject<>();
		}

		@Override
		public Expression<StackFrame, N> visit(RecursionOperatorAstNode ast) throws JsonQueryException {
			return new RecursionOperator<>(env.getJsonProvider(), env.getJqVersion().compareTo(Versions.JQ_1_6) >= 0);
		}

		@Override
		public Expression<StackFrame, N> visit(BreakExpressionAstNode ast) throws JsonQueryException {
			return new BreakExpression<>(ast.name());
		}

		@Override
		public Expression<StackFrame, N> visit(FunctionDefinitionAstNode fd) throws JsonQueryException {
			boolean isTopLevelDefinition = context.isRootScope();
			FunctionSignature signature = fd.signature();
			context.addLocalFunction(signature);
			// Before the body compiles, so a recursive call inside it can still tell which of this def's
			// parameters take a value and which take a filter -- which a tail call has to resolve itself.
			context.recordFunctionParameterNames(signature, fd.args());

			List<Integer> paramSlots = new ArrayList<>();
			int fnSize;
			int ownClosureSlot;
			int tailCallSlot;
			Expression<StackFrame, N> compiledBody;
			ClosureSpec closureSpec;
			context.pushFunctionScope();
			try {
				for (String arg : fd.args()) {
					if (arg.startsWith("$")) {
						context.addLocalVariable(arg.substring(1));
						paramSlots.add(context.getVariableSlot(arg.substring(1)));
					} else {
						FunctionSignature parameterSignature = FunctionSignature.of(arg, 0);
						context.addLocalFunction(parameterSignature);
						paramSlots.add(context.getFunctionSlot(parameterSignature));
					}
				}
				// After the parameter slots are assigned, so a tail call back into this def knows where to
				// write its new arguments -- which, besides jumping, is all such a call does.
				context.markDefinitionScope(signature, paramSlots);
				ownClosureSlot = context.reserveClosureSlot();
				// A def body is the one place tail position starts: what the body emits is what the call emits,
				// and the call's frame is gone by the time anything downstream sees a value.
				context.setTailPosition(true);
				compiledBody = compileNonNull(env, context, fd.body());
				fnSize = context.getSlotCount();
				closureSpec = context.getClosureSpec();
				tailCallSlot = context.currentScopeTailCallSlot();
			} finally {
				context.popScope();
			}
			int definerClosureSlot = context.getCurrentFunctionClosureSlot();

			SymbolLocation loc = context.getFunctionLocation(signature);
			int slot = loc != null ? loc.slot : 0;
			if (context.exportsTopLevelFunctions() && isTopLevelDefinition) {
				context.recordRootFunctionSlot(signature, slot);
			}
			context.markFoldBarrier();
			ResolvedFunctionDefinition<N> resolvedDef = new ResolvedFunctionDefinition<>(slot, closureSpec, fnSize, fd.args(), paramSlots, compiledBody, ownClosureSlot, definerClosureSlot, context.metersRuntimeBudgets(), tailCallSlot);
			// freeLocalSlots always come from resolvedDef's own closureSpec, which is already precise for
			// calls to *this* def -- including through nested defs in its body: resolving a deeper def's
			// own capture threads an entry through every intermediate function-boundary scope's
			// closureSpec as a side effect (see CompileContext#getVariableLocation), so this def's own
			// closureSpec already reflects what any nested def inside its body ultimately needs, with no
			// separate propagation required here.
			//
			// hasOpaqueVariableReference adds two more conservative sources resolvedDef's own (variables-
			// only) closureSpec can't see: (a) any local function captured across a closure boundary
			// (closureSpec.capturedFunctions()) -- e.g. a def that calls another def declared further out
			// (including itself, recursively) -- since we don't attempt to trace *that* function's own
			// transitive slot dependencies back through the boundary; (b) when the body has no local
			// variable capture at all, whatever FreeVariables says about compiledBody directly, to catch
			// dependencies closureSpec never tracks in the first place (a declared/global variable read).
			// The second check is skipped whenever there *is* a local capture to avoid double-counting the
			// unconditional opacity every ResolvedCapturedVariableAccess reports on its own (already
			// captured precisely, above) -- the one case this misses is a body mixing a local capture with
			// an untracked (e.g. global) read; accepted as a rare, deliberately conservative edge case.
			boolean hasOpaqueVariableReference = resolvedDef.hasOpaqueVariableReference()
					|| !closureSpec.capturedFunctions().isEmpty()
					|| (closureSpec.capturedVariables().isEmpty() && FreeVariables.dependsOnVariables(compiledBody));
			context.recordFunctionDependsOnInfo(signature,
					new FunctionDependsOnInfo(compiledBody.dependsOnInput(), compiledBody.dependsOnExternalState(), resolvedDef.freeLocalSlots(), hasOpaqueVariableReference));
			return resolvedDef;
		}

		@Override
		public CompiledMatcher<N> visit(ValueMatcherAstNode matcher) {
			return new CompiledMatcher<>(new ValueMatcher<>(matcher.name()), Collections.singleton(matcher.name()));
		}

		@Override
		public CompiledMatcher<N> visit(ArrayMatcherAstNode matcher) throws JsonQueryException {
			List<PatternMatcher<N>> compiled = new ArrayList<>();
			Set<String> variableNames = new HashSet<>();
			for (PatternMatcherAstNode element : matcher.matchers()) {
				CompiledMatcher<N> elementResult = compileMatcher(element);
				compiled.add(elementResult.matcher);
				variableNames.addAll(elementResult.variableNames);
			}
			return new CompiledMatcher<>(new ArrayMatcher<>(env.getJsonProvider(), compiled, env.getJqVersion()), variableNames);
		}

		@Override
		public CompiledMatcher<N> visit(ObjectMatcherAstNode matcher) throws JsonQueryException {
			List<ObjectMatcher.FieldMatcher<N>> compiled = new ArrayList<>();
			Set<String> variableNames = new HashSet<>();
			for (ObjectMatcherAstNode.FieldMatcher field : matcher.matchers()) {
				CompiledFieldMatcher<N> fieldResult = compileFieldMatcher(field);
				compiled.add(fieldResult.matcher);
				variableNames.addAll(fieldResult.variableNames);
			}
			return new CompiledMatcher<>(new ObjectMatcher<>(env.getJsonProvider(), compiled, env.getJqVersion()), variableNames);
		}

		@Override
		public CompiledFieldMatcher<N> visit(ObjectMatcherAstNode.ConstantKeyFieldMatcher field) throws JsonQueryException {
			Expression<StackFrame, N> name = new ValueLiteral<>(env.getJsonProvider().createString(field.name()));
			PatternMatcherAstNode sub = field.matcher();
			CompiledMatcher<N> subResult = sub != null ? compileMatcher(sub) : null;
			Set<String> variableNames = subResult != null ? new HashSet<>(subResult.variableNames) : new HashSet<>();
			if (field.dollar())
				variableNames.add(field.name());
			ObjectMatcher.FieldMatcher<N> compiled = new ObjectMatcher.FieldMatcher<>(field.dollar(), field.dollar() ? field.name() : null, name, subResult != null ? subResult.matcher : null, context.outputCounterOf(name));
			return new CompiledFieldMatcher<>(compiled, variableNames);
		}

		@Override
		public CompiledFieldMatcher<N> visit(ObjectMatcherAstNode.ExpressionKeyFieldMatcher field) throws JsonQueryException {
			Expression<StackFrame, N> name = compileNonNull(env, context, field.name());
			CompiledMatcher<N> matcherResult = compileMatcher(field.matcher());
			ObjectMatcher.FieldMatcher<N> compiled = new ObjectMatcher.FieldMatcher<>(false, null, name, matcherResult.matcher, context.outputCounterOf(name));
			return new CompiledFieldMatcher<>(compiled, matcherResult.variableNames);
		}
	}

	/**
	 * Looks up a function by name/arity in a single map: exact-arity match first, then variadic-arity
	 * fallback (a registration accepting any arity).
	 */
	private static @Nullable Function lookupFunction(Map<FunctionSignature, Function> functions, FunctionSignature signature) {
		Function factory = functions.get(signature);
		if (factory != null)
			return factory;
		return functions.get(signature.asVariadic());
	}

	/**
	 * Exact-then-variadic lookup of {@code name}/{@code arity} against {@code env.getDeclaredFunctions()},
	 * mirroring {@link #lookupFunction}'s exact-then-variadic pattern for the defined/builtin registries.
	 */
	private static @Nullable FunctionSignature resolveDeclaredFunctionKey(Environment<?> env, FunctionSignature signature) {
		if (env.getDeclaredFunctions().contains(signature))
			return signature;
		FunctionSignature variadic = signature.asVariadic();
		return env.getDeclaredFunctions().contains(variadic) ? variadic : null;
	}

	/**
	 * Compiles a (non-module-qualified) function call/formatting-filter reference: local/captured {@code def}
	 * first, then a declared (no compile-time implementation, resolved via a {@link Memory} global slot
	 * at runtime) global, then a defined/builtin (fixed at compile time) global. Declared is checked before
	 * environment-defined, jq-library, or Java builtin registries so that a declared signature coinciding
	 * with a real builtin still requires a binding rather than silently falling back to the builtin.
	 * <p>
	 * The defined/builtin step searches the environment's own registries first and then each
	 * {@code FunctionLoader} in the order it was added, and within every one of those tiers applies the same
	 * rule: exact-signature Java function, then exact-signature jq definition, then variadic Java function.
	 * Whether a function is written in Java or in jq is a detail of how it was supplied, so it must not decide
	 * the winner differently in one tier than in another; keep the two blocks below symmetric.
	 * <p>
	 * A loader tier is consulted whole before the next one is asked, exactly as a module loader is: the first
	 * loader that supplies the name at any of those three steps answers the call, so an earlier loader's
	 * variadic function beats a later loader's exact one.
	 */
	private static <N> Expression<StackFrame, N> compileFunctionCall(Environment<N> env, CompileContext context, FunctionSignature signature, List<Expression<StackFrame, N>> compiledArgs) throws JsonQueryException {
		return compileFunctionCall(env, context, signature, context.getFunctionLocation(signature), compiledArgs);
	}

	/**
	 * As above, for the one caller that needs the resolved {@link SymbolLocation} for itself as well.
	 * <p>
	 * It has to be passed in rather than resolved again here, because resolving it twice does not give the
	 * same answer: the first walk threads a capture chain through every intervening closure, and a second
	 * finds that chain already in place and answers from it -- same slot, but without the definition's
	 * dependency facts or its parameter names.
	 */
	private static <N> Expression<StackFrame, N> compileFunctionCall(Environment<N> env, CompileContext context, FunctionSignature signature, @Nullable SymbolLocation loc, List<Expression<StackFrame, N>> compiledArgs) throws JsonQueryException {
		String fullName = signature.name();
		BindContext<N> bindContext = bindContextOf(env);
		if (loc != null) {
			int slot = loc.slot;
			FunctionDependsOnInfo info = loc.dependsOnInfo;
			BoundArgumentInfo boundArgumentInfo = loc.boundArgumentInfo;
			Expression<StackFrame, N> precomputed = boundArgumentInfo != null && compiledArgs.isEmpty() ? precomputedBoundArgument(boundArgumentInfo) : null;
			if (precomputed != null)
				return precomputed;
			if (!loc.isLocal) {
				return boundArgumentInfo != null
						? new ResolvedCapturedFunctionBoundArgumentAccess<>(bindContext, fullName, slot, context.getCurrentFunctionClosureSlot(), compiledArgs, boundArgumentInfo)
						: new ResolvedCapturedFunctionAccess<>(bindContext, fullName, slot, context.getCurrentFunctionClosureSlot(), compiledArgs, info);
			}
			return boundArgumentInfo != null
					? new ResolvedLocalFunctionBoundArgumentAccess<>(bindContext, fullName, slot, compiledArgs, boundArgumentInfo)
					: new ResolvedLocalFunctionAccess<>(bindContext, fullName, slot, compiledArgs, info);
		}

		FunctionSignature declaredKey = resolveDeclaredFunctionKey(env, signature);
		if (declaredKey != null) {
			int globalIndex = context.getOrAssignGlobalFunctionIndex(declaredKey);
			return new ResolvedGlobalFunctionAccess<>(bindContext, fullName, globalIndex, compiledArgs);
		}

		Map<FunctionSignature, Function> envFunctions = env.getFunctions();
		@Var Function factory = envFunctions.get(signature);
		if (factory == null) {
			JqFunction jqFunction = env.getJqFunctions().get(signature);
			if (jqFunction != null)
				return JqFunctionCompiler.compile(env, context, signature, jqFunction, JqFunctionCompiler.Origin.ENVIRONMENT, compiledArgs);
			factory = envFunctions.get(signature.asVariadic());
		}
		if (factory != null)
			return bindFunctionCall(bindContext, factory, compiledArgs);

		for (FunctionLoader loader : env.getFunctionLoaders()) {
			Map<FunctionSignature, Function> loadedFunctions = loader.getFunctions(env.getJqVersion());
			@Var
			Function loaded = loadedFunctions.get(signature);
			if (loaded == null) {
				JqFunction jqFunction = loader.getJqFunctions(env.getJqVersion()).get(signature);
				if (jqFunction != null)
					return JqFunctionCompiler.compile(env, context, signature, jqFunction, JqFunctionCompiler.Origin.LOADER, compiledArgs);
				loaded = loadedFunctions.get(signature.asVariadic());
			}
			if (loaded != null)
				return bindFunctionCall(bindContext, loaded, compiledArgs);
		}
		throw new JsonQueryException(String.format("Function %s does not exist", signature));
	}

	/**
	 * The bind-time view of {@code env}, as every {@link Function} in a query compiled against it sees it.
	 * <p>
	 * One is built per call site while compiling and then held by the bound expression, so the run-time
	 * bind path -- {@code Resolved*FunctionAccess.apply}, which re-binds on every evaluation -- allocates
	 * nothing.
	 */
	private static <N> BindContext<N> bindContextOf(Environment<N> env) {
		JsonProvider<N> jsonProvider = env.getJsonProvider();
		Version jqVersion = env.getJqVersion();
		return new BindContext<N>() {
			@Override
			public JsonProvider<N> getJsonProvider() {
				return jsonProvider;
			}

			@Override
			public Version getJqVersion() {
				return jqVersion;
			}
		};
	}

	/**
	 * Binds a resolved Java {@link Function} to the call's already-compiled arguments.
	 */
	private static <N> Expression<StackFrame, N> bindFunctionCall(BindContext<N> bindContext, Function factory, List<Expression<StackFrame, N>> compiledArgs) throws JsonQueryException {
		Expression<StackFrame, N> fn = factory.bind(bindContext, compiledArgs);
		return new ResolvedFunctionCall<>(fn, fn.dependsOnExternalState(), fn.dependsOnInput(), compiledArgs);
	}

	/**
	 * A variable's default supplier, whether registered on the {@code Environment} or (for {@code $}-style
	 * data imports, which the compiler must not register on {@code Environment} itself) collected on
	 * {@code context} via {@link CompileContext#addImportedVariableDefault}.
	 */
	private static <N> @Nullable Supplier<N> resolveVariableSupplier(Environment<N> env, CompileContext context, String name) {
		Supplier<N> supplier = env.getVariables().get(name);
		if (supplier != null)
			return supplier;
		N constant = env.getConstants().get(name);
		if (constant != null)
			return () -> constant;
		if (!context.importedVariableDefaults().containsKey(name))
			return null;
		@SuppressWarnings("unchecked")
		N value = (N) context.importedVariableDefaults().get(name);
		return () -> value;
	}

	/**
	 * Resolves {@code bareName} (the plain, unqualified Environment-registered name) as a global variable --
	 * declared first (a {@link Memory} global slot, checked first for the same reason as
	 * {@link #compileFunctionCall}), then defined/constant/imported (fixed at compile time). Returns
	 * {@code null} if {@code bareName} isn't a global at all. {@code displayName} is what the user actually
	 * wrote ({@code name} or the self-qualified {@code name::name} form), used only for error/{@code toString()}
	 * purposes.
	 */
	private static <N> @Nullable Expression<StackFrame, N> compileGlobalVariableAccess(Environment<N> env, CompileContext context, String displayName, String bareName) {
		if (env.getDeclaredVariables().contains(bareName)) {
			int globalIndex = context.getOrAssignGlobalVariableIndex(bareName);
			return new ResolvedGlobalVariableAccess<>(displayName, globalIndex);
		}
		Supplier<N> supplier = resolveVariableSupplier(env, context, bareName);
		if (supplier != null)
			return new ResolvedFixedVariableAccess<>(displayName, supplier);
		return null;
	}

	private static <N> Expression<StackFrame, N> compileVariableRef(Environment<N> env, CompileContext context, @Nullable String moduleName, String varName) throws JsonQueryException {
		if (moduleName != null) {
			String fullName = moduleName + "::" + varName;
			if (context.isLocalVariable(fullName)) {
				SymbolLocation loc = context.getVariableLocation(fullName);
				int slot = loc != null ? loc.slot : 0;
				BoundArgumentInfo boundArgumentInfo = loc != null ? loc.boundArgumentInfo : null;
				Expression<StackFrame, N> precomputed = boundArgumentInfo != null ? precomputedBoundArgument(boundArgumentInfo) : null;
				if (precomputed != null)
					return precomputed;
				if (loc != null && !loc.isLocal) {
					return boundArgumentInfo != null
							? new ResolvedCapturedVariableBoundArgumentAccess<>(fullName, slot, context.getCurrentFunctionClosureSlot(), boundArgumentInfo)
							: new ResolvedCapturedVariableAccess<>(fullName, slot, context.getCurrentFunctionClosureSlot());
				}
				return boundArgumentInfo != null
						? new ResolvedLocalVariableBoundArgumentAccess<>(fullName, slot, boundArgumentInfo)
						: new ResolvedLocalVariableAccess<>(fullName, slot);
			}
			if (moduleName.equals(varName)) {
				Expression<StackFrame, N> global = compileGlobalVariableAccess(env, context, fullName, varName);
				if (global != null)
					return global;
			}
			throw new JsonQueryException(String.format("Variable $%s::%s is not defined", moduleName, varName));
		}

		if (context.isLocalVariable(varName)) {
			SymbolLocation loc = context.getVariableLocation(varName);
			int slot = loc != null ? loc.slot : 0;
			BoundArgumentInfo boundArgumentInfo = loc != null ? loc.boundArgumentInfo : null;
			Expression<StackFrame, N> precomputed = boundArgumentInfo != null ? precomputedBoundArgument(boundArgumentInfo) : null;
			if (precomputed != null)
				return precomputed;
			if (loc != null && !loc.isLocal) {
				return boundArgumentInfo != null
						? new ResolvedCapturedVariableBoundArgumentAccess<>(varName, slot, context.getCurrentFunctionClosureSlot(), boundArgumentInfo)
						: new ResolvedCapturedVariableAccess<>(varName, slot, context.getCurrentFunctionClosureSlot());
			}
			return boundArgumentInfo != null
					? new ResolvedLocalVariableBoundArgumentAccess<>(varName, slot, boundArgumentInfo)
					: new ResolvedLocalVariableAccess<>(varName, slot);
		}

		Expression<StackFrame, N> global = compileGlobalVariableAccess(env, context, varName, varName);
		if (global != null)
			return global;

		throw new JsonQueryException(String.format("Variable $%s is not defined", varName));
	}

	private static final Object[] NO_ARGUMENTS = new Object[0];

	/**
	 * Works out what goes in each of a callee's parameter slots, and hands it over once per combination of
	 * values its {@code $} parameters' arguments produce.
	 * <p>
	 * Nothing here touches a frame. A filter parameter becomes a {@link Function} closing over the argument
	 * expression and the <em>caller's</em> frame -- jq's call-by-name -- and a {@code $} parameter becomes a
	 * value the argument emitted, evaluated against that same caller frame. Neither needs the callee's frame
	 * to exist yet, which is what lets the callee decide when to push one, reuse one, or replace one. See
	 * {@code ResolvedFunctionDefinition}'s loop.
	 * <p>
	 * The array is reused between combinations: {@code bodyTask} installs the entries into a frame before
	 * running anything, so the next combination is free to overwrite it.
	 *
	 * @param callerFrame the frame the arguments are evaluated against
	 * @param paramNames the callee's declared parameter names, {@code $}-prefixed for a value parameter
	 * @param fnArgs the call site's argument expressions, one per parameter
	 * @param in the caller's input
	 * @param path the caller's input path
	 * @param bodyTask receives one parameter array per combination
	 * @throws JsonQueryException if evaluating an argument does
	 */
	public static <N> void bindParameters(StackFrame callerFrame, List<String> paramNames, List<Expression<StackFrame, N>> fnArgs, N in, Path<N> path, Consumer<Object[]> bodyTask) throws JsonQueryException {
		if (paramNames.isEmpty()) {
			bodyTask.accept(NO_ARGUMENTS);
			return;
		}
		Object[] arguments = new Object[paramNames.size()];
		for (int i = 0; i < paramNames.size(); i++) {
			if (!paramNames.get(i).startsWith("$"))
				arguments[i] = boundFilter(callerFrame, fnArgs.get(i));
		}
		bindValueParameters(callerFrame, paramNames, fnArgs, 0, in, path, arguments, bodyTask);
	}

	private static <N> void bindValueParameters(StackFrame callerFrame, List<String> paramNames, List<Expression<StackFrame, N>> fnArgs, int index, N in, Path<N> path, Object[] arguments, Consumer<Object[]> bodyTask) throws JsonQueryException {
		if (index >= paramNames.size()) {
			bodyTask.accept(arguments);
			return;
		}
		if (!paramNames.get(index).startsWith("$")) {
			bindValueParameters(callerFrame, paramNames, fnArgs, index + 1, in, path, arguments, bodyTask);
			return;
		}
		fnArgs.get(index).apply(callerFrame, in, path, (value, valuePath) -> {
			arguments[index] = StackFrameValues.toSlot(value);
			bindValueParameters(callerFrame, paramNames, fnArgs, index + 1, in, path, arguments, bodyTask);
		});
	}

	private static <N> Function boundFilter(StackFrame callerFrame, Expression<StackFrame, N> argument) {
		return new Function() {
			@Override
			@SuppressWarnings("unchecked")
			public <Context extends RuntimeContext, N1> Expression<Context, N1> bind(BindContext<N1> bindCtx, List<Expression<Context, N1>> emptyArgs) {
				Expression<StackFrame, N1> effectiveArgument = (Expression<StackFrame, N1>) (Expression<?, ?>) argument;
				return (sFrame, inVal, pVal, outVal) -> effectiveArgument.apply(callerFrame, inVal, pVal, outVal);
			}
		};
	}

	public static <JsonNode> Expression<StackFrame, JsonNode> compileNonNull(Environment<JsonNode> env, CompileContext context, AstNode ast) throws JsonQueryException {
		return compileNonNull(env, context, ModuleScope.root(env), ast);
	}

	public static <JsonNode> Expression<StackFrame, JsonNode> compileNonNull(Environment<JsonNode> env, CompileContext context, ModuleScope<JsonNode> scope, AstNode ast) throws JsonQueryException {
		Expression<StackFrame, JsonNode> compiled = compile(env, context, scope, ast);
		if (compiled == null)
			throw new JsonQueryException("Cannot resolve null expression");
		return compiled;
	}

	public static <JsonNode> Maybe<JsonNode> evaluateMetadata(JsonProvider<JsonNode> jsonProvider, @Nullable AstNode metadataExpr) {
		if (metadataExpr == null)
			return Maybe.absent();
		Maybe<JsonNode> metadata = ExpressionUtils.evaluateLiteralExpression(jsonProvider, metadataExpr);
		if (metadata.isAbsent())
			throw new IllegalArgumentException("Module metadata must be constant");
		if (!jsonProvider.isObject(metadata.get()))
			throw new IllegalArgumentException("Module metadata must be an object");
		return metadata;
	}

	public static <JsonNode> Maybe<JsonNode> evaluateMetadata(JsonProvider<JsonNode> jsonProvider, TopLevelAstNode.ImportStatement statement) {
		return evaluateMetadata(jsonProvider, statement.metadataExpr());
	}

	public static <JsonNode> JsonNode evaluateMetadata(JsonProvider<JsonNode> jsonProvider, TopLevelAstNode.ModuleDirective directive) {
		return evaluateMetadata(jsonProvider, directive.metadataExpr()).get();
	}

	public static <JsonNode> Expression<StackFrame, JsonNode> compileBinaryOperator(
			BinaryOperator operator,
			Expression<StackFrame, JsonNode> lhs,
			Expression<StackFrame, JsonNode> rhs,
			int lhsOutputIndex,
			int rhsOutputIndex,
			Version version,
			JsonProvider<JsonNode> jsonProvider) {
		switch (operator) {
			case ASSIGN:
				return new Assignment<>(jsonProvider, lhs, rhs, version, lhsOutputIndex, rhsOutputIndex);
			case UPDATE:
				return new UpdateAssignment<>(jsonProvider, lhs, rhs, version, lhsOutputIndex, rhsOutputIndex);
			case DEFAULT_EQUAL:
				return new ComplexAlternativeAssignment<>(jsonProvider, lhs, rhs, version, lhsOutputIndex, rhsOutputIndex);
			case PLUS_EQUAL:
				return new ComplexPlusAssignment<>(jsonProvider, lhs, rhs, version, lhsOutputIndex, rhsOutputIndex);
			case MINUS_EQUAL:
				return new ComplexMinusAssignment<>(jsonProvider, lhs, rhs, version, lhsOutputIndex, rhsOutputIndex);
			case TIMES_EQUAL:
				return new ComplexMultiplyAssignment<>(jsonProvider, lhs, rhs, version, lhsOutputIndex, rhsOutputIndex);
			case DIVIDE_EQUAL:
				return new ComplexDivideAssignment<>(jsonProvider, lhs, rhs, version, lhsOutputIndex, rhsOutputIndex);
			case MODULO_EQUAL:
				return new ComplexModuloAssignment<>(jsonProvider, lhs, rhs, version, lhsOutputIndex, rhsOutputIndex);
			case DEFAULT:
				return new AlternativeOperatorExpression<>(jsonProvider, lhs, rhs, lhsOutputIndex, rhsOutputIndex);
			case OR:
				return new BooleanOrExpression<>(jsonProvider, lhs, rhs, lhsOutputIndex, rhsOutputIndex);
			case AND:
				return new BooleanAndExpression<>(jsonProvider, lhs, rhs, lhsOutputIndex, rhsOutputIndex);
			case LESS_EQUAL:
				return new CompareLessEqualTest<>(jsonProvider, lhs, rhs, lhsOutputIndex, rhsOutputIndex);
			case LESS:
				return new CompareLessTest<>(jsonProvider, lhs, rhs, lhsOutputIndex, rhsOutputIndex);
			case GREATER_EQUAL:
				return new CompareGreaterEqualTest<>(jsonProvider, lhs, rhs, lhsOutputIndex, rhsOutputIndex);
			case GREATER:
				return new CompareGreaterTest<>(jsonProvider, lhs, rhs, lhsOutputIndex, rhsOutputIndex);
			case EQUAL:
				return new CompareEqualTest<>(jsonProvider, lhs, rhs, lhsOutputIndex, rhsOutputIndex);
			case NOT_EQUAL:
				return new CompareNotEqualTest<>(jsonProvider, lhs, rhs, lhsOutputIndex, rhsOutputIndex);
			case PLUS:
				return new PlusExpression<>(jsonProvider, lhs, rhs, version, lhsOutputIndex, rhsOutputIndex);
			case MINUS:
				return new MinusExpression<>(jsonProvider, lhs, rhs, version, lhsOutputIndex, rhsOutputIndex);
			case MODULO:
				return new ModuloExpression<>(jsonProvider, lhs, rhs, version, lhsOutputIndex, rhsOutputIndex);
			case DIVIDE:
				return new DivideExpression<>(jsonProvider, lhs, rhs, version, lhsOutputIndex, rhsOutputIndex);
			case TIMES:
				return new MultiplyExpression<>(jsonProvider, lhs, rhs, version, lhsOutputIndex, rhsOutputIndex);
			default:
				throw new IllegalArgumentException("Unknown operator: " + operator);
		}
	}
}
