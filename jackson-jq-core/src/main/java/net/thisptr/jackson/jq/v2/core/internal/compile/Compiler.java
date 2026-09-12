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
import net.thisptr.jackson.jq.v2.core.diagnostic.DiagnosticListener;
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
import net.thisptr.jackson.jq.v2.core.internal.diagnostics.PipeParenthesesCheck;
import net.thisptr.jackson.jq.v2.core.internal.memory.Memory;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.tree.ArrayConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.BreakExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.Comma;
import net.thisptr.jackson.jq.v2.core.internal.tree.Conditional;
import net.thisptr.jackson.jq.v2.core.internal.tree.FieldConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.FixedInputExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.ForeachExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.IdentifierKeyFieldConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.JsonQueryKeyFieldConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.Label;
import net.thisptr.jackson.jq.v2.core.internal.tree.NegativeExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.ObjectConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.PipedQuery;
import net.thisptr.jackson.jq.v2.core.internal.tree.PrecomputedConstantExpression;
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
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.matchers.ArrayMatcher;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.matchers.ObjectMatcher;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.matchers.ValueMatcher;
import net.thisptr.jackson.jq.v2.core.internal.utils.ExpressionUtils;
import net.thisptr.jackson.jq.v2.core.internal.utils.StackFrameValues;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.Maybe;
import net.thisptr.jackson.jq.v2.spi.ConstantExpression;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.JqFunction;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class Compiler {
	private static final int MAX_PRECOMPUTED_RESULTS = 256;

	private static final class TooManyConstantResultsException extends JsonQueryException {
		private static final long serialVersionUID = 1L;

		TooManyConstantResultsException() {
			super(String.format("constant expression produced more than %d values", MAX_PRECOMPUTED_RESULTS));
		}
	}

	private static <N> List<Expression<StackFrame, N>> precomputeConstantArguments(Environment<N> env, CompileContext context, List<Expression<StackFrame, N>> args) {
		List<Expression<StackFrame, N>> result = new ArrayList<>(args.size());
		for (Expression<StackFrame, N> arg : args)
			result.add(precomputeConstantArgument(env, context, arg));
		return result;
	}

	private static <N> Expression<StackFrame, N> precomputeConstantArgument(Environment<N> env, CompileContext context, Expression<StackFrame, N> expression) {
		if (expression instanceof ConstantExpression<?, ?> || expression.dependsOnInput() || expression.dependsOnExternalState() || FreeVariables.dependsOnVariables(expression))
			return expression;

		Memory memory = new Memory(context.getGlobalCount());
		StackFrame frame = memory.pushFrame(context.getSlotCount());
		List<N> results = new ArrayList<>();
		try {
			expression.apply(frame, env.getJsonProvider().createNull(), UntrackedPath.getInstance(), (value, path) -> {
				if (results.size() == MAX_PRECOMPUTED_RESULTS)
					throw new TooManyConstantResultsException();
				results.add(value);
			});
		} catch (TooManyConstantResultsException ignored) {
			return expression;
		} catch (JsonQueryException ignored) {
			// Preserve jq's lazy error behavior. The expression may be unreachable or evaluated
			// under try/catch at runtime, so a failed speculative evaluation is not foldable.
			return expression;
		} finally {
			memory.popFrame();
		}
		return new PrecomputedConstantExpression<>(env.getJsonProvider(), expression, results);
	}

	private static <N> Expression<StackFrame, N> restoreFixedInput(Expression<StackFrame, N> expression, boolean inputFixed) {
		return inputFixed && expression.dependsOnInput() ? new FixedInputExpression<>(expression) : expression;
	}

	@SuppressWarnings("unchecked")
	private static <N> @Nullable Expression<StackFrame, N> precomputedBoundArgument(BoundArgumentInfo info) {
		Expression<?, ?> expression = info.precomputedExpression();
		return expression == null ? null : (Expression<StackFrame, N>) expression;
	}

	public static <JsonNode> Expression<StackFrame, JsonNode> compile(Environment<JsonNode> env, AstNode ast) throws JsonQueryException {
		return compile(env, new CompileOptions(), (Module) null, ast);
	}

	public static <JsonNode> Expression<StackFrame, JsonNode> compile(Environment<JsonNode> env, CompileOptions options, @Nullable Module currentModule, AstNode ast) throws JsonQueryException {
		return compileRoot(env, options, currentModule, ast, false);
	}

	/**
	 * Compiles a module's own defining source. Unlike {@link #compile(Environment, Module, AstNode)}, this
	 * tracks the module's genuinely top-level {@code def}s (see {@link CompileContext#exportsTopLevelFunctions()}
	 * / {@link CompileContext#isRootScope()}) so {@link RootExpression#applyForModuleExports} can harvest their
	 * real, correctly closure-bound {@link Function} values after running the module body once. This is how
	 * {@code FileSystemModuleLoader.loadModuleActual} populates a file-based module's exported functions.
	 * Ordinary query compilation must never do this -- use {@link #compile(Environment, CompileOptions, Module, AstNode)}.
	 */
	public static <JsonNode> Expression<StackFrame, JsonNode> compileModule(Environment<JsonNode> env, CompileOptions options, @Nullable Module currentModule, AstNode ast) throws JsonQueryException {
		return compileRoot(env, options, currentModule, ast, true);
	}

	private static <JsonNode> Expression<StackFrame, JsonNode> compileRoot(Environment<JsonNode> env, CompileOptions options, @Nullable Module currentModule, AstNode ast, boolean exportTopLevelFunctions) throws JsonQueryException {
		// Only whole queries and module sources are diagnosed. Function bodies that jq libraries
		// bring along are compiled through the inner compile() below, never through here, so a
		// caller never sees warnings about jq's own builtins.
		DiagnosticListener diagnosticListener = options.getDiagnosticListener();
		if (diagnosticListener != null)
			PipeParenthesesCheck.run(ast, diagnosticListener);

		CompileContext context = new CompileContext(exportTopLevelFunctions);
		Expression<StackFrame, JsonNode> compiled = compile(env, context, currentModule, ast);
		if (compiled == null)
			throw new JsonQueryException("Cannot resolve null expression");
		Set<String> definedVariables = new HashSet<>(env.getVariables().keySet());
		definedVariables.addAll(env.getConstants().keySet());
		definedVariables.addAll(context.importedVariableDefaults().keySet());
		Set<FunctionSignature> definedFunctions = new HashSet<>(env.getFunctions().keySet());
		definedFunctions.addAll(env.getJqFunctions().keySet());
		return new RootExpression<>(context.getSlotCount(), context.getGlobalCount(), compiled,
				definedVariables, definedFunctions,
				context.globalVariableIndices(), context.globalFunctionIndices(),
				env.getDeclaredVariables(), env.getDeclaredFunctions(), context.rootFunctionSlots());
	}

	public static <JsonNode> @Nullable Expression<StackFrame, JsonNode> compile(Environment<JsonNode> env, CompileContext context, @Nullable AstNode ast) throws JsonQueryException {
		return compile(env, context, (Module) null, ast);
	}

	public static <JsonNode> @Nullable Expression<StackFrame, JsonNode> compile(Environment<JsonNode> env, CompileContext context, @Nullable Module currentModule, @Nullable AstNode ast) throws JsonQueryException {
		if (ast == null)
			return null;
		return new CompilationVisitor<>(env, context, currentModule).compileExpression(ast);
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
		private final @Nullable Module currentModule;

		CompilationVisitor(Environment<N> env, CompileContext context, @Nullable Module currentModule) {
			this.env = env;
			this.context = context;
			this.currentModule = currentModule;
		}

		private Expression<StackFrame, N> compileExpression(AstNode ast) throws JsonQueryException {
			return accept(ast, Expression.class);
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
			return compileNonNull(env, context, currentModule, paren.value());
		}

		@Override
		public Expression<StackFrame, N> visit(FunctionCallAstNode call) throws JsonQueryException {
			boolean inputFixed = context.isInputFixed();
			@Var List<Expression<StackFrame, N>> compiledArgs = new ArrayList<>();
			context.setInputFixed(false);
			try {
				for (AstNode arg : call.args()) {
					compiledArgs.add(compile(env, context, currentModule, arg));
				}
			} finally {
				context.setInputFixed(inputFixed);
			}
			compiledArgs = Collections.unmodifiableList(precomputeConstantArguments(env, context, compiledArgs));

			if (call.moduleName() != null) {
				@Var Module mod = context.getImportedModule(call.moduleName());
				if (mod == null)
					mod = env.getImportedModules().get(call.moduleName());
				Function factory = mod != null ? lookupFunction(mod.getFunctions(), call.name(), compiledArgs.size()) : null;
				if (factory == null) {
					throw new JsonQueryException(String.format("Function %s::%s/%d does not exist", call.moduleName(), call.name(), compiledArgs.size()));
				}
				Expression<StackFrame, N> fn = factory.bindArguments(env.getJsonProvider(), compiledArgs, env.getJqVersion());
				Expression<StackFrame, N> result = new ResolvedFunctionCall<>(fn, fn.dependsOnExternalState(), fn.dependsOnInput(), inputFixed, compiledArgs);
				return restoreFixedInput(result, inputFixed);
			}

			return restoreFixedInput(compileFunctionCall(env, context, call.name(), compiledArgs), inputFixed);
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
					Maybe<N> data = env.getModuleLoader().loadData(currentModule, imp.path, metadata);
					if (data.isAbsent()) {
						throw new JsonQueryException(String.format("module not found: %s", imp.path));
					}
					if (imp.name != null) {
						context.addImportedVariableDefault(imp.name, data.get());
					}
				} else {
					Module mod = env.getModuleLoader().loadModule(currentModule, imp.path, metadata);
					if (mod == null) {
						throw new JsonQueryException(String.format("module not found: %s", imp.path));
					}
					if (imp.name != null) {
						context.addImportedModule(imp.name, mod);
					}
				}
			}
			Expression<StackFrame, N> compiledInner = compileNonNull(env, context, currentModule, top.expr());
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

			boolean savedInputFixed = context.isInputFixed();
			Expression<StackFrame, N> compiledLeft = compileNonNull(env, context, currentModule, left);
			Expression<StackFrame, N> right;
			context.setInputFixed(!compiledLeft.dependsOnInput());
			try {
				right = compileNonNull(env, context, currentModule, piped.rhs);
			} finally {
				context.setInputFixed(savedInputFixed);
			}
			return new PipedQuery<>(compiledLeft, right);
		}

		private Expression<StackFrame, N> compileAsBinding(AsBindingAstNode binding, AstNode bodyAst) throws JsonQueryException {
			boolean savedInputFixed = context.isInputFixed();
			Expression<StackFrame, N> value = compileNonNull(env, context, currentModule, binding.value());
			CompiledMatcher<N> matcherResult = compileMatcher(binding.matcher());

			context.pushLocalScope();
			Map<String, Integer> slots = new HashMap<>();
			Expression<StackFrame, N> body;
			try {
				for (String varName : matcherResult.variableNames) {
					context.addLocalVariable(varName);
					slots.put(varName, context.getVariableSlot(varName));
				}
				context.setInputFixed(savedInputFixed);
				body = compileNonNull(env, context, currentModule, bodyAst);
			} finally {
				context.setInputFixed(savedInputFixed);
				context.popScope();
			}
			PatternMatcher<N> compiledMatcher = matcherResult.matcher.resolveSlots(slots);
			return new VariableBinding<>(value, compiledMatcher, new HashSet<>(slots.values()), body);
		}

		private Expression<StackFrame, N> compileLabel(LabelAstNode label, AstNode bodyAst) throws JsonQueryException {
			return new Label<>(label.name(), compileNonNull(env, context, currentModule, bodyAst));
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
			for (AstNode q : semi.expressions()) {
				newExpressions.add(compileNonNull(env, context, q));
			}
			return new SemicolonOperator<>(newExpressions);
		}

		@Override
		public Expression<StackFrame, N> visit(ObjectConstructionAstNode obj) throws JsonQueryException {
			ObjectConstruction<N> res = new ObjectConstruction<>(env.getJsonProvider());
			for (ObjectConstructionAstNode.FieldConstructionAst fc : obj.fields) {
				res.add(compileField(fc));
			}
			return res;
		}

		@Override
		public FieldConstruction<N> visit(ObjectConstructionAstNode.IdentifierKeyFieldConstructionAst field) throws JsonQueryException {
			Expression<StackFrame, N> value = compile(env, context, field.value);
			return new IdentifierKeyFieldConstruction<>(env.getJsonProvider(), field.key, value, env.getJqVersion());
		}

		@Override
		public FieldConstruction<N> visit(ObjectConstructionAstNode.JsonQueryKeyFieldConstructionAst field) throws JsonQueryException {
			Expression<StackFrame, N> key = compileNonNull(env, context, field.key());
			Expression<StackFrame, N> value = compileNonNull(env, context, field.value());
			return new JsonQueryKeyFieldConstruction<>(env.getJsonProvider(), key, value, env.getJqVersion());
		}

		@Override
		public FieldConstruction<N> visit(ObjectConstructionAstNode.StringKeyFieldConstructionAst field) throws JsonQueryException {
			Expression<StackFrame, N> key = compileNonNull(env, context, field.key);
			Expression<StackFrame, N> value = compile(env, context, field.value);
			return new StringKeyFieldConstruction<>(env.getJsonProvider(), key, value, env.getJqVersion());
		}

		@Override
		public FieldConstruction<N> visit(ObjectConstructionAstNode.VariableKeyFieldConstruction field) throws JsonQueryException {
			// Desugar `{ $x }` into the same shape as `{ x: $x }` -- no dedicated resolved class needed.
			Expression<StackFrame, N> value = compileVariableRef(env, context, null, field.name());
			return new IdentifierKeyFieldConstruction<>(env.getJsonProvider(), field.name(), value, env.getJqVersion());
		}

		@Override
		public Expression<StackFrame, N> visit(ArrayConstructionAstNode arr) throws JsonQueryException {
			return new ArrayConstruction<>(env.getJsonProvider(), compile(env, context, arr.q));
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
			boolean savedInputFixed = context.isInputFixed();
			if (bin.operator == BinaryOperator.UPDATE) {
				// `|=`'s rhs is rebound to the value at the resolved path, not `.`.
				context.setInputFixed(savedInputFixed && !lhs.dependsOnInput());
			}
			Expression<StackFrame, N> rhs;
			try {
				rhs = compileNonNull(env, context, bin.rhs);
			} finally {
				context.setInputFixed(savedInputFixed);
			}
			return compileBinaryOperator(bin.operator, lhs, rhs, env.getJqVersion(), env.getJsonProvider(), savedInputFixed);
		}

		@Override
		public Expression<StackFrame, N> visit(NegativeExpressionAstNode neg) throws JsonQueryException {
			return new NegativeExpression<>(env.getJsonProvider(), compileNonNull(env, context, neg.value()), env.getJqVersion());
		}

		@Override
		public Expression<StackFrame, N> visit(ConditionalAstNode cond) throws JsonQueryException {
			List<Pair<Expression<StackFrame, N>, Expression<StackFrame, N>>> newSwitches = new ArrayList<>();
			for (Pair<AstNode, AstNode> sw : cond.switches()) {
				Expression<StackFrame, N> newIf = compileNonNull(env, context, sw._1);
				Expression<StackFrame, N> newThen = compileNonNull(env, context, sw._2);
				newSwitches.add(Pair.of(newIf, newThen));
			}
			Expression<StackFrame, N> newElse = compileNonNull(env, context, cond.otherwise());
			return new Conditional<>(env.getJsonProvider(), newSwitches, newElse);
		}

		@Override
		public Expression<StackFrame, N> visit(TryCatchAstNode tc) throws JsonQueryException {
			Expression<StackFrame, N> newTry = compileNonNull(env, context, tc.tryExpr());
			// catchExpr sees the caught error message, not `.` -- its `.` is input-independent iff tryExpr's is.
			boolean savedInputFixed = context.isInputFixed();
			context.setInputFixed(!newTry.dependsOnInput());
			Expression<StackFrame, N> newCatch;
			try {
				newCatch = compile(env, context, tc.catchExpr());
			} finally {
				context.setInputFixed(savedInputFixed);
			}
			return new TryCatch<>(env.getJsonProvider(), newTry, newCatch, env.getJqVersion());
		}

		@Override
		public Expression<StackFrame, N> visit(TryCatchAstNode.Question question) throws JsonQueryException {
			Expression<StackFrame, N> expression = compileNonNull(env, context, question.tryExpr());
			return new TryCatch<>(env.getJsonProvider(), expression, env.getJqVersion());
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
				compiledMatcher = compiledMatcher.resolveSlots(slots);
				// reduceExpr sees the accumulator, not `.` -- its `.` is input-independent iff iterExpr and initExpr's both are.
				boolean savedInputFixed = context.isInputFixed();
				context.setInputFixed(!compiledIter.dependsOnInput() && !compiledInit.dependsOnInput());
				Expression<StackFrame, N> compiledReduce;
				try {
					compiledReduce = compileNonNull(env, context, red.reduceExpr());
				} finally {
					context.setInputFixed(savedInputFixed);
				}
				return new ReduceExpression<>(env.getJsonProvider(), compiledMatcher, compiledInit, compiledReduce, compiledIter, new HashSet<>(slots.values()));
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
				compiledMatcher = compiledMatcher.resolveSlots(slots);
				// updateExpr sees the accumulator, not `.` -- its `.` is input-independent iff iterExpr and initExpr's both are.
				boolean savedInputFixed = context.isInputFixed();
				context.setInputFixed(!compiledIter.dependsOnInput() && !compiledInit.dependsOnInput());
				Expression<StackFrame, N> compiledUpdate;
				try {
					compiledUpdate = compileNonNull(env, context, fe.updateExpr());
				} finally {
					context.setInputFixed(savedInputFixed);
				}
				// extractExpr sees updateExpr's own output, not the iter/init-fixedness above.
				@Var Expression<StackFrame, N> compiledExtract = null;
				if (fe.extractExpr() != null) {
					context.setInputFixed(!compiledUpdate.dependsOnInput());
					try {
						compiledExtract = compile(env, context, fe.extractExpr());
					} finally {
						context.setInputFixed(savedInputFixed);
					}
				}
				return new ForeachExpression<>(compiledMatcher, compiledInit, compiledUpdate, compiledExtract, compiledIter, new HashSet<>(slots.values()));
			} finally {
				context.popScope();
			}
		}

		@Override
		public Expression<StackFrame, N> visit(FormattingFilterAstNode ff) throws JsonQueryException {
			String fname = ff.name().startsWith("@") ? ff.name() : "@" + ff.name();
			return compileFunctionCall(env, context, fname, Collections.emptyList());
		}

		@Override
		public Expression<StackFrame, N> visit(StringInterpolationAstNode si) throws JsonQueryException {
			List<Pair<Integer, Expression<StackFrame, N>>> compiledInterpolations = new ArrayList<>();
			@Var boolean anyInterpolationDependsOnInput = false;
			for (Pair<Integer, AstNode> pair : si.interpolations()) {
				Expression<StackFrame, N> resExpr = compileNonNull(env, context, pair._2);
				compiledInterpolations.add(Pair.of(pair._1, resExpr));
				anyInterpolationDependsOnInput = anyInterpolationDependsOnInput || resExpr.dependsOnInput();
			}
			// formatter sees each interpolated value, not `.` -- its `.` is input-independent iff every
			// interpolation expression's is.
			boolean savedInputFixed = context.isInputFixed();
			context.setInputFixed(!anyInterpolationDependsOnInput);
			Expression<StackFrame, N> compiledFormatter;
			try {
				compiledFormatter = compile(env, context, si.formatter());
			} finally {
				context.setInputFixed(savedInputFixed);
			}
			return new StringInterpolation<>(env.getJsonProvider(), si.template(), compiledInterpolations, compiledFormatter, env.getJqVersion());
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
				return new BracketFieldAccess<>(env.getJsonProvider(), target, start, end, bfa.permissive(), env.getJqVersion());
			} else {
				return new BracketFieldAccess<>(env.getJsonProvider(), target, start, bfa.permissive(), env.getJqVersion());
			}
		}

		@Override
		public Expression<StackFrame, N> visit(IdentifierFieldAccessAstNode ifa) throws JsonQueryException {
			Expression<StackFrame, N> target = compileNonNull(env, context, ifa.target());
			return new IdentifierFieldAccess<>(env.getJsonProvider(), target, ifa.field(), ifa.permissive(), env.getJqVersion());
		}

		@Override
		public Expression<StackFrame, N> visit(StringFieldAccessAstNode sfa) throws JsonQueryException {
			Expression<StackFrame, N> target = compileNonNull(env, context, sfa.target());
			Expression<StackFrame, N> key = compileNonNull(env, context, sfa.key());
			return new StringFieldAccess<>(env.getJsonProvider(), target, key, sfa.permissive(), env.getJqVersion());
		}

		@Override
		public Expression<StackFrame, N> visit(BracketExtractFieldAccessAstNode befa) throws JsonQueryException {
			Expression<StackFrame, N> target = compileNonNull(env, context, befa.target());
			return new BracketExtractFieldAccess<>(env.getJsonProvider(), target, befa.permissive(), env.getJqVersion());
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
			return new ThisObject<>(!context.isInputFixed());
		}

		@Override
		public Expression<StackFrame, N> visit(RecursionOperatorAstNode ast) throws JsonQueryException {
			return new RecursionOperator<>(env.getJsonProvider(), !context.isInputFixed(), env.getJqVersion().compareTo(Versions.JQ_1_6) >= 0);
		}

		@Override
		public Expression<StackFrame, N> visit(BreakExpressionAstNode ast) throws JsonQueryException {
			return new BreakExpression<>(ast.name());
		}

		@Override
		public Expression<StackFrame, N> visit(FunctionDefinitionAstNode fd) throws JsonQueryException {
			boolean isTopLevelDefinition = context.isRootScope();
			context.addLocalFunction(fd.fname(), fd.args().size());

			List<Integer> paramSlots = new ArrayList<>();
			int fnSize;
			int ownClosureSlot;
			Expression<StackFrame, N> compiledBody;
			ClosureSpec closureSpec;
			context.pushFunctionScope();
			try {
				for (String arg : fd.args()) {
					if (arg.startsWith("$")) {
						context.addLocalVariable(arg.substring(1));
						paramSlots.add(context.getVariableSlot(arg.substring(1)));
					} else {
						context.addLocalFunction(arg, 0);
						paramSlots.add(context.getFunctionSlot(arg, 0));
					}
				}
				ownClosureSlot = context.reserveClosureSlot();
				compiledBody = compileNonNull(env, context, fd.body());
				fnSize = context.getSlotCount();
				closureSpec = context.getClosureSpec();
			} finally {
				context.popScope();
			}
			int definerClosureSlot = context.getCurrentFunctionClosureSlot();

			SymbolLocation loc = context.getFunctionLocation(fd.fname(), fd.args().size());
			int slot = loc != null ? loc.slot : 0;
			if (context.exportsTopLevelFunctions() && isTopLevelDefinition) {
				context.recordRootFunctionSlot(FunctionSignature.of(fd.fname(), fd.args().size()), slot);
			}
			ResolvedFunctionDefinition<N> resolvedDef = new ResolvedFunctionDefinition<>(slot, closureSpec, fnSize, fd.args(), paramSlots, compiledBody, ownClosureSlot, definerClosureSlot);
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
			context.recordFunctionDependsOnInfo(fd.fname(), fd.args().size(),
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
			ObjectMatcher.FieldMatcher<N> compiled = new ObjectMatcher.FieldMatcher<>(field.dollar(), field.dollar() ? field.name() : null, name, subResult != null ? subResult.matcher : null);
			return new CompiledFieldMatcher<>(compiled, variableNames);
		}

		@Override
		public CompiledFieldMatcher<N> visit(ObjectMatcherAstNode.ExpressionKeyFieldMatcher field) throws JsonQueryException {
			Expression<StackFrame, N> name = compileNonNull(env, context, field.name());
			CompiledMatcher<N> matcherResult = compileMatcher(field.matcher());
			ObjectMatcher.FieldMatcher<N> compiled = new ObjectMatcher.FieldMatcher<>(false, null, name, matcherResult.matcher);
			return new CompiledFieldMatcher<>(compiled, matcherResult.variableNames);
		}
	}

	/**
	 * Looks up a function by name/arity in a single map: exact-arity match first, then variadic-arity
	 * fallback (a registration accepting any arity).
	 */
	private static @Nullable Function lookupFunction(Map<FunctionSignature, Function> functions, String fname, int nargs) {
		FunctionSignature key = FunctionSignature.of(fname, nargs);
		Function factory = functions.get(key);
		if (factory != null)
			return factory;
		return functions.get(key.asVariadic());
	}

	/**
	 * Exact-then-variadic lookup of {@code name}/{@code arity} against {@code env.getDeclaredFunctions()},
	 * mirroring {@link #lookupFunction}'s exact-then-variadic pattern for the defined/builtin registries.
	 */
	private static @Nullable FunctionSignature resolveDeclaredFunctionKey(Environment<?> env, String name, int arity) {
		FunctionSignature exact = FunctionSignature.of(name, arity);
		if (env.getDeclaredFunctions().contains(exact))
			return exact;
		FunctionSignature variadic = exact.asVariadic();
		return env.getDeclaredFunctions().contains(variadic) ? variadic : null;
	}

	/**
	 * Compiles a (non-module-qualified) function call/formatting-filter reference: local/captured {@code def}
	 * first, then a declared (no compile-time implementation, resolved via a {@link Memory} global slot
	 * at runtime) global, then a defined/builtin (fixed at compile time) global. Declared is checked before
	 * environment-defined, jq-library, or Java builtin registries so that a declared signature coinciding
	 * with a real builtin still requires a binding rather than silently falling back to the builtin.
	 */
	private static <N> Expression<StackFrame, N> compileFunctionCall(Environment<N> env, CompileContext context, String fullName, List<Expression<StackFrame, N>> compiledArgs) throws JsonQueryException {
		int arity = compiledArgs.size();
		if (context.isLocalFunction(fullName, arity)) {
			SymbolLocation loc = context.getFunctionLocation(fullName, arity);
			int slot = loc != null ? loc.slot : 0;
			FunctionDependsOnInfo info = loc != null ? loc.dependsOnInfo : null;
			BoundArgumentInfo boundArgumentInfo = loc != null ? loc.boundArgumentInfo : null;
			Expression<StackFrame, N> precomputed = boundArgumentInfo != null && compiledArgs.isEmpty() ? precomputedBoundArgument(boundArgumentInfo) : null;
			if (precomputed != null)
				return precomputed;
			if (loc != null && !loc.isLocal) {
				return boundArgumentInfo != null
						? new ResolvedCapturedFunctionBoundArgumentAccess<>(env.getJsonProvider(), env.getJqVersion(), fullName, slot, context.getCurrentFunctionClosureSlot(), compiledArgs, boundArgumentInfo, context.isInputFixed())
						: new ResolvedCapturedFunctionAccess<>(env.getJsonProvider(), env.getJqVersion(), fullName, slot, context.getCurrentFunctionClosureSlot(), compiledArgs, info, context.isInputFixed());
			}
			return boundArgumentInfo != null
					? new ResolvedLocalFunctionBoundArgumentAccess<>(env.getJsonProvider(), env.getJqVersion(), fullName, slot, compiledArgs, boundArgumentInfo, context.isInputFixed())
					: new ResolvedLocalFunctionAccess<>(env.getJsonProvider(), env.getJqVersion(), fullName, slot, compiledArgs, info, context.isInputFixed());
		}

		FunctionSignature declaredKey = resolveDeclaredFunctionKey(env, fullName, arity);
		if (declaredKey != null) {
			int globalIndex = context.getOrAssignGlobalFunctionIndex(declaredKey);
			return new ResolvedGlobalFunctionAccess<>(env.getJsonProvider(), env.getJqVersion(), fullName, globalIndex, compiledArgs);
		}

		FunctionSignature exact = FunctionSignature.of(fullName, arity);
		@Var Function factory = env.getFunctions().get(exact);
		if (factory == null) {
			JqFunction jqFunction = env.getJqFunctions().get(exact);
			if (jqFunction != null)
				return JqFunctionCompiler.compile(env, context, exact, jqFunction, JqFunctionCompiler.Origin.ENVIRONMENT, compiledArgs);
			factory = env.getFunctions().get(exact.asVariadic());
		}
		if (factory == null) {
			JqFunction jqFunction = env.getFunctionLoader().getJqFunctions(env.getJqVersion()).get(exact);
			if (jqFunction != null)
				return JqFunctionCompiler.compile(env, context, exact, jqFunction, JqFunctionCompiler.Origin.LOADER, compiledArgs);
			Map<FunctionSignature, Function> loadedFunctions = env.getFunctionLoader().getFunctions(env.getJqVersion());
			factory = loadedFunctions.get(exact);
			if (factory == null)
				factory = loadedFunctions.get(exact.asVariadic());
		}
		if (factory == null)
			throw new JsonQueryException(String.format("Function %s/%d does not exist", fullName, arity));
		Expression<StackFrame, N> fn = factory.bindArguments(env.getJsonProvider(), compiledArgs, env.getJqVersion());
		return new ResolvedFunctionCall<>(fn, fn.dependsOnExternalState(), fn.dependsOnInput(), context.isInputFixed(), compiledArgs);
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

	public static <N> void bindAndApply(StackFrame callerFrame, StackFrame currentFrame, List<String> paramNames, List<Integer> paramSlots, List<Expression<StackFrame, N>> fnArgs, N in, Path<N> path, Output<N> output, Consumer<StackFrame> bodyTask) throws JsonQueryException {
		for (int i = 0; i < paramNames.size(); i++) {
			String pName = paramNames.get(i);
			int slot = paramSlots.get(i);
			Expression<StackFrame, N> pExpr = fnArgs.get(i);
			if (!pName.startsWith("$")) {
				currentFrame.set(slot, new Function() {
					@Override
					@SuppressWarnings("unchecked")
					public <Context, N1> Expression<Context, N1> bindArguments(JsonProvider<N1> jp, List<Expression<Context, N1>> emptyArgs, Version v) {
						Expression<StackFrame, N1> effectiveExpr = (Expression<StackFrame, N1>) (Expression<?, ?>) pExpr;
						return (sFrame, inVal, pVal, outVal) -> effectiveExpr.apply(callerFrame, inVal, pVal, outVal);
					}
				});
			}
		}
		bindValueParams(callerFrame, currentFrame, paramNames, paramSlots, fnArgs, 0, in, path, output, bodyTask);
	}

	private static <N> void bindValueParams(StackFrame callerFrame, StackFrame currentFrame, List<String> paramNames, List<Integer> paramSlots, List<Expression<StackFrame, N>> fnArgs, int index, N in, Path<N> path, Output<N> output, Consumer<StackFrame> bodyTask) throws JsonQueryException {
		if (index >= paramNames.size()) {
			bodyTask.accept(currentFrame);
			return;
		}
		String argName = paramNames.get(index);
		Expression<StackFrame, N> argExpr = fnArgs.get(index);
		int slot = paramSlots.get(index);
		if (argName.startsWith("$")) {
			argExpr.apply(callerFrame, in, path, (val, p) -> {
				currentFrame.set(slot, StackFrameValues.toSlot(val));
				bindValueParams(callerFrame, currentFrame, paramNames, paramSlots, fnArgs, index + 1, in, path, output, bodyTask);
			});
		} else {
			bindValueParams(callerFrame, currentFrame, paramNames, paramSlots, fnArgs, index + 1, in, path, output, bodyTask);
		}
	}

	public static <JsonNode> Expression<StackFrame, JsonNode> compileNonNull(Environment<JsonNode> env, CompileContext context, AstNode ast) throws JsonQueryException {
		return compileNonNull(env, context, (Module) null, ast);
	}

	public static <JsonNode> Expression<StackFrame, JsonNode> compileNonNull(Environment<JsonNode> env, CompileContext context, @Nullable Module currentModule, AstNode ast) throws JsonQueryException {
		Expression<StackFrame, JsonNode> compiled = compile(env, context, currentModule, ast);
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
			Version version,
			JsonProvider<JsonNode> jsonProvider,
			boolean inputFixed) {
		switch (operator) {
			case ASSIGN:
				return new Assignment<>(jsonProvider, lhs, rhs, version, inputFixed);
			case UPDATE:
				return new UpdateAssignment<>(jsonProvider, lhs, rhs, version, inputFixed);
			case DEFAULT_EQUAL:
				return new ComplexAlternativeAssignment<>(jsonProvider, lhs, rhs, version, inputFixed);
			case PLUS_EQUAL:
				return new ComplexPlusAssignment<>(jsonProvider, lhs, rhs, version, inputFixed);
			case MINUS_EQUAL:
				return new ComplexMinusAssignment<>(jsonProvider, lhs, rhs, version, inputFixed);
			case TIMES_EQUAL:
				return new ComplexMultiplyAssignment<>(jsonProvider, lhs, rhs, version, inputFixed);
			case DIVIDE_EQUAL:
				return new ComplexDivideAssignment<>(jsonProvider, lhs, rhs, version, inputFixed);
			case MODULO_EQUAL:
				return new ComplexModuloAssignment<>(jsonProvider, lhs, rhs, version, inputFixed);
			case DEFAULT:
				return new AlternativeOperatorExpression<>(jsonProvider, lhs, rhs);
			case OR:
				return new BooleanOrExpression<>(jsonProvider, lhs, rhs);
			case AND:
				return new BooleanAndExpression<>(jsonProvider, lhs, rhs);
			case LESS_EQUAL:
				return new CompareLessEqualTest<>(jsonProvider, lhs, rhs);
			case LESS:
				return new CompareLessTest<>(jsonProvider, lhs, rhs);
			case GREATER_EQUAL:
				return new CompareGreaterEqualTest<>(jsonProvider, lhs, rhs);
			case GREATER:
				return new CompareGreaterTest<>(jsonProvider, lhs, rhs);
			case EQUAL:
				return new CompareEqualTest<>(jsonProvider, lhs, rhs);
			case NOT_EQUAL:
				return new CompareNotEqualTest<>(jsonProvider, lhs, rhs);
			case PLUS:
				return new PlusExpression<>(jsonProvider, lhs, rhs, version);
			case MINUS:
				return new MinusExpression<>(jsonProvider, lhs, rhs, version);
			case MODULO:
				return new ModuloExpression<>(jsonProvider, lhs, rhs, version);
			case DIVIDE:
				return new DivideExpression<>(jsonProvider, lhs, rhs, version);
			case TIMES:
				return new MultiplyExpression<>(jsonProvider, lhs, rhs, version);
			default:
				throw new IllegalArgumentException("Unknown operator: " + operator);
		}
	}
}
