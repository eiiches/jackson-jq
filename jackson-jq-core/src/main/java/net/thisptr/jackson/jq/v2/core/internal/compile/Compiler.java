package net.thisptr.jackson.jq.v2.core.internal.compile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.internal.ast.ArrayConstruction;
import net.thisptr.jackson.jq.v2.core.internal.ast.AssignPipeComponentAst;
import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.Conditional;
import net.thisptr.jackson.jq.v2.core.internal.ast.FieldConstructionAst;
import net.thisptr.jackson.jq.v2.core.internal.ast.ForeachExpression;
import net.thisptr.jackson.jq.v2.core.internal.ast.FormattingFilter;
import net.thisptr.jackson.jq.v2.core.internal.ast.FunctionCall;
import net.thisptr.jackson.jq.v2.core.internal.ast.FunctionDefinition;
import net.thisptr.jackson.jq.v2.core.internal.ast.IdentifierKeyFieldConstructionAst;
import net.thisptr.jackson.jq.v2.core.internal.ast.ImportStatement;
import net.thisptr.jackson.jq.v2.core.internal.ast.JsonQueryKeyFieldConstructionAst;
import net.thisptr.jackson.jq.v2.core.internal.ast.NegativeExpression;
import net.thisptr.jackson.jq.v2.core.internal.ast.ObjectConstruction;
import net.thisptr.jackson.jq.v2.core.internal.ast.PipeComponentAst;
import net.thisptr.jackson.jq.v2.core.internal.ast.PipedQuery;
import net.thisptr.jackson.jq.v2.core.internal.ast.ReduceExpression;
import net.thisptr.jackson.jq.v2.core.internal.ast.SemicolonOperator;
import net.thisptr.jackson.jq.v2.core.internal.ast.StringInterpolation;
import net.thisptr.jackson.jq.v2.core.internal.ast.StringKeyFieldConstructionAst;
import net.thisptr.jackson.jq.v2.core.internal.ast.TopLevelExpression;
import net.thisptr.jackson.jq.v2.core.internal.ast.TransformPipeComponentAst;
import net.thisptr.jackson.jq.v2.core.internal.ast.TryCatch;
import net.thisptr.jackson.jq.v2.core.internal.ast.Tuple;
import net.thisptr.jackson.jq.v2.core.internal.ast.VariableAccess;
import net.thisptr.jackson.jq.v2.core.internal.ast.VariableKeyFieldConstruction;
import net.thisptr.jackson.jq.v2.core.internal.ast.binaryop.BinaryOpNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.fieldaccess.BracketExtractFieldAccess;
import net.thisptr.jackson.jq.v2.core.internal.ast.fieldaccess.BracketFieldAccess;
import net.thisptr.jackson.jq.v2.core.internal.ast.fieldaccess.IdentifierFieldAccess;
import net.thisptr.jackson.jq.v2.core.internal.ast.fieldaccess.StringFieldAccess;
import net.thisptr.jackson.jq.v2.core.internal.ast.matcher.PatternMatcherAst;
import net.thisptr.jackson.jq.v2.core.internal.ast.matcher.matchers.ArrayMatcherAst;
import net.thisptr.jackson.jq.v2.core.internal.ast.matcher.matchers.ObjectMatcherAst;
import net.thisptr.jackson.jq.v2.core.internal.misc.Pair;
import net.thisptr.jackson.jq.v2.core.internal.tree.literal.StringLiteral;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.matchers.ArrayMatcher;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.matchers.ObjectMatcher;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.matchers.ValueMatcher;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.FunctionNameAndArity;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class Compiler {

	public static <JsonNode> Expression compile(Environment<JsonNode> env, AstNode ast) throws JsonQueryException {
		return compile(env, (Module) null, ast);
	}

	public static <JsonNode> Expression compile(Environment<JsonNode> env, @Nullable Module currentModule, AstNode ast) throws JsonQueryException {
		CompileContext context = new CompileContext();
		Expression compiled = compile(env, context, currentModule, ast);
		if (compiled == null)
			throw new JsonQueryException("Cannot resolve null expression");
		return new net.thisptr.jackson.jq.v2.core.internal.tree.RootExpression<>(context.getSlotCount(), compiled);
	}

	public static <JsonNode> @Nullable Expression compile(Environment<JsonNode> env, CompileContext context, @Nullable AstNode ast) throws JsonQueryException {
		return compile(env, context, (Module) null, ast);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static <JsonNode> @Nullable Expression compile(Environment<JsonNode> env, CompileContext context, @Nullable Module currentModule, @Nullable AstNode ast) throws JsonQueryException {
		if (ast == null)
			return null;

		if (ast instanceof FunctionCall) {
			FunctionCall call = (FunctionCall) ast;
			List<Expression> compiledArgs = new ArrayList<>();
			for (AstNode arg : call.args()) {
				compiledArgs.add(compile(env, context, currentModule, arg));
			}

			String fullName = call.moduleName() != null ? call.moduleName() + "::" + call.name() : call.name();
			if (call.moduleName() == null && context.isLocalFunction(call.name(), compiledArgs.size())) {
				SymbolLocation loc = context.getFunctionLocation(call.name(), compiledArgs.size());
				int slot = loc != null ? loc.slot : 0;
				if (loc != null && !loc.isLocal) {
					return new net.thisptr.jackson.jq.v2.core.internal.tree.ResolvedCapturedFunctionAccess(call.name(), slot, compiledArgs);
				}
				return new net.thisptr.jackson.jq.v2.core.internal.tree.ResolvedLocalFunctionAccess(call.name(), slot, compiledArgs);
			}

			FunctionNameAndArity key = FunctionNameAndArity.of(fullName, compiledArgs.size());
			FunctionFactory factory = env.getFunctionFactory(key);
			if (factory == null) {
				throw new JsonQueryException(String.format("Function %s/%d does not exist", fullName, compiledArgs.size()));
			}

			Function<JsonNode> fn = factory.createFunction(env.jsonProvider(), compiledArgs, env.version());
			return new net.thisptr.jackson.jq.v2.core.internal.tree.ResolvedFunctionCall<>(fullName, fn);
		}

		if (ast instanceof VariableAccess) {
			VariableAccess varAccess = (VariableAccess) ast;
			return compileVariableRef(env, context, varAccess.moduleName(), varAccess.name());
		}

		if (ast instanceof TopLevelExpression) {
			TopLevelExpression<JsonNode> top = (TopLevelExpression<JsonNode>) ast;
			for (ImportStatement<JsonNode> imp : top.imports()) {
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
					}
				} else {
					Module mod = env.getModuleLoader().loadModule(currentModule, imp.path, metadata);
					if (mod == null) {
						throw new JsonQueryException(String.format("module not found: %s", imp.path));
					}
					for (Entry<String, FunctionFactory> entry : mod.getAllFunctions().entrySet()) {
						String[] parts = entry.getKey().split("/", 2);
						int arity = Integer.parseInt(parts[1]);
						String fnName = imp.name != null ? imp.name + "::" + parts[0] : parts[0];
						env.addFunctionFactory(FunctionNameAndArity.of(fnName, arity), entry.getValue());
					}
				}
			}
			Expression compiledInner = compileNonNull(env, context, currentModule, top.expr());
			return new net.thisptr.jackson.jq.v2.core.internal.tree.TopLevelExpression<>(top.moduleDirective(), Collections.emptyList(), compiledInner);
		}

		if (ast instanceof PipedQuery) {
			PipedQuery piped = (PipedQuery) ast;
			List<net.thisptr.jackson.jq.v2.core.internal.tree.PipeComponent<JsonNode>> newComponents = new ArrayList<>();

			@Var int pushedScopes = 0;
			try {
				for (PipeComponentAst comp : piped.components()) {
					if (comp instanceof AssignPipeComponentAst) {
						AssignPipeComponentAst assign = (AssignPipeComponentAst) comp;
						Expression compiledExpr = compileNonNull(env, context, assign.expr);
						PatternMatcher<JsonNode> compiledMatcher = compileMatcher(env, context, assign.matcher);

						context.pushLocalScope();
						pushedScopes++;

						Set<String> varNames = new HashSet<>();
						collectVariableNames(assign.matcher, varNames);
						Map<String, Integer> slots = new HashMap<>();
						for (String varName : varNames) {
							context.addLocalVariable(varName);
							slots.put(varName, context.getSlot(varName));
						}

						newComponents.add(new net.thisptr.jackson.jq.v2.core.internal.tree.AssignPipeComponent<>(compiledExpr, compiledMatcher, slots));
					} else if (comp instanceof TransformPipeComponentAst) {
						TransformPipeComponentAst transform = (TransformPipeComponentAst) comp;
						Expression compiledExpr = compileNonNull(env, context, transform.expr);
						newComponents.add(new net.thisptr.jackson.jq.v2.core.internal.tree.TransformPipeComponent<>(compiledExpr));
					} else {
						newComponents.add((net.thisptr.jackson.jq.v2.core.internal.tree.PipeComponent) comp);
					}
				}
			} finally {
				for (int i = 0; i < pushedScopes; i++) {
					context.popScope();
				}
			}

			return new net.thisptr.jackson.jq.v2.core.internal.tree.PipedQuery<>(newComponents);
		}

		if (ast instanceof SemicolonOperator) {
			SemicolonOperator semi = (SemicolonOperator) ast;
			List<Expression> newExpressions = new ArrayList<>();
			for (AstNode q : semi.expressions()) {
				newExpressions.add(compileNonNull(env, context, q));
			}
			return new net.thisptr.jackson.jq.v2.core.internal.tree.SemicolonOperator(newExpressions);
		}

		if (ast instanceof ObjectConstruction) {
			ObjectConstruction obj = (ObjectConstruction) ast;
			net.thisptr.jackson.jq.v2.core.internal.tree.ObjectConstruction<JsonNode> res = new net.thisptr.jackson.jq.v2.core.internal.tree.ObjectConstruction<>();
			for (FieldConstructionAst fc : obj.fields) {
				if (fc instanceof IdentifierKeyFieldConstructionAst) {
					IdentifierKeyFieldConstructionAst ik = (IdentifierKeyFieldConstructionAst) fc;
					Expression val = compile(env, context, ik.value);
					res.add(new net.thisptr.jackson.jq.v2.core.internal.tree.IdentifierKeyFieldConstruction<>(ik.key, val));
				} else if (fc instanceof JsonQueryKeyFieldConstructionAst) {
					JsonQueryKeyFieldConstructionAst jq = (JsonQueryKeyFieldConstructionAst) fc;
					Expression key = compileNonNull(env, context, jq.key());
					Expression val = compileNonNull(env, context, jq.value());
					res.add(new net.thisptr.jackson.jq.v2.core.internal.tree.JsonQueryKeyFieldConstruction<>(key, val));
				} else if (fc instanceof StringKeyFieldConstructionAst) {
					StringKeyFieldConstructionAst sk = (StringKeyFieldConstructionAst) fc;
					Expression key = compileNonNull(env, context, sk.key);
					Expression val = compile(env, context, sk.value);
					res.add(new net.thisptr.jackson.jq.v2.core.internal.tree.StringKeyFieldConstruction<>(key, val));
				} else if (fc instanceof VariableKeyFieldConstruction) {
					// desugar `{ $x }` into the same shape as `{ x: $x }` -- no dedicated resolved class needed.
					VariableKeyFieldConstruction vk = (VariableKeyFieldConstruction) fc;
					Expression compiledValue = compileVariableRef(env, context, null, vk.name());
					res.add(new net.thisptr.jackson.jq.v2.core.internal.tree.IdentifierKeyFieldConstruction<>(vk.name(), compiledValue));
				} else {
					throw new IllegalStateException("Unknown field construction: " + fc.getClass());
				}
			}
			return res;
		}

		if (ast instanceof ArrayConstruction) {
			ArrayConstruction arr = (ArrayConstruction) ast;
			return new net.thisptr.jackson.jq.v2.core.internal.tree.ArrayConstruction(compile(env, context, arr.q));
		}

		if (ast instanceof BinaryOpNode) {
			BinaryOpNode bin = (BinaryOpNode) ast;
			Expression lhs = compileNonNull(env, context, bin.lhs);
			Expression rhs = compileNonNull(env, context, bin.rhs);
			return bin.operator.create(lhs, rhs, env.version());
		}

		if (ast instanceof NegativeExpression) {
			NegativeExpression neg = (NegativeExpression) ast;
			return new net.thisptr.jackson.jq.v2.core.internal.tree.NegativeExpression(compileNonNull(env, context, neg.value()));
		}

		if (ast instanceof Conditional) {
			Conditional cond = (Conditional) ast;
			List<Pair<Expression, Expression>> newSwitches = new ArrayList<>();
			for (Pair<AstNode, AstNode> sw : cond.switches()) {
				Expression newIf = compileNonNull(env, context, sw._1);
				Expression newThen = compileNonNull(env, context, sw._2);
				newSwitches.add(Pair.of(newIf, newThen));
			}
			Expression newElse = compileNonNull(env, context, cond.otherwise());
			return new net.thisptr.jackson.jq.v2.core.internal.tree.Conditional(newSwitches, newElse);
		}

		if (ast instanceof TryCatch) {
			TryCatch tc = (TryCatch) ast;
			Expression newTry = compileNonNull(env, context, tc.tryExpr());
			Expression newCatch = compile(env, context, tc.catchExpr());
			if (tc instanceof TryCatch.Question) {
				return new net.thisptr.jackson.jq.v2.core.internal.tree.TryCatch.Question(newTry);
			}
			return new net.thisptr.jackson.jq.v2.core.internal.tree.TryCatch(newTry, newCatch);
		}

		if (ast instanceof Tuple) {
			Tuple tuple = (Tuple) ast;
			List<Expression> newQs = new ArrayList<>();
			for (AstNode q : tuple.qs) {
				newQs.add(compileNonNull(env, context, q));
			}
			return new net.thisptr.jackson.jq.v2.core.internal.tree.Tuple(newQs);
		}

		if (ast instanceof ReduceExpression) {
			ReduceExpression red = (ReduceExpression) ast;
			Expression compiledIter = compileNonNull(env, context, red.iterExpr());
			Expression compiledInit = compileNonNull(env, context, red.initExpr());
			PatternMatcher<JsonNode> compiledMatcher = compileMatcher(env, context, red.matcher());

			Set<String> varNames = new HashSet<>();
			collectVariableNames(red.matcher(), varNames);
			Map<String, Integer> slots = new HashMap<>();
			context.pushLocalScope();
			try {
				for (String varName : varNames) {
					context.addLocalVariable(varName);
					slots.put(varName, context.getSlot(varName));
				}
				Expression compiledReduce = compileNonNull(env, context, red.reduceExpr());
				return new net.thisptr.jackson.jq.v2.core.internal.tree.ReduceExpression<>(compiledMatcher, compiledInit, compiledReduce, compiledIter, slots);
			} finally {
				context.popScope();
			}
		}

		if (ast instanceof ForeachExpression) {
			ForeachExpression fe = (ForeachExpression) ast;
			Expression compiledIter = compileNonNull(env, context, fe.iterExpr());
			Expression compiledInit = compileNonNull(env, context, fe.initExpr());
			PatternMatcher<JsonNode> compiledMatcher = compileMatcher(env, context, fe.matcher());

			Set<String> varNames = new HashSet<>();
			collectVariableNames(fe.matcher(), varNames);
			Map<String, Integer> slots = new HashMap<>();
			context.pushLocalScope();
			try {
				for (String varName : varNames) {
					context.addLocalVariable(varName);
					slots.put(varName, context.getSlot(varName));
				}
				Expression compiledUpdate = compileNonNull(env, context, fe.updateExpr());
				Expression compiledExtract = fe.extractExpr() != null ? compile(env, context, fe.extractExpr()) : null;
				return new net.thisptr.jackson.jq.v2.core.internal.tree.ForeachExpression<>(compiledMatcher, compiledInit, compiledUpdate, compiledExtract, compiledIter, slots);
			} finally {
				context.popScope();
			}
		}

		if (ast instanceof FormattingFilter) {
			FormattingFilter ff = (FormattingFilter) ast;
			String fname = ff.name().startsWith("@") ? ff.name() : "@" + ff.name();
			FunctionNameAndArity key = FunctionNameAndArity.of(fname, 0);
			FunctionFactory factory = env.getFunctionFactory(key);
			if (factory == null) {
				throw new JsonQueryException(String.format("Formatting operator %s does not exist", fname));
			}
			Function<JsonNode> fn = factory.createFunction(env.jsonProvider(), Collections.emptyList(), env.version());
			return new net.thisptr.jackson.jq.v2.core.internal.tree.ResolvedFunctionCall<>(fname, fn);
		}

		if (ast instanceof StringInterpolation) {
			StringInterpolation si = (StringInterpolation) ast;
			List<Pair<Integer, Expression>> compiledInterpolations = new ArrayList<>();
			for (Pair<Integer, AstNode> pair : si.interpolations()) {
				Expression resExpr = compileNonNull(env, context, pair._2);
				compiledInterpolations.add(Pair.of(pair._1, resExpr));
			}
			Expression compiledFormatter = compile(env, context, si.formatter());
			return new net.thisptr.jackson.jq.v2.core.internal.tree.StringInterpolation(si.template(), compiledInterpolations, compiledFormatter);
		}

		if (ast instanceof BracketFieldAccess) {
			BracketFieldAccess bfa = (BracketFieldAccess) ast;
			Expression target = compileNonNull(env, context, bfa.target());
			@Var Expression start = compile(env, context, bfa.startExpr());
			@Var Expression end = compile(env, context, bfa.endExpr());
			if (start == null)
				start = new net.thisptr.jackson.jq.v2.core.internal.tree.literal.NullLiteral();
			if (end == null)
				end = new net.thisptr.jackson.jq.v2.core.internal.tree.literal.NullLiteral();
			if (bfa.isRange()) {
				return new net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess.BracketFieldAccess(target, start, end, bfa.permissive());
			} else {
				return new net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess.BracketFieldAccess(target, start, bfa.permissive());
			}
		}

		if (ast instanceof IdentifierFieldAccess) {
			IdentifierFieldAccess ifa = (IdentifierFieldAccess) ast;
			Expression target = compileNonNull(env, context, ifa.target());
			return new net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess.IdentifierFieldAccess(target, ifa.field(), ifa.permissive());
		}

		if (ast instanceof StringFieldAccess) {
			StringFieldAccess sfa = (StringFieldAccess) ast;
			Expression target = compileNonNull(env, context, sfa.target());
			Expression key = compileNonNull(env, context, sfa.key());
			return new net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess.StringFieldAccess(target, key, sfa.permissive());
		}

		if (ast instanceof BracketExtractFieldAccess) {
			BracketExtractFieldAccess befa = (BracketExtractFieldAccess) ast;
			Expression target = compileNonNull(env, context, befa.target());
			return new net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess.BracketExtractFieldAccess(target, befa.permissive());
		}

		if (ast instanceof FunctionDefinition) {
			FunctionDefinition fd = (FunctionDefinition) ast;
			context.addLocalFunction(fd.fname(), fd.args().size());

			CompileContext fnContext = context.copy();
			fnContext.pushFunctionScope();
			List<Integer> paramSlots = new ArrayList<>();
			for (String arg : fd.args()) {
				if (arg.startsWith("$")) {
					fnContext.addLocalVariable(arg.substring(1));
					paramSlots.add(fnContext.getSlot(arg.substring(1)));
				} else {
					fnContext.addLocalFunction(arg, 0);
					paramSlots.add(fnContext.getSlot(arg));
				}
			}
			int fnSize = fnContext.getSlotCount();
			Expression compiledBody = compileNonNull(env, fnContext, fd.body());
			ClosureSpec closureSpec = fnContext.getClosureSpec();

			FunctionNameAndArity key = FunctionNameAndArity.of(fd.fname(), fd.args().size());
			FunctionFactory envFactory = new FunctionFactory() {
				@Override
				@SuppressWarnings({"unchecked", "rawtypes"})
				public <N> Function<N> createFunction(JsonProvider<N> jsonProvider, List<Expression> fnArgs, Version version) {
					return (callerFrame, input, path, output) -> {
						ExecutionStack<N>.Frame fnFrame = callerFrame != null
								? callerFrame.getStack().pushFrame(callerFrame, fnSize)
								: new ExecutionStack<N>().pushFrame(callerFrame, fnSize);
						try {
							bindAndApply(jsonProvider, callerFrame, fnFrame, fd.args(), paramSlots, fnArgs, input, path, output, (execFrame) -> {
								compiledBody.apply(jsonProvider, execFrame, input, path, output, false);
							});
						} finally {
							fnFrame.getStack().popFrame();
						}
					};
				}
			};
			env.addFunctionFactory(key, envFactory);

			SymbolLocation loc = context.getFunctionLocation(fd.fname(), fd.args().size());
			int slot = loc != null ? loc.slot : 0;
			return new net.thisptr.jackson.jq.v2.core.internal.tree.ResolvedFunctionDefinition(slot, closureSpec, fnSize, fd.args(), paramSlots, compiledBody);
		}

		return (Expression) ast;
	}

	private static <N> Expression compileVariableRef(Environment<N> env, CompileContext context, @Nullable String moduleName, String varName) throws JsonQueryException {
		if (moduleName != null) {
			String fullName = moduleName + "::" + varName;
			if (context.isLocalVariable(fullName)) {
				SymbolLocation loc = context.getVariableLocation(fullName);
				int slot = loc != null ? loc.slot : 0;
				if (loc != null && !loc.isLocal) {
					return new net.thisptr.jackson.jq.v2.core.internal.tree.ResolvedCapturedVariableAccess(fullName, slot);
				}
				return new net.thisptr.jackson.jq.v2.core.internal.tree.ResolvedLocalVariableAccess(fullName, slot);
			}

			@Var Supplier<N> supplier = env.getVariable(fullName);
			if (supplier == null && moduleName.equals(varName)) {
				supplier = env.getVariable(varName);
			}
			if (supplier != null) {
				return new net.thisptr.jackson.jq.v2.core.internal.tree.ResolvedGlobalVariableAccess<>(fullName, supplier);
			}

			throw new JsonQueryException(String.format("Variable $%s::%s is not defined", moduleName, varName));
		}

		if (context.isLocalVariable(varName)) {
			SymbolLocation loc = context.getVariableLocation(varName);
			int slot = loc != null ? loc.slot : 0;
			if (loc != null && !loc.isLocal) {
				return new net.thisptr.jackson.jq.v2.core.internal.tree.ResolvedCapturedVariableAccess(varName, slot);
			}
			return new net.thisptr.jackson.jq.v2.core.internal.tree.ResolvedLocalVariableAccess(varName, slot);
		}

		Supplier<N> supplier = env.getVariable(varName);
		if (supplier != null) {
			return new net.thisptr.jackson.jq.v2.core.internal.tree.ResolvedGlobalVariableAccess<>(varName, supplier);
		}

		throw new JsonQueryException(String.format("Variable $%s is not defined", varName));
	}

	@SuppressWarnings("unchecked")
	private static <N> PatternMatcher<N> compileMatcher(Environment<N> env, CompileContext context, PatternMatcherAst matcher) throws JsonQueryException {
		if (matcher instanceof ValueMatcher) {
			return (ValueMatcher<N>) matcher;
		}
		if (matcher instanceof ArrayMatcherAst) {
			ArrayMatcherAst am = (ArrayMatcherAst) matcher;
			List<PatternMatcher<N>> compiled = new ArrayList<>();
			for (PatternMatcherAst m : am.matchers()) {
				compiled.add(compileMatcher(env, context, m));
			}
			return new ArrayMatcher<>(compiled);
		}
		if (matcher instanceof ObjectMatcherAst) {
			ObjectMatcherAst om = (ObjectMatcherAst) matcher;
			List<ObjectMatcher.FieldMatcher<N>> compiled = new ArrayList<>();
			for (ObjectMatcherAst.FieldMatcher fm : om.matchers()) {
				Expression name = compileNonNull(env, context, fm.name());
				PatternMatcher<N> sub = fm.rawMatcher() != null ? compileMatcher(env, context, fm.rawMatcher()) : null;
				compiled.add(new ObjectMatcher.FieldMatcher<>(fm.dollar(), name, sub));
			}
			return new ObjectMatcher<>(compiled);
		}
		throw new IllegalStateException("Unknown matcher type: " + matcher.getClass());
	}

	public static <N> void bindAndApply(JsonProvider<N> jsonProvider, ExecutionStack<N>.@Nullable Frame callerFrame, ExecutionStack<N>.Frame currentFrame, List<String> paramNames, List<Integer> paramSlots, List<Expression> fnArgs, N in, @Nullable Path<N> path, PathOutput<N> output, Consumer<ExecutionStack<N>.Frame> bodyTask) throws JsonQueryException {
		for (int i = 0; i < paramNames.size(); i++) {
			String pName = paramNames.get(i);
			int slot = paramSlots.get(i);
			Expression pExpr = fnArgs.get(i);
			if (!pName.startsWith("$")) {
				currentFrame.set(slot, new FunctionFactory() {
					@Override
					@SuppressWarnings({"unchecked", "rawtypes"})
					public <N1> Function<N1> createFunction(JsonProvider<N1> jp, List<Expression> emptyArgs, Version v) {
						return (sFrame, inVal, pVal, outVal) -> pExpr.apply(jp, (ExecutionStack.Frame) callerFrame, inVal, pVal, outVal, false);
					}
				});
			}
		}
		bindValueParams(jsonProvider, callerFrame, currentFrame, paramNames, paramSlots, fnArgs, 0, in, path, output, bodyTask);
	}

	private static <N> void bindValueParams(JsonProvider<N> jsonProvider, ExecutionStack<N>.@Nullable Frame callerFrame, ExecutionStack<N>.Frame currentFrame, List<String> paramNames, List<Integer> paramSlots, List<Expression> fnArgs, int index, N in, @Nullable Path<N> path, PathOutput<N> output, Consumer<ExecutionStack<N>.Frame> bodyTask) throws JsonQueryException {
		if (index >= paramNames.size()) {
			bodyTask.accept(currentFrame);
			return;
		}
		String argName = paramNames.get(index);
		Expression argExpr = fnArgs.get(index);
		int slot = paramSlots.get(index);
		if (argName.startsWith("$")) {
			argExpr.apply(jsonProvider, callerFrame, in, path, (val, p) -> {
				currentFrame.set(slot, val);
				bindValueParams(jsonProvider, callerFrame, currentFrame, paramNames, paramSlots, fnArgs, index + 1, in, path, output, bodyTask);
			}, false);
		} else {
			bindValueParams(jsonProvider, callerFrame, currentFrame, paramNames, paramSlots, fnArgs, index + 1, in, path, output, bodyTask);
		}
	}

	private static void collectVariableNames(PatternMatcherAst matcher, Set<String> out) {
		if (matcher instanceof ValueMatcher) {
			out.add(((ValueMatcher<?>) matcher).name());
		} else if (matcher instanceof ArrayMatcherAst) {
			for (PatternMatcherAst m : ((ArrayMatcherAst) matcher).matchers()) {
				collectVariableNames(m, out);
			}
		} else if (matcher instanceof ObjectMatcherAst) {
			for (ObjectMatcherAst.FieldMatcher fm : ((ObjectMatcherAst) matcher).matchers()) {
				if (fm.dollar() && fm.name() instanceof StringLiteral) {
					out.add(((StringLiteral) fm.name()).value());
				}
				if (fm.rawMatcher() != null) {
					collectVariableNames(fm.rawMatcher(), out);
				}
			}
		}
	}

	public static <JsonNode> Expression compileNonNull(Environment<JsonNode> env, CompileContext context, AstNode ast) throws JsonQueryException {
		return compileNonNull(env, context, (Module) null, ast);
	}

	public static <JsonNode> Expression compileNonNull(Environment<JsonNode> env, CompileContext context, @Nullable Module currentModule, AstNode ast) throws JsonQueryException {
		Expression compiled = compile(env, context, currentModule, ast);
		if (compiled == null)
			throw new JsonQueryException("Cannot resolve null expression");
		return compiled;
	}
}
