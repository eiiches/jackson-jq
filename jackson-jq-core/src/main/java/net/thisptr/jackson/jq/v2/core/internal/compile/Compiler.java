package net.thisptr.jackson.jq.v2.core.internal.compile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.ArrayConstructionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.BinaryOpAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.BreakExpressionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.ConditionalAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.ForeachExpressionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.FormattingFilterAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.FunctionCallAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.FunctionDefinitionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.NegativeExpressionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.ObjectConstructionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.ParenAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.PipedQueryAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.RecursionOperatorAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.ReduceExpressionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.SemicolonOperatorAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.StringInterpolationAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.ThisObjectAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.TopLevelAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.TryCatchAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.TupleAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.VariableAccessAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.fieldaccess.BracketExtractFieldAccessAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.fieldaccess.BracketFieldAccessAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.fieldaccess.IdentifierFieldAccessAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.fieldaccess.StringFieldAccessAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.literal.BooleanLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.literal.NullLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.literal.NumericLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.literal.StringLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.matcher.PatternMatcherAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.matcher.matchers.ArrayMatcherAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.matcher.matchers.ObjectMatcherAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.matcher.matchers.ValueMatcherAstNode;
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
import net.thisptr.jackson.jq.v2.core.internal.memory.Memory;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.tree.ArrayConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.AssignPipeComponent;
import net.thisptr.jackson.jq.v2.core.internal.tree.BreakExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.Conditional;
import net.thisptr.jackson.jq.v2.core.internal.tree.FixedInputExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.ForeachExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.IdentifierKeyFieldConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.JsonQueryKeyFieldConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.LabelPipeComponent;
import net.thisptr.jackson.jq.v2.core.internal.tree.NegativeExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.ObjectConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.PipeComponent;
import net.thisptr.jackson.jq.v2.core.internal.tree.PipedQuery;
import net.thisptr.jackson.jq.v2.core.internal.tree.PrecomputedConstantExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.RecursionOperator;
import net.thisptr.jackson.jq.v2.core.internal.tree.ReduceExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.SemicolonOperator;
import net.thisptr.jackson.jq.v2.core.internal.tree.StringInterpolation;
import net.thisptr.jackson.jq.v2.core.internal.tree.StringKeyFieldConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.ThisObject;
import net.thisptr.jackson.jq.v2.core.internal.tree.TopLevelExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.TransformPipeComponent;
import net.thisptr.jackson.jq.v2.core.internal.tree.TryCatch;
import net.thisptr.jackson.jq.v2.core.internal.tree.Tuple;
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
import net.thisptr.jackson.jq.v2.json.JsonProvider;
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
		return compile(env, (Module) null, ast);
	}

	public static <JsonNode> Expression<StackFrame, JsonNode> compile(Environment<JsonNode> env, @Nullable Module currentModule, AstNode ast) throws JsonQueryException {
		return compileRoot(env, currentModule, ast, false);
	}

	/**
	 * Compiles a module's own defining source. Unlike {@link #compile(Environment, Module, AstNode)}, this
	 * tracks the module's genuinely top-level {@code def}s (see {@link CompileContext#exportsTopLevelFunctions()}
	 * / {@link CompileContext#isRootScope()}) so {@link RootExpression#applyForModuleExports} can harvest their
	 * real, correctly closure-bound {@link Function} values after running the module body once. This is how
	 * {@code FileSystemModuleLoader.loadModuleActual} populates a file-based module's exported functions.
	 * Ordinary query compilation must never do this -- use {@link #compile(Environment, Module, AstNode)}.
	 */
	public static <JsonNode> Expression<StackFrame, JsonNode> compileModule(Environment<JsonNode> env, @Nullable Module currentModule, AstNode ast) throws JsonQueryException {
		return compileRoot(env, currentModule, ast, true);
	}

	private static <JsonNode> Expression<StackFrame, JsonNode> compileRoot(Environment<JsonNode> env, @Nullable Module currentModule, AstNode ast, boolean exportTopLevelFunctions) throws JsonQueryException {
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

		if (ast instanceof ParenAstNode) {
			ParenAstNode paren = (ParenAstNode) ast;
			return compile(env, context, currentModule, paren.value());
		}

		if (ast instanceof FunctionCallAstNode) {
			FunctionCallAstNode call = (FunctionCallAstNode) ast;
			boolean inputFixed = context.isInputFixed();
			@Var List<Expression<StackFrame, JsonNode>> compiledArgs = new ArrayList<>();
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
				Expression<StackFrame, JsonNode> fn = factory.bindArguments(env.getJsonProvider(), compiledArgs, env.getJqVersion());
				Expression<StackFrame, JsonNode> result = new ResolvedFunctionCall<>(fn, fn.dependsOnExternalState(), fn.dependsOnInput(), inputFixed, compiledArgs);
				return restoreFixedInput(result, inputFixed);
			}

			return restoreFixedInput(compileFunctionCall(env, context, call.name(), compiledArgs), inputFixed);
		}

		if (ast instanceof VariableAccessAstNode) {
			VariableAccessAstNode varAccess = (VariableAccessAstNode) ast;
			return compileVariableRef(env, context, varAccess.moduleName(), varAccess.name());
		}

		if (ast instanceof TopLevelAstNode) {
			TopLevelAstNode top = (TopLevelAstNode) ast;
			for (TopLevelAstNode.ImportStatement imp : top.imports()) {
				JsonNode metadata = evaluateMetadata(env.getJsonProvider(), imp);
				if (imp.dollarImport) {
					JsonNode data = env.getModuleLoader().loadData(currentModule, imp.path, metadata);
					if (data == null) {
						throw new JsonQueryException(String.format("module not found: %s", imp.path));
					}
					if (imp.name != null) {
						context.addImportedVariableDefault(imp.name, data);
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
			Expression<StackFrame, JsonNode> compiledInner = compileNonNull(env, context, currentModule, top.expr());
			return new TopLevelExpression<>(compiledInner);
		}

		if (ast instanceof PipedQueryAstNode) {
			PipedQueryAstNode piped = (PipedQueryAstNode) ast;
			List<PipeComponent<JsonNode>> newComponents = new ArrayList<>();

			@Var int pushedScopes = 0;
			boolean savedInputFixed = context.isInputFixed();
			@Var boolean fixed = savedInputFixed;
			try {
				for (PipedQueryAstNode.PipeComponent comp : piped.components()) {
					if (comp instanceof PipedQueryAstNode.AssignPipeComponent) {
						PipedQueryAstNode.AssignPipeComponent assign = (PipedQueryAstNode.AssignPipeComponent) comp;
						context.setInputFixed(fixed);
						Expression<StackFrame, JsonNode> compiledExpr = compileNonNull(env, context, assign.expr);
						@Var PatternMatcher<JsonNode> compiledMatcher = compileMatcher(env, context, assign.matcher);

						context.pushLocalScope();
						pushedScopes++;

						Set<String> varNames = new HashSet<>();
						collectVariableNames(assign.matcher, varNames);
						Map<String, Integer> slots = new HashMap<>();
						for (String varName : varNames) {
							context.addLocalVariable(varName);
							slots.put(varName, context.getVariableSlot(varName));
						}
						compiledMatcher = compiledMatcher.resolveSlots(slots);

						newComponents.add(new AssignPipeComponent<>(compiledExpr, compiledMatcher, new HashSet<>(slots.values())));
						// `.` doesn't change across an `as` binding -- `fixed` passes through unchanged.
						// (Whether the bound variable itself is "free" is handled separately, by
						// PipedQuery's free-variable analysis closing over the bound slots.)
					} else if (comp instanceof PipedQueryAstNode.TransformPipeComponent) {
						PipedQueryAstNode.TransformPipeComponent transform = (PipedQueryAstNode.TransformPipeComponent) comp;
						context.setInputFixed(fixed);
						Expression<StackFrame, JsonNode> compiledExpr = compileNonNull(env, context, transform.expr);
						newComponents.add(new TransformPipeComponent<>(compiledExpr));
						// This stage's output, now known, is the next stage's input.
						fixed = !compiledExpr.dependsOnInput();
					} else if (comp instanceof PipedQueryAstNode.LabelPipeComponent) {
						PipedQueryAstNode.LabelPipeComponent label = (PipedQueryAstNode.LabelPipeComponent) comp;
						newComponents.add(new LabelPipeComponent<>(label.name));
						// `label $out | ...` doesn't rebind `.` -- `fixed` passes through unchanged.
					} else {
						throw new IllegalStateException("Unknown pipe component: " + comp.getClass());
					}
				}
			} finally {
				context.setInputFixed(savedInputFixed);
				for (int i = 0; i < pushedScopes; i++) {
					context.popScope();
				}
			}

			return new PipedQuery<>(newComponents);
		}

		if (ast instanceof SemicolonOperatorAstNode) {
			SemicolonOperatorAstNode semi = (SemicolonOperatorAstNode) ast;
			List<Expression<StackFrame, JsonNode>> newExpressions = new ArrayList<>();
			for (AstNode q : semi.expressions()) {
				newExpressions.add(compileNonNull(env, context, q));
			}
			return new SemicolonOperator<>(newExpressions);
		}

		if (ast instanceof ObjectConstructionAstNode) {
			ObjectConstructionAstNode obj = (ObjectConstructionAstNode) ast;
			ObjectConstruction<JsonNode> res = new ObjectConstruction<>(env.getJsonProvider());
			for (ObjectConstructionAstNode.FieldConstructionAst fc : obj.fields) {
				if (fc instanceof ObjectConstructionAstNode.IdentifierKeyFieldConstructionAst) {
					ObjectConstructionAstNode.IdentifierKeyFieldConstructionAst ik = (ObjectConstructionAstNode.IdentifierKeyFieldConstructionAst) fc;
					Expression<StackFrame, JsonNode> val = compile(env, context, ik.value);
					res.add(new IdentifierKeyFieldConstruction<>(env.getJsonProvider(), ik.key, val, env.getJqVersion()));
				} else if (fc instanceof ObjectConstructionAstNode.JsonQueryKeyFieldConstructionAst) {
					ObjectConstructionAstNode.JsonQueryKeyFieldConstructionAst jq = (ObjectConstructionAstNode.JsonQueryKeyFieldConstructionAst) fc;
					Expression<StackFrame, JsonNode> key = compileNonNull(env, context, jq.key());
					Expression<StackFrame, JsonNode> val = compileNonNull(env, context, jq.value());
					res.add(new JsonQueryKeyFieldConstruction<>(env.getJsonProvider(), key, val, env.getJqVersion()));
				} else if (fc instanceof ObjectConstructionAstNode.StringKeyFieldConstructionAst) {
					ObjectConstructionAstNode.StringKeyFieldConstructionAst sk = (ObjectConstructionAstNode.StringKeyFieldConstructionAst) fc;
					Expression<StackFrame, JsonNode> key = compileNonNull(env, context, sk.key);
					Expression<StackFrame, JsonNode> val = compile(env, context, sk.value);
					res.add(new StringKeyFieldConstruction<>(env.getJsonProvider(), key, val, env.getJqVersion()));
				} else if (fc instanceof ObjectConstructionAstNode.VariableKeyFieldConstruction) {
					// desugar `{ $x }` into the same shape as `{ x: $x }` -- no dedicated resolved class needed.
					ObjectConstructionAstNode.VariableKeyFieldConstruction vk = (ObjectConstructionAstNode.VariableKeyFieldConstruction) fc;
					Expression<StackFrame, JsonNode> compiledValue = compileVariableRef(env, context, null, vk.name());
					res.add(new IdentifierKeyFieldConstruction<>(env.getJsonProvider(), vk.name(), compiledValue, env.getJqVersion()));
				} else {
					throw new IllegalStateException("Unknown field construction: " + fc.getClass());
				}
			}
			return res;
		}

		if (ast instanceof ArrayConstructionAstNode) {
			ArrayConstructionAstNode arr = (ArrayConstructionAstNode) ast;
			return new ArrayConstruction<>(env.getJsonProvider(), compile(env, context, arr.q));
		}

		if (ast instanceof BinaryOpAstNode) {
			BinaryOpAstNode bin = (BinaryOpAstNode) ast;
			Expression<StackFrame, JsonNode> lhs = compileNonNull(env, context, bin.lhs);
			boolean savedInputFixed = context.isInputFixed();
			if (bin.operator == BinaryOperator.UPDATE) {
				// `|=`'s rhs is rebound to the value at the resolved path, not `.`.
				context.setInputFixed(savedInputFixed && !lhs.dependsOnInput());
			}
			Expression<StackFrame, JsonNode> rhs;
			try {
				rhs = compileNonNull(env, context, bin.rhs);
			} finally {
				context.setInputFixed(savedInputFixed);
			}
			return compileBinaryOperator(bin.operator, lhs, rhs, env.getJqVersion(), env.getJsonProvider(), savedInputFixed);
		}

		if (ast instanceof NegativeExpressionAstNode) {
			NegativeExpressionAstNode neg = (NegativeExpressionAstNode) ast;
			return new NegativeExpression<>(env.getJsonProvider(), compileNonNull(env, context, neg.value()), env.getJqVersion());
		}

		if (ast instanceof ConditionalAstNode) {
			ConditionalAstNode cond = (ConditionalAstNode) ast;
			List<Pair<Expression<StackFrame, JsonNode>, Expression<StackFrame, JsonNode>>> newSwitches = new ArrayList<>();
			for (Pair<AstNode, AstNode> sw : cond.switches()) {
				Expression<StackFrame, JsonNode> newIf = compileNonNull(env, context, sw._1);
				Expression<StackFrame, JsonNode> newThen = compileNonNull(env, context, sw._2);
				newSwitches.add(Pair.of(newIf, newThen));
			}
			Expression<StackFrame, JsonNode> newElse = compileNonNull(env, context, cond.otherwise());
			return new Conditional<>(env.getJsonProvider(), newSwitches, newElse);
		}

		if (ast instanceof TryCatchAstNode) {
			TryCatchAstNode tc = (TryCatchAstNode) ast;
			Expression<StackFrame, JsonNode> newTry = compileNonNull(env, context, tc.tryExpr());
			if (tc instanceof TryCatchAstNode.Question) {
				return new TryCatch<>(env.getJsonProvider(), newTry);
			}
			// catchExpr sees the caught error message, not `.` -- its `.` is input-independent iff tryExpr's is.
			boolean savedInputFixed = context.isInputFixed();
			context.setInputFixed(!newTry.dependsOnInput());
			Expression<StackFrame, JsonNode> newCatch;
			try {
				newCatch = compile(env, context, tc.catchExpr());
			} finally {
				context.setInputFixed(savedInputFixed);
			}
			return new TryCatch<>(env.getJsonProvider(), newTry, newCatch);
		}

		if (ast instanceof TupleAstNode) {
			TupleAstNode tuple = (TupleAstNode) ast;
			List<Expression<StackFrame, JsonNode>> newQs = new ArrayList<>();
			for (AstNode q : tuple.qs) {
				newQs.add(compileNonNull(env, context, q));
			}
			return new Tuple<>(newQs);
		}

		if (ast instanceof ReduceExpressionAstNode) {
			ReduceExpressionAstNode red = (ReduceExpressionAstNode) ast;
			Expression<StackFrame, JsonNode> compiledIter = compileNonNull(env, context, red.iterExpr());
			Expression<StackFrame, JsonNode> compiledInit = compileNonNull(env, context, red.initExpr());
			@Var PatternMatcher<JsonNode> compiledMatcher = compileMatcher(env, context, red.matcher());

			Set<String> varNames = new HashSet<>();
			collectVariableNames(red.matcher(), varNames);
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
				Expression<StackFrame, JsonNode> compiledReduce;
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

		if (ast instanceof ForeachExpressionAstNode) {
			ForeachExpressionAstNode fe = (ForeachExpressionAstNode) ast;
			Expression<StackFrame, JsonNode> compiledIter = compileNonNull(env, context, fe.iterExpr());
			Expression<StackFrame, JsonNode> compiledInit = compileNonNull(env, context, fe.initExpr());
			@Var PatternMatcher<JsonNode> compiledMatcher = compileMatcher(env, context, fe.matcher());

			Set<String> varNames = new HashSet<>();
			collectVariableNames(fe.matcher(), varNames);
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
				Expression<StackFrame, JsonNode> compiledUpdate;
				try {
					compiledUpdate = compileNonNull(env, context, fe.updateExpr());
				} finally {
					context.setInputFixed(savedInputFixed);
				}
				// extractExpr sees updateExpr's own output, not the iter/init-fixedness above.
				@Var Expression<StackFrame, JsonNode> compiledExtract = null;
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

		if (ast instanceof FormattingFilterAstNode) {
			FormattingFilterAstNode ff = (FormattingFilterAstNode) ast;
			String fname = ff.name().startsWith("@") ? ff.name() : "@" + ff.name();
			return compileFunctionCall(env, context, fname, Collections.emptyList());
		}

		if (ast instanceof StringInterpolationAstNode) {
			StringInterpolationAstNode si = (StringInterpolationAstNode) ast;
			List<Pair<Integer, Expression<StackFrame, JsonNode>>> compiledInterpolations = new ArrayList<>();
			@Var boolean anyInterpolationDependsOnInput = false;
			for (Pair<Integer, AstNode> pair : si.interpolations()) {
				Expression<StackFrame, JsonNode> resExpr = compileNonNull(env, context, pair._2);
				compiledInterpolations.add(Pair.of(pair._1, resExpr));
				anyInterpolationDependsOnInput = anyInterpolationDependsOnInput || resExpr.dependsOnInput();
			}
			// formatter sees each interpolated value, not `.` -- its `.` is input-independent iff every
			// interpolation expression's is.
			boolean savedInputFixed = context.isInputFixed();
			context.setInputFixed(!anyInterpolationDependsOnInput);
			Expression<StackFrame, JsonNode> compiledFormatter;
			try {
				compiledFormatter = compile(env, context, si.formatter());
			} finally {
				context.setInputFixed(savedInputFixed);
			}
			return new StringInterpolation<>(env.getJsonProvider(), si.template(), compiledInterpolations, compiledFormatter, env.getJqVersion());
		}

		if (ast instanceof BracketFieldAccessAstNode) {
			BracketFieldAccessAstNode bfa = (BracketFieldAccessAstNode) ast;
			Expression<StackFrame, JsonNode> target = compileNonNull(env, context, bfa.target());
			@Var Expression<StackFrame, JsonNode> start = compile(env, context, bfa.startExpr());
			@Var Expression<StackFrame, JsonNode> end = compile(env, context, bfa.endExpr());
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

		if (ast instanceof IdentifierFieldAccessAstNode) {
			IdentifierFieldAccessAstNode ifa = (IdentifierFieldAccessAstNode) ast;
			Expression<StackFrame, JsonNode> target = compileNonNull(env, context, ifa.target());
			return new IdentifierFieldAccess<>(env.getJsonProvider(), target, ifa.field(), ifa.permissive(), env.getJqVersion());
		}

		if (ast instanceof StringFieldAccessAstNode) {
			StringFieldAccessAstNode sfa = (StringFieldAccessAstNode) ast;
			Expression<StackFrame, JsonNode> target = compileNonNull(env, context, sfa.target());
			Expression<StackFrame, JsonNode> key = compileNonNull(env, context, sfa.key());
			return new StringFieldAccess<>(env.getJsonProvider(), target, key, sfa.permissive(), env.getJqVersion());
		}

		if (ast instanceof BracketExtractFieldAccessAstNode) {
			BracketExtractFieldAccessAstNode befa = (BracketExtractFieldAccessAstNode) ast;
			Expression<StackFrame, JsonNode> target = compileNonNull(env, context, befa.target());
			return new BracketExtractFieldAccess<>(env.getJsonProvider(), target, befa.permissive(), env.getJqVersion());
		}

		if (ast instanceof BooleanLiteralAstNode) {
			return new ValueLiteral<>(env.getJsonProvider().createBoolean(((BooleanLiteralAstNode) ast).value()));
		}

		if (ast instanceof NumericLiteralAstNode) {
			return new ValueLiteral<>(env.getJsonProvider().createNumber(new BigDecimal(((NumericLiteralAstNode) ast).text())));
		}

		if (ast instanceof NullLiteralAstNode) {
			return new ValueLiteral<>(env.getJsonProvider().createNull());
		}

		if (ast instanceof StringLiteralAstNode) {
			return new ValueLiteral<>(env.getJsonProvider().createString(((StringLiteralAstNode) ast).value()));
		}

		if (ast instanceof ThisObjectAstNode) {
			return new ThisObject<>(!context.isInputFixed());
		}

		if (ast instanceof RecursionOperatorAstNode) {
			return new RecursionOperator<>(env.getJsonProvider(), !context.isInputFixed());
		}

		if (ast instanceof BreakExpressionAstNode) {
			return new BreakExpression(((BreakExpressionAstNode) ast).name());
		}

		if (ast instanceof FunctionDefinitionAstNode) {
			FunctionDefinitionAstNode fd = (FunctionDefinitionAstNode) ast;
			boolean isTopLevelDefinition = context.isRootScope();
			context.addLocalFunction(fd.fname(), fd.args().size());

			List<Integer> paramSlots = new ArrayList<>();
			int fnSize;
			int ownClosureSlot;
			Expression<StackFrame, JsonNode> compiledBody;
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
			ResolvedFunctionDefinition<JsonNode> resolvedDef = new ResolvedFunctionDefinition<>(slot, closureSpec, fnSize, fd.args(), paramSlots, compiledBody, ownClosureSlot, definerClosureSlot);
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

		throw new IllegalStateException("Unknown AST node: " + ast.getClass());
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

	private static <N> PatternMatcher<N> compileMatcher(Environment<N> env, CompileContext context, PatternMatcherAstNode matcher) throws JsonQueryException {
		if (matcher instanceof ValueMatcherAstNode) {
			return new ValueMatcher<>(((ValueMatcherAstNode) matcher).name());
		}
		if (matcher instanceof ArrayMatcherAstNode) {
			ArrayMatcherAstNode am = (ArrayMatcherAstNode) matcher;
			List<PatternMatcher<N>> compiled = new ArrayList<>();
			for (PatternMatcherAstNode m : am.matchers()) {
				compiled.add(compileMatcher(env, context, m));
			}
			return new ArrayMatcher<>(env.getJsonProvider(), compiled, env.getJqVersion());
		}
		if (matcher instanceof ObjectMatcherAstNode) {
			ObjectMatcherAstNode om = (ObjectMatcherAstNode) matcher;
			List<ObjectMatcher.FieldMatcher<N>> compiled = new ArrayList<>();
			for (ObjectMatcherAstNode.FieldMatcher fm : om.matchers()) {
				compiled.add(compileFieldMatcher(env, context, fm));
			}
			return new ObjectMatcher<>(env.getJsonProvider(), compiled, env.getJqVersion());
		}
		throw new IllegalStateException("Unknown matcher type: " + matcher.getClass());
	}

	private static <N> ObjectMatcher.FieldMatcher<N> compileFieldMatcher(Environment<N> env, CompileContext context, ObjectMatcherAstNode.FieldMatcher fm) throws JsonQueryException {
		if (fm instanceof ObjectMatcherAstNode.ConstantKeyFieldMatcher) {
			ObjectMatcherAstNode.ConstantKeyFieldMatcher ckfm = (ObjectMatcherAstNode.ConstantKeyFieldMatcher) fm;
			Expression<StackFrame, N> name = new ValueLiteral<>(env.getJsonProvider().createString(ckfm.name()));
			PatternMatcherAstNode sub = ckfm.matcher();
			return new ObjectMatcher.FieldMatcher<>(ckfm.dollar(), ckfm.dollar() ? ckfm.name() : null, name, sub != null ? compileMatcher(env, context, sub) : null);
		}
		if (fm instanceof ObjectMatcherAstNode.ExpressionKeyFieldMatcher) {
			ObjectMatcherAstNode.ExpressionKeyFieldMatcher ekfm = (ObjectMatcherAstNode.ExpressionKeyFieldMatcher) fm;
			Expression<StackFrame, N> name = compileNonNull(env, context, ekfm.name());
			return new ObjectMatcher.FieldMatcher<>(false, null, name, compileMatcher(env, context, ekfm.matcher()));
		}
		throw new IllegalStateException("Unknown field matcher type: " + fm.getClass());
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
				currentFrame.set(slot, val);
				bindValueParams(callerFrame, currentFrame, paramNames, paramSlots, fnArgs, index + 1, in, path, output, bodyTask);
			});
		} else {
			bindValueParams(callerFrame, currentFrame, paramNames, paramSlots, fnArgs, index + 1, in, path, output, bodyTask);
		}
	}

	private static void collectVariableNames(PatternMatcherAstNode matcher, Set<String> out) {
		if (matcher instanceof ValueMatcherAstNode) {
			out.add(((ValueMatcherAstNode) matcher).name());
		} else if (matcher instanceof ArrayMatcherAstNode) {
			for (PatternMatcherAstNode m : ((ArrayMatcherAstNode) matcher).matchers()) {
				collectVariableNames(m, out);
			}
		} else if (matcher instanceof ObjectMatcherAstNode) {
			for (ObjectMatcherAstNode.FieldMatcher fm : ((ObjectMatcherAstNode) matcher).matchers()) {
				if (fm instanceof ObjectMatcherAstNode.ConstantKeyFieldMatcher) {
					ObjectMatcherAstNode.ConstantKeyFieldMatcher ckfm = (ObjectMatcherAstNode.ConstantKeyFieldMatcher) fm;
					if (ckfm.dollar())
						out.add(ckfm.name());
				}
				PatternMatcherAstNode sub = fm.matcher();
				if (sub != null) {
					collectVariableNames(sub, out);
				}
			}
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

	public static <JsonNode> @Nullable JsonNode evaluateMetadata(JsonProvider<JsonNode> jsonProvider, @Nullable AstNode metadataExpr) {
		if (metadataExpr == null)
			return null;
		JsonNode metadata = ExpressionUtils.evaluateLiteralExpression(jsonProvider, metadataExpr);
		if (metadata == null)
			throw new IllegalArgumentException("Module metadata must be constant");
		if (!jsonProvider.isObject(metadata))
			throw new IllegalArgumentException("Module metadata must be an object");
		return metadata;
	}

	public static <JsonNode> @Nullable JsonNode evaluateMetadata(JsonProvider<JsonNode> jsonProvider, TopLevelAstNode.ImportStatement statement) {
		return evaluateMetadata(jsonProvider, statement.metadataExpr());
	}

	public static <JsonNode> JsonNode evaluateMetadata(JsonProvider<JsonNode> jsonProvider, TopLevelAstNode.ModuleDirective directive) {
		return Objects.requireNonNull(evaluateMetadata(jsonProvider, directive.metadataExpr()));
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
