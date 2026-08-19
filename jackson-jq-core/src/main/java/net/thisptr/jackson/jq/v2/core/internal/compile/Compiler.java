package net.thisptr.jackson.jq.v2.core.internal.compile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.internal.ast.ArrayConstructionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.BinaryOpAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.BreakExpressionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ConditionalAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ForeachExpressionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.FormattingFilterAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.FunctionCallAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.FunctionDefinitionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.NegativeExpressionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ObjectConstructionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.PipedQueryAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.RecursionOperatorAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ReduceExpressionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.SemicolonOperatorAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.StringInterpolationAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ThisObjectAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.TopLevelAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.TryCatchAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.TupleAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.VariableAccessAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.fieldaccess.BracketExtractFieldAccessAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.fieldaccess.BracketFieldAccessAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.fieldaccess.IdentifierFieldAccessAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.fieldaccess.StringFieldAccessAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.literal.BooleanLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.literal.DoubleLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.literal.LongLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.literal.NullLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.literal.StringLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.matcher.PatternMatcherAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.matcher.matchers.ArrayMatcherAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.matcher.matchers.ObjectMatcherAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.matcher.matchers.ValueMatcherAstNode;
import net.thisptr.jackson.jq.v2.core.internal.misc.Pair;
import net.thisptr.jackson.jq.v2.core.internal.tree.ArrayConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.AssignPipeComponent;
import net.thisptr.jackson.jq.v2.core.internal.tree.BreakExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.Conditional;
import net.thisptr.jackson.jq.v2.core.internal.tree.ForeachExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.IdentifierKeyFieldConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.JsonQueryKeyFieldConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.LabelPipeComponent;
import net.thisptr.jackson.jq.v2.core.internal.tree.NegativeExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.ObjectConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.PipeComponent;
import net.thisptr.jackson.jq.v2.core.internal.tree.PipedQuery;
import net.thisptr.jackson.jq.v2.core.internal.tree.RecursionOperator;
import net.thisptr.jackson.jq.v2.core.internal.tree.ReduceExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.ResolvedCapturedFunctionAccess;
import net.thisptr.jackson.jq.v2.core.internal.tree.ResolvedCapturedVariableAccess;
import net.thisptr.jackson.jq.v2.core.internal.tree.ResolvedFunctionCall;
import net.thisptr.jackson.jq.v2.core.internal.tree.ResolvedFunctionDefinition;
import net.thisptr.jackson.jq.v2.core.internal.tree.ResolvedGlobalVariableAccess;
import net.thisptr.jackson.jq.v2.core.internal.tree.ResolvedLocalFunctionAccess;
import net.thisptr.jackson.jq.v2.core.internal.tree.ResolvedLocalVariableAccess;
import net.thisptr.jackson.jq.v2.core.internal.tree.RootExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.SemicolonOperator;
import net.thisptr.jackson.jq.v2.core.internal.tree.StringInterpolation;
import net.thisptr.jackson.jq.v2.core.internal.tree.StringKeyFieldConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.ThisObject;
import net.thisptr.jackson.jq.v2.core.internal.tree.TopLevelExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.TransformPipeComponent;
import net.thisptr.jackson.jq.v2.core.internal.tree.TryCatch;
import net.thisptr.jackson.jq.v2.core.internal.tree.Tuple;
import net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess.BracketExtractFieldAccess;
import net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess.BracketFieldAccess;
import net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess.IdentifierFieldAccess;
import net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess.StringFieldAccess;
import net.thisptr.jackson.jq.v2.core.internal.tree.literal.BooleanLiteral;
import net.thisptr.jackson.jq.v2.core.internal.tree.literal.DoubleLiteral;
import net.thisptr.jackson.jq.v2.core.internal.tree.literal.LongLiteral;
import net.thisptr.jackson.jq.v2.core.internal.tree.literal.NullLiteral;
import net.thisptr.jackson.jq.v2.core.internal.tree.literal.StringLiteral;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.matchers.ArrayMatcher;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.matchers.ObjectMatcher;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.matchers.ValueMatcher;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionNameAndArity;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class Compiler {

	public static <JsonNode> Expression<JsonNode> compile(Environment<JsonNode> env, AstNode ast) throws JsonQueryException {
		return compile(env, (Module) null, ast);
	}

	public static <JsonNode> Expression<JsonNode> compile(Environment<JsonNode> env, @Nullable Module currentModule, AstNode ast) throws JsonQueryException {
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
	public static <JsonNode> Expression<JsonNode> compileModule(Environment<JsonNode> env, @Nullable Module currentModule, AstNode ast) throws JsonQueryException {
		return compileRoot(env, currentModule, ast, true);
	}

	private static <JsonNode> Expression<JsonNode> compileRoot(Environment<JsonNode> env, @Nullable Module currentModule, AstNode ast, boolean exportTopLevelFunctions) throws JsonQueryException {
		CompileContext context = new CompileContext(exportTopLevelFunctions);
		registerEnvironmentGlobals(env, context);
		Expression<JsonNode> compiled = compile(env, context, currentModule, ast);
		if (compiled == null)
			throw new JsonQueryException("Cannot resolve null expression");
		return new RootExpression<>(context.getSlotCount(), compiled,
				env.variables(), env.functions(), context.globalVariableSlots(), context.globalFunctionSlots(),
				context.globalVariables(), context.globalFunctions(), context.rootFunctionSlots());
	}

	public static <JsonNode> void registerEnvironmentGlobals(Environment<JsonNode> env, CompileContext context) {
		for (String name : env.variables().keySet()) {
			context.addGlobalVariable(name, name);
			context.addGlobalVariable(name + "::" + name, name);
		}
		for (FunctionNameAndArity key : env.functions().keySet())
			context.addGlobalFunction(key);
	}

	public static <JsonNode> @Nullable Expression<JsonNode> compile(Environment<JsonNode> env, CompileContext context, @Nullable AstNode ast) throws JsonQueryException {
		return compile(env, context, (Module) null, ast);
	}

	public static <JsonNode> @Nullable Expression<JsonNode> compile(Environment<JsonNode> env, CompileContext context, @Nullable Module currentModule, @Nullable AstNode ast) throws JsonQueryException {
		if (ast == null)
			return null;

		if (ast instanceof FunctionCallAstNode) {
			FunctionCallAstNode call = (FunctionCallAstNode) ast;
			List<Expression<JsonNode>> compiledArgs = new ArrayList<>();
			for (AstNode arg : call.args()) {
				compiledArgs.add(compile(env, context, currentModule, arg));
			}

			String fullName = call.moduleName() != null ? call.moduleName() + "::" + call.name() : call.name();
			if (context.isLocalFunction(fullName, compiledArgs.size())) {
				SymbolLocation loc = context.getFunctionLocation(fullName, compiledArgs.size());
				int slot = loc != null ? loc.slot : 0;
				@Var Function defaultFactory = null;
				@Var Expression<JsonNode> defaultFunction = null;
				if (loc != null && loc.isGlobal) {
					defaultFactory = env.getFunction(FunctionNameAndArity.of(fullName, compiledArgs.size()));
					if (defaultFactory != null)
						defaultFunction = defaultFactory.bindArguments(env.jsonProvider(), compiledArgs, env.version());
				}
				if (loc != null && !loc.isLocal) {
					return new ResolvedCapturedFunctionAccess<>(env.jsonProvider(), env.version(), fullName, slot, context.getCurrentFunctionClosureSlot(), compiledArgs, defaultFactory, defaultFunction);
				}
				return new ResolvedLocalFunctionAccess<>(env.jsonProvider(), env.version(), fullName, slot, compiledArgs, defaultFactory, defaultFunction);
			}

			FunctionNameAndArity key = FunctionNameAndArity.of(fullName, compiledArgs.size());
			Function factory = env.getFunction(key);
			if (factory == null) {
				throw new JsonQueryException(String.format("Function %s/%d does not exist", fullName, compiledArgs.size()));
			}

			Expression<JsonNode> fn = factory.bindArguments(env.jsonProvider(), compiledArgs, env.version());
			return new ResolvedFunctionCall<>(fullName, fn);
		}

		if (ast instanceof VariableAccessAstNode) {
			VariableAccessAstNode varAccess = (VariableAccessAstNode) ast;
			return compileVariableRef(env, context, varAccess.moduleName(), varAccess.name());
		}

		if (ast instanceof TopLevelAstNode) {
			@SuppressWarnings("unchecked")
			TopLevelAstNode<JsonNode> top = (TopLevelAstNode<JsonNode>) ast;
			for (TopLevelAstNode.ImportStatement<JsonNode> imp : top.imports()) {
				if (env.getModuleLoader() == null) {
					throw new JsonQueryException(String.format("module not found: %s", imp.path));
				}
				JsonNode metadata = imp.getMetadata(env.jsonProvider());
				if (imp.dollarImport) {
					JsonNode data = env.getModuleLoader().loadData(currentModule, imp.path, metadata);
					if (data == null) {
						throw new JsonQueryException(String.format("module not found: %s", imp.path));
					}
					if (imp.name != null) {
						env.addVariable(imp.name, data);
						context.addGlobalVariable(imp.name, imp.name);
						context.addGlobalVariable(imp.name + "::" + imp.name, imp.name);
					}
				} else {
					Module mod = env.getModuleLoader().loadModule(currentModule, imp.path, metadata);
					if (mod == null) {
						throw new JsonQueryException(String.format("module not found: %s", imp.path));
					}
					for (Map.Entry<String, Function> entry : mod.getAllFunctions().entrySet()) {
						String[] parts = entry.getKey().split("/", 2);
						int arity = Integer.parseInt(parts[1]);
						String fnName = imp.name != null ? imp.name + "::" + parts[0] : parts[0];
						FunctionNameAndArity key = FunctionNameAndArity.of(fnName, arity);
						env.addFunction(key, entry.getValue());
						context.addGlobalFunction(key);
					}
				}
			}
			Expression<JsonNode> compiledInner = compileNonNull(env, context, currentModule, top.expr());
			return new TopLevelExpression<>(top.moduleDirective(), Collections.emptyList(), compiledInner);
		}

		if (ast instanceof PipedQueryAstNode) {
			PipedQueryAstNode piped = (PipedQueryAstNode) ast;
			List<PipeComponent<JsonNode>> newComponents = new ArrayList<>();

			@Var int pushedScopes = 0;
			try {
				for (PipedQueryAstNode.PipeComponent comp : piped.components()) {
					if (comp instanceof PipedQueryAstNode.AssignPipeComponent) {
						PipedQueryAstNode.AssignPipeComponent assign = (PipedQueryAstNode.AssignPipeComponent) comp;
						Expression<JsonNode> compiledExpr = compileNonNull(env, context, assign.expr);
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

						newComponents.add(new AssignPipeComponent<>(compiledExpr, compiledMatcher));
					} else if (comp instanceof PipedQueryAstNode.TransformPipeComponent) {
						PipedQueryAstNode.TransformPipeComponent transform = (PipedQueryAstNode.TransformPipeComponent) comp;
						Expression<JsonNode> compiledExpr = compileNonNull(env, context, transform.expr);
						newComponents.add(new TransformPipeComponent<>(compiledExpr));
					} else if (comp instanceof PipedQueryAstNode.LabelPipeComponent) {
						PipedQueryAstNode.LabelPipeComponent label = (PipedQueryAstNode.LabelPipeComponent) comp;
						newComponents.add(new LabelPipeComponent<>(label.name));
					} else {
						throw new IllegalStateException("Unknown pipe component: " + comp.getClass());
					}
				}
			} finally {
				for (int i = 0; i < pushedScopes; i++) {
					context.popScope();
				}
			}

			return new PipedQuery<>(newComponents);
		}

		if (ast instanceof SemicolonOperatorAstNode) {
			SemicolonOperatorAstNode semi = (SemicolonOperatorAstNode) ast;
			List<Expression<JsonNode>> newExpressions = new ArrayList<>();
			for (AstNode q : semi.expressions()) {
				newExpressions.add(compileNonNull(env, context, q));
			}
			return new SemicolonOperator<>(newExpressions);
		}

		if (ast instanceof ObjectConstructionAstNode) {
			ObjectConstructionAstNode obj = (ObjectConstructionAstNode) ast;
			ObjectConstruction<JsonNode> res = new ObjectConstruction<>(env.jsonProvider());
			for (ObjectConstructionAstNode.FieldConstructionAst fc : obj.fields) {
				if (fc instanceof ObjectConstructionAstNode.IdentifierKeyFieldConstructionAst) {
					ObjectConstructionAstNode.IdentifierKeyFieldConstructionAst ik = (ObjectConstructionAstNode.IdentifierKeyFieldConstructionAst) fc;
					Expression<JsonNode> val = compile(env, context, ik.value);
					res.add(new IdentifierKeyFieldConstruction<>(env.jsonProvider(), ik.key, val));
				} else if (fc instanceof ObjectConstructionAstNode.JsonQueryKeyFieldConstructionAst) {
					ObjectConstructionAstNode.JsonQueryKeyFieldConstructionAst jq = (ObjectConstructionAstNode.JsonQueryKeyFieldConstructionAst) fc;
					Expression<JsonNode> key = compileNonNull(env, context, jq.key());
					Expression<JsonNode> val = compileNonNull(env, context, jq.value());
					res.add(new JsonQueryKeyFieldConstruction<>(env.jsonProvider(), key, val));
				} else if (fc instanceof ObjectConstructionAstNode.StringKeyFieldConstructionAst) {
					ObjectConstructionAstNode.StringKeyFieldConstructionAst sk = (ObjectConstructionAstNode.StringKeyFieldConstructionAst) fc;
					Expression<JsonNode> key = compileNonNull(env, context, sk.key);
					Expression<JsonNode> val = compile(env, context, sk.value);
					res.add(new StringKeyFieldConstruction<>(env.jsonProvider(), key, val));
				} else if (fc instanceof ObjectConstructionAstNode.VariableKeyFieldConstruction) {
					// desugar `{ $x }` into the same shape as `{ x: $x }` -- no dedicated resolved class needed.
					ObjectConstructionAstNode.VariableKeyFieldConstruction vk = (ObjectConstructionAstNode.VariableKeyFieldConstruction) fc;
					Expression<JsonNode> compiledValue = compileVariableRef(env, context, null, vk.name());
					res.add(new IdentifierKeyFieldConstruction<>(env.jsonProvider(), vk.name(), compiledValue));
				} else {
					throw new IllegalStateException("Unknown field construction: " + fc.getClass());
				}
			}
			return res;
		}

		if (ast instanceof ArrayConstructionAstNode) {
			ArrayConstructionAstNode arr = (ArrayConstructionAstNode) ast;
			return new ArrayConstruction<>(env.jsonProvider(), compile(env, context, arr.q));
		}

		if (ast instanceof BinaryOpAstNode) {
			BinaryOpAstNode bin = (BinaryOpAstNode) ast;
			Expression<JsonNode> lhs = compileNonNull(env, context, bin.lhs);
			Expression<JsonNode> rhs = compileNonNull(env, context, bin.rhs);
			return bin.operator.create(lhs, rhs, env.version(), env.jsonProvider());
		}

		if (ast instanceof NegativeExpressionAstNode) {
			NegativeExpressionAstNode neg = (NegativeExpressionAstNode) ast;
			return new NegativeExpression<>(env.jsonProvider(), compileNonNull(env, context, neg.value()));
		}

		if (ast instanceof ConditionalAstNode) {
			ConditionalAstNode cond = (ConditionalAstNode) ast;
			List<Pair<Expression<JsonNode>, Expression<JsonNode>>> newSwitches = new ArrayList<>();
			for (Pair<AstNode, AstNode> sw : cond.switches()) {
				Expression<JsonNode> newIf = compileNonNull(env, context, sw._1);
				Expression<JsonNode> newThen = compileNonNull(env, context, sw._2);
				newSwitches.add(Pair.of(newIf, newThen));
			}
			Expression<JsonNode> newElse = compileNonNull(env, context, cond.otherwise());
			return new Conditional<>(env.jsonProvider(), newSwitches, newElse);
		}

		if (ast instanceof TryCatchAstNode) {
			TryCatchAstNode tc = (TryCatchAstNode) ast;
			Expression<JsonNode> newTry = compileNonNull(env, context, tc.tryExpr());
			Expression<JsonNode> newCatch = compile(env, context, tc.catchExpr());
			if (tc instanceof TryCatchAstNode.Question) {
				return new TryCatch.Question<>(env.jsonProvider(), newTry);
			}
			return new TryCatch<>(env.jsonProvider(), newTry, newCatch);
		}

		if (ast instanceof TupleAstNode) {
			TupleAstNode tuple = (TupleAstNode) ast;
			List<Expression<JsonNode>> newQs = new ArrayList<>();
			for (AstNode q : tuple.qs) {
				newQs.add(compileNonNull(env, context, q));
			}
			return new Tuple<>(newQs);
		}

		if (ast instanceof ReduceExpressionAstNode) {
			ReduceExpressionAstNode red = (ReduceExpressionAstNode) ast;
			Expression<JsonNode> compiledIter = compileNonNull(env, context, red.iterExpr());
			Expression<JsonNode> compiledInit = compileNonNull(env, context, red.initExpr());
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
				Expression<JsonNode> compiledReduce = compileNonNull(env, context, red.reduceExpr());
				return new ReduceExpression<>(env.jsonProvider(), compiledMatcher, compiledInit, compiledReduce, compiledIter);
			} finally {
				context.popScope();
			}
		}

		if (ast instanceof ForeachExpressionAstNode) {
			ForeachExpressionAstNode fe = (ForeachExpressionAstNode) ast;
			Expression<JsonNode> compiledIter = compileNonNull(env, context, fe.iterExpr());
			Expression<JsonNode> compiledInit = compileNonNull(env, context, fe.initExpr());
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
				Expression<JsonNode> compiledUpdate = compileNonNull(env, context, fe.updateExpr());
				Expression<JsonNode> compiledExtract = fe.extractExpr() != null ? compile(env, context, fe.extractExpr()) : null;
				return new ForeachExpression<>(compiledMatcher, compiledInit, compiledUpdate, compiledExtract, compiledIter);
			} finally {
				context.popScope();
			}
		}

		if (ast instanceof FormattingFilterAstNode) {
			FormattingFilterAstNode ff = (FormattingFilterAstNode) ast;
			String fname = ff.name().startsWith("@") ? ff.name() : "@" + ff.name();
			SymbolLocation loc = context.getFunctionLocation(fname, 0);
			if (loc == null) {
				throw new JsonQueryException(String.format("Formatting operator %s does not exist", fname));
			}
			Function defaultFactory = loc.isGlobal ? env.getFunction(FunctionNameAndArity.of(fname, 0)) : null;
			Expression<JsonNode> defaultFunction = defaultFactory != null ? defaultFactory.bindArguments(env.jsonProvider(), Collections.emptyList(), env.version()) : null;
			if (!loc.isLocal)
				return new ResolvedCapturedFunctionAccess<>(env.jsonProvider(), env.version(), fname, loc.slot, context.getCurrentFunctionClosureSlot(), Collections.emptyList(), defaultFactory, defaultFunction);
			return new ResolvedLocalFunctionAccess<>(env.jsonProvider(), env.version(), fname, loc.slot, Collections.emptyList(), defaultFactory, defaultFunction);
		}

		if (ast instanceof StringInterpolationAstNode) {
			StringInterpolationAstNode si = (StringInterpolationAstNode) ast;
			List<Pair<Integer, Expression<JsonNode>>> compiledInterpolations = new ArrayList<>();
			for (Pair<Integer, AstNode> pair : si.interpolations()) {
				Expression<JsonNode> resExpr = compileNonNull(env, context, pair._2);
				compiledInterpolations.add(Pair.of(pair._1, resExpr));
			}
			Expression<JsonNode> compiledFormatter = compile(env, context, si.formatter());
			return new StringInterpolation<>(env.jsonProvider(), si.template(), compiledInterpolations, compiledFormatter);
		}

		if (ast instanceof BracketFieldAccessAstNode) {
			BracketFieldAccessAstNode bfa = (BracketFieldAccessAstNode) ast;
			Expression<JsonNode> target = compileNonNull(env, context, bfa.target());
			@Var Expression<JsonNode> start = compile(env, context, bfa.startExpr());
			@Var Expression<JsonNode> end = compile(env, context, bfa.endExpr());
			if (start == null)
				start = new NullLiteral<>(env.jsonProvider());
			if (end == null)
				end = new NullLiteral<>(env.jsonProvider());
			if (bfa.isRange()) {
				return new BracketFieldAccess<>(env.jsonProvider(), target, start, end, bfa.permissive());
			} else {
				return new BracketFieldAccess<>(env.jsonProvider(), target, start, bfa.permissive());
			}
		}

		if (ast instanceof IdentifierFieldAccessAstNode) {
			IdentifierFieldAccessAstNode ifa = (IdentifierFieldAccessAstNode) ast;
			Expression<JsonNode> target = compileNonNull(env, context, ifa.target());
			return new IdentifierFieldAccess<>(env.jsonProvider(), target, ifa.field(), ifa.permissive());
		}

		if (ast instanceof StringFieldAccessAstNode) {
			StringFieldAccessAstNode sfa = (StringFieldAccessAstNode) ast;
			Expression<JsonNode> target = compileNonNull(env, context, sfa.target());
			Expression<JsonNode> key = compileNonNull(env, context, sfa.key());
			return new StringFieldAccess<>(env.jsonProvider(), target, key, sfa.permissive());
		}

		if (ast instanceof BracketExtractFieldAccessAstNode) {
			BracketExtractFieldAccessAstNode befa = (BracketExtractFieldAccessAstNode) ast;
			Expression<JsonNode> target = compileNonNull(env, context, befa.target());
			return new BracketExtractFieldAccess<>(env.jsonProvider(), target, befa.permissive());
		}

		if (ast instanceof BooleanLiteralAstNode) {
			return new BooleanLiteral<>(env.jsonProvider(), ((BooleanLiteralAstNode) ast).value());
		}

		if (ast instanceof LongLiteralAstNode) {
			return new LongLiteral<>(env.jsonProvider(), ((LongLiteralAstNode) ast).value());
		}

		if (ast instanceof DoubleLiteralAstNode) {
			return new DoubleLiteral<>(env.jsonProvider(), ((DoubleLiteralAstNode) ast).value());
		}

		if (ast instanceof NullLiteralAstNode) {
			return new NullLiteral<>(env.jsonProvider());
		}

		if (ast instanceof StringLiteralAstNode) {
			return new StringLiteral<>(env.jsonProvider(), ((StringLiteralAstNode) ast).value());
		}

		if (ast instanceof ThisObjectAstNode) {
			return new ThisObject();
		}

		if (ast instanceof RecursionOperatorAstNode) {
			return new RecursionOperator<>(env.jsonProvider());
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
			Expression<JsonNode> compiledBody;
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
				context.recordRootFunctionSlot(FunctionNameAndArity.of(fd.fname(), fd.args().size()), slot);
			}
			return new ResolvedFunctionDefinition(slot, closureSpec, fnSize, fd.args(), paramSlots, compiledBody, ownClosureSlot, definerClosureSlot);
		}

		throw new IllegalStateException("Unknown AST node: " + ast.getClass());
	}

	private static <N> Expression<N> compileVariableRef(Environment<N> env, CompileContext context, @Nullable String moduleName, String varName) throws JsonQueryException {
		if (moduleName != null) {
			String fullName = moduleName + "::" + varName;
			if (context.isLocalVariable(fullName)) {
				SymbolLocation loc = context.getVariableLocation(fullName);
				int slot = loc != null ? loc.slot : 0;
				if (loc != null && loc.isGlobal) {
					@Var Supplier<N> supplier = env.getVariable(fullName);
					if (supplier == null && moduleName.equals(varName))
						supplier = env.getVariable(varName);
					if (supplier == null)
						throw new JsonQueryException(String.format("Variable $%s::%s is not defined", moduleName, varName));
					return new ResolvedGlobalVariableAccess<>(fullName, slot, !loc.isLocal, context.getCurrentFunctionClosureSlot(), supplier);
				}
				if (loc != null && !loc.isLocal) {
					return new ResolvedCapturedVariableAccess<>(fullName, slot, context.getCurrentFunctionClosureSlot());
				}
				return new ResolvedLocalVariableAccess<>(fullName, slot);
			}

			throw new JsonQueryException(String.format("Variable $%s::%s is not defined", moduleName, varName));
		}

		if (context.isLocalVariable(varName)) {
			SymbolLocation loc = context.getVariableLocation(varName);
			int slot = loc != null ? loc.slot : 0;
			if (loc != null && loc.isGlobal) {
				Supplier<N> supplier = env.getVariable(varName);
				if (supplier == null)
					throw new JsonQueryException(String.format("Variable $%s is not defined", varName));
				return new ResolvedGlobalVariableAccess<>(varName, slot, !loc.isLocal, context.getCurrentFunctionClosureSlot(), supplier);
			}
			if (loc != null && !loc.isLocal) {
				return new ResolvedCapturedVariableAccess<>(varName, slot, context.getCurrentFunctionClosureSlot());
			}
			return new ResolvedLocalVariableAccess<>(varName, slot);
		}

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
			return new ArrayMatcher<>(env.jsonProvider(), compiled);
		}
		if (matcher instanceof ObjectMatcherAstNode) {
			ObjectMatcherAstNode om = (ObjectMatcherAstNode) matcher;
			List<ObjectMatcher.FieldMatcher<N>> compiled = new ArrayList<>();
			for (ObjectMatcherAstNode.FieldMatcher fm : om.matchers()) {
				Expression<N> name = compileNonNull(env, context, fm.name());
				PatternMatcher<N> sub = fm.rawMatcher() != null ? compileMatcher(env, context, fm.rawMatcher()) : null;
				compiled.add(new ObjectMatcher.FieldMatcher<>(fm.dollar(), name, sub));
			}
			return new ObjectMatcher<>(env.jsonProvider(), compiled);
		}
		throw new IllegalStateException("Unknown matcher type: " + matcher.getClass());
	}

	public static <N> void bindAndApply(@Nullable StackFrame callerFrame, StackFrame currentFrame, List<String> paramNames, List<Integer> paramSlots, List<Expression<N>> fnArgs, N in, @Nullable Path<N> path, PathOutput<N> output, Consumer<StackFrame> bodyTask) throws JsonQueryException {
		for (int i = 0; i < paramNames.size(); i++) {
			String pName = paramNames.get(i);
			int slot = paramSlots.get(i);
			Expression<N> pExpr = fnArgs.get(i);
			if (!pName.startsWith("$")) {
				currentFrame.set(slot, new Function() {
					@Override
					@SuppressWarnings("unchecked")
					public <N1> Expression<N1> bindArguments(JsonProvider<N1> jp, List<Expression<N1>> emptyArgs, Version v) {
						Expression<N1> effectiveExpr = (Expression<N1>) (Expression<?>) pExpr;
						StackFrame effectiveCallerFrame = (StackFrame) (Object) callerFrame;
						return (sFrame, inVal, pVal, outVal, ignoredRequirePath) -> effectiveExpr.apply(effectiveCallerFrame, inVal, pVal, outVal, false);
					}
				});
			}
		}
		bindValueParams(callerFrame, currentFrame, paramNames, paramSlots, fnArgs, 0, in, path, output, bodyTask);
	}

	private static <N> void bindValueParams(@Nullable StackFrame callerFrame, StackFrame currentFrame, List<String> paramNames, List<Integer> paramSlots, List<Expression<N>> fnArgs, int index, N in, @Nullable Path<N> path, PathOutput<N> output, Consumer<StackFrame> bodyTask) throws JsonQueryException {
		if (index >= paramNames.size()) {
			bodyTask.accept(currentFrame);
			return;
		}
		String argName = paramNames.get(index);
		Expression<N> argExpr = fnArgs.get(index);
		int slot = paramSlots.get(index);
		if (argName.startsWith("$")) {
			argExpr.apply(callerFrame, in, path, (val, p) -> {
				currentFrame.set(slot, val);
				bindValueParams(callerFrame, currentFrame, paramNames, paramSlots, fnArgs, index + 1, in, path, output, bodyTask);
			}, false);
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
				if (fm.dollar() && fm.name() instanceof StringLiteralAstNode) {
					out.add(((StringLiteralAstNode) fm.name()).value());
				}
				if (fm.rawMatcher() != null) {
					collectVariableNames(fm.rawMatcher(), out);
				}
			}
		}
	}

	public static <JsonNode> Expression<JsonNode> compileNonNull(Environment<JsonNode> env, CompileContext context, AstNode ast) throws JsonQueryException {
		return compileNonNull(env, context, (Module) null, ast);
	}

	public static <JsonNode> Expression<JsonNode> compileNonNull(Environment<JsonNode> env, CompileContext context, @Nullable Module currentModule, AstNode ast) throws JsonQueryException {
		Expression<JsonNode> compiled = compile(env, context, currentModule, ast);
		if (compiled == null)
			throw new JsonQueryException("Cannot resolve null expression");
		return compiled;
	}
}
