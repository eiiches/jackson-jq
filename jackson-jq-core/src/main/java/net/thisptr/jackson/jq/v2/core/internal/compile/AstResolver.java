package net.thisptr.jackson.jq.v2.core.internal.compile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.internal.misc.Pair;
import net.thisptr.jackson.jq.v2.core.internal.tree.ArrayConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.AssignPipeComponent;
import net.thisptr.jackson.jq.v2.core.internal.tree.Conditional;
import net.thisptr.jackson.jq.v2.core.internal.tree.FieldConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.ForeachExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.FormattingFilter;
import net.thisptr.jackson.jq.v2.core.internal.tree.FunctionCall;
import net.thisptr.jackson.jq.v2.core.internal.tree.FunctionDefinition;
import net.thisptr.jackson.jq.v2.core.internal.tree.IdentifierKeyFieldConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.ImportStatement;
import net.thisptr.jackson.jq.v2.core.internal.tree.JsonQueryKeyFieldConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.NegativeExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.ObjectConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.PipeComponent;
import net.thisptr.jackson.jq.v2.core.internal.tree.PipedQuery;
import net.thisptr.jackson.jq.v2.core.internal.tree.ReduceExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.ResolvedCapturedFunctionAccess;
import net.thisptr.jackson.jq.v2.core.internal.tree.ResolvedCapturedVariableAccess;
import net.thisptr.jackson.jq.v2.core.internal.tree.ResolvedFunctionCall;
import net.thisptr.jackson.jq.v2.core.internal.tree.ResolvedGlobalVariableAccess;
import net.thisptr.jackson.jq.v2.core.internal.tree.ResolvedLocalFunctionAccess;
import net.thisptr.jackson.jq.v2.core.internal.tree.ResolvedLocalVariableAccess;
import net.thisptr.jackson.jq.v2.core.internal.tree.SemicolonOperator;
import net.thisptr.jackson.jq.v2.core.internal.tree.StringKeyFieldConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.TopLevelExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.TransformPipeComponent;
import net.thisptr.jackson.jq.v2.core.internal.tree.TryCatch;
import net.thisptr.jackson.jq.v2.core.internal.tree.Tuple;
import net.thisptr.jackson.jq.v2.core.internal.tree.VariableAccess;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.BinaryOperatorExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess.BracketExtractFieldAccess;
import net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess.BracketFieldAccess;
import net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess.IdentifierFieldAccess;
import net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess.StringFieldAccess;
import net.thisptr.jackson.jq.v2.core.internal.tree.literal.StringLiteral;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.matchers.ArrayMatcher;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.matchers.ObjectMatcher;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.matchers.ValueMatcher;
import net.thisptr.jackson.jq.v2.spi.Closure;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.FunctionNameAndArity;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.Module;

public class AstResolver {

	public static <JsonNode> Expression resolve(Environment<JsonNode> env, Expression expr) throws JsonQueryException {
		CompileContext context = new CompileContext();
		Expression resolved = resolve(env, context, expr);
		if (resolved == null)
			throw new JsonQueryException("Cannot resolve null expression");
		return new net.thisptr.jackson.jq.v2.core.internal.tree.RootExpression<>(context.getSlotCount(), resolved);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static <JsonNode> @Nullable Expression resolve(Environment<JsonNode> env, CompileContext context, @Nullable Expression expr) throws JsonQueryException {
		if (expr == null)
			return null;

		if (expr instanceof FunctionCall) {
			FunctionCall call = (FunctionCall) expr;
			List<Expression> compiledArgs = new ArrayList<>();
			for (Expression arg : call.args()) {
				compiledArgs.add(resolve(env, context, arg));
			}

			String fullName = call.moduleName() != null ? call.moduleName() + "::" + call.name() : call.name();
			if (call.moduleName() == null && context.isLocalFunction(call.name(), compiledArgs.size())) {
				SymbolLocation loc = context.getFunctionLocation(call.name(), compiledArgs.size());
				int slot = loc != null ? loc.slot : 0;
				if (loc != null && !loc.isLocal) {
					return new ResolvedCapturedFunctionAccess(call.name(), slot, compiledArgs);
				}
				return new ResolvedLocalFunctionAccess(call.name(), slot, compiledArgs);
			}

			FunctionNameAndArity key = FunctionNameAndArity.of(fullName, compiledArgs.size());
			FunctionFactory factory = env.getFunctionFactory(key);
			if (factory == null) {
				throw new JsonQueryException(String.format("Function %s/%d does not exist", fullName, compiledArgs.size()));
			}

			Function<JsonNode> fn = factory.createFunction(env.jsonProvider(), compiledArgs, env.version());
			return new ResolvedFunctionCall<>(fullName, fn);
		}

		if (expr instanceof VariableAccess) {
			VariableAccess varAccess = (VariableAccess) expr;
			String varName = varAccess.name();

			if (context.isLocalVariable(varName)) {
				SymbolLocation loc = context.getVariableLocation(varName);
				int slot = loc != null ? loc.slot : 0;
				if (loc != null && !loc.isLocal) {
					return new ResolvedCapturedVariableAccess(varName, slot);
				}
				return new ResolvedLocalVariableAccess(varName, slot);
			}

			Supplier<JsonNode> supplier = env.getVariable(varName);
			if (supplier != null) {
				return new ResolvedGlobalVariableAccess<>(varName, supplier);
			}

			throw new JsonQueryException(String.format("Variable $%s is not defined", varName));
		}

		if (expr instanceof TopLevelExpression) {
			TopLevelExpression<JsonNode> top = (TopLevelExpression<JsonNode>) expr;
			for (ImportStatement<JsonNode> imp : top.imports()) {
				if (env.getModuleLoader() == null) {
					throw new JsonQueryException(String.format("module not found: %s", imp.path));
				}
				JsonNode metadata = imp.getMetadata(env.jsonProvider());
				if (imp.dollarImport) {
					JsonNode data = env.getModuleLoader().loadData(env.rootScope().getCurrentModule(), imp.path, metadata);
					if (data == null) {
						throw new JsonQueryException(String.format("module not found: %s", imp.path));
					}
					if (imp.name != null) {
						env.addVariable(imp.name, data);
					}
				} else {
					Module mod = env.getModuleLoader().loadModule(env.rootScope().getCurrentModule(), imp.path, metadata);
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
			Expression resolvedInner = resolveNonNull(env, context, top.expr());
			return new TopLevelExpression<>(top.moduleDirective(), Collections.emptyList(), resolvedInner);
		}

		if (expr instanceof PipedQuery) {
			PipedQuery<JsonNode> piped = (PipedQuery<JsonNode>) expr;
			List<PipeComponent<JsonNode>> newComponents = new ArrayList<>();

			@Var int pushedScopes = 0;
			try {
				for (PipeComponent<JsonNode> comp : piped.components()) {
					if (comp instanceof AssignPipeComponent) {
						AssignPipeComponent<JsonNode> assign = (AssignPipeComponent<JsonNode>) comp;
						Expression resolvedExpr = resolveNonNull(env, context, assign.expr);

						context.pushLocalScope();
						pushedScopes++;

						Set<String> varNames = new HashSet<>();
						collectVariableNames(assign.matcher, varNames);
						java.util.Map<String, Integer> slots = new java.util.HashMap<>();
						for (String varName : varNames) {
							context.addLocalVariable(varName);
							slots.put(varName, context.getSlot(varName));
						}

						newComponents.add(new AssignPipeComponent<>(resolvedExpr, assign.matcher, slots));
					} else if (comp instanceof TransformPipeComponent) {
						TransformPipeComponent<JsonNode> transform = (TransformPipeComponent<JsonNode>) comp;
						Expression resolvedExpr = resolveNonNull(env, context, transform.expr);
						newComponents.add(new TransformPipeComponent<>(resolvedExpr));
					} else {
						newComponents.add(comp);
					}
				}
			} finally {
				for (int i = 0; i < pushedScopes; i++) {
					context.popScope();
				}
			}

			return new PipedQuery<>(newComponents);
		}

		if (expr instanceof SemicolonOperator) {
			SemicolonOperator semi = (SemicolonOperator) expr;
			List<Expression> newExpressions = new ArrayList<>();
			for (Expression q : semi.expressions()) {
				newExpressions.add(resolveNonNull(env, context, q));
			}
			return new SemicolonOperator(newExpressions);
		}

		if (expr instanceof ObjectConstruction) {
			ObjectConstruction<JsonNode> obj = (ObjectConstruction<JsonNode>) expr;
			ObjectConstruction<JsonNode> res = new ObjectConstruction<>();
			for (FieldConstruction<JsonNode> fc : obj.fields) {
				if (fc instanceof IdentifierKeyFieldConstruction) {
					IdentifierKeyFieldConstruction<JsonNode> ik = (IdentifierKeyFieldConstruction<JsonNode>) fc;
					Expression val = resolve(env, context, ik.value);
					res.add(new IdentifierKeyFieldConstruction<>(ik.key, val));
				} else if (fc instanceof JsonQueryKeyFieldConstruction) {
					JsonQueryKeyFieldConstruction<JsonNode> jq = (JsonQueryKeyFieldConstruction<JsonNode>) fc;
					Expression key = resolveNonNull(env, context, jq.key());
					Expression val = resolveNonNull(env, context, jq.value());
					res.add(new JsonQueryKeyFieldConstruction<>(key, val));
				} else if (fc instanceof StringKeyFieldConstruction) {
					StringKeyFieldConstruction<JsonNode> sk = (StringKeyFieldConstruction<JsonNode>) fc;
					Expression key = resolveNonNull(env, context, sk.key);
					Expression val = resolve(env, context, sk.value);
					res.add(new StringKeyFieldConstruction<>(key, val));
				} else if (fc instanceof net.thisptr.jackson.jq.v2.core.internal.tree.VariableKeyFieldConstruction) {
					net.thisptr.jackson.jq.v2.core.internal.tree.VariableKeyFieldConstruction<JsonNode> vk = (net.thisptr.jackson.jq.v2.core.internal.tree.VariableKeyFieldConstruction<JsonNode>) fc;
					SymbolLocation loc = context.getVariableLocation(vk.name());
					boolean isLocal = loc != null ? loc.isLocal : true;
					int slot = loc != null ? loc.slot : 0;
					res.add(new net.thisptr.jackson.jq.v2.core.internal.tree.ResolvedVariableKeyFieldConstruction<>(vk.name(), isLocal, slot));
				} else {
					res.add(fc);
				}
			}
			return res;
		}

		if (expr instanceof ArrayConstruction) {
			ArrayConstruction arr = (ArrayConstruction) expr;
			return new ArrayConstruction(resolve(env, context, arr.q));
		}

		if (expr instanceof BinaryOperatorExpression) {
			BinaryOperatorExpression bin = (BinaryOperatorExpression) expr;
			bin.lhs(resolveNonNull(env, context, bin.lhs()));
			bin.rhs(resolveNonNull(env, context, bin.rhs()));
			return bin;
		}

		if (expr instanceof NegativeExpression) {
			NegativeExpression neg = (NegativeExpression) expr;
			return new NegativeExpression(resolveNonNull(env, context, neg.value()));
		}

		if (expr instanceof Conditional) {
			Conditional cond = (Conditional) expr;
			List<Pair<Expression, Expression>> newSwitches = new ArrayList<>();
			for (Pair<Expression, Expression> sw : cond.switches()) {
				Expression newIf = resolveNonNull(env, context, sw._1);
				Expression newThen = resolveNonNull(env, context, sw._2);
				newSwitches.add(Pair.of(newIf, newThen));
			}
			Expression newElse = resolveNonNull(env, context, cond.otherwise());
			return new Conditional(newSwitches, newElse);
		}

		if (expr instanceof TryCatch) {
			TryCatch tc = (TryCatch) expr;
			Expression newTry = resolveNonNull(env, context, tc.tryExpr());
			Expression newCatch = resolve(env, context, tc.catchExpr());
			if (tc instanceof TryCatch.Question) {
				return new TryCatch.Question(newTry);
			}
			return new TryCatch(newTry, newCatch);
		}

		if (expr instanceof Tuple) {
			Tuple tuple = (Tuple) expr;
			List<Expression> newQs = new ArrayList<>();
			for (Expression q : tuple.qs) {
				newQs.add(resolveNonNull(env, context, q));
			}
			return new Tuple(newQs);
		}

		if (expr instanceof ReduceExpression) {
			ReduceExpression<JsonNode> red = (ReduceExpression<JsonNode>) expr;
			Expression resolvedIter = resolveNonNull(env, context, red.iterExpr());
			Expression resolvedInit = resolveNonNull(env, context, red.initExpr());

			Set<String> varNames = new HashSet<>();
			collectVariableNames(red.matcher(), varNames);
			Map<String, Integer> slots = new HashMap<>();
			context.pushLocalScope();
			try {
				for (String varName : varNames) {
					context.addLocalVariable(varName);
					slots.put(varName, context.getSlot(varName));
				}
				Expression resolvedReduce = resolveNonNull(env, context, red.reduceExpr());
				return new ReduceExpression<>(red.matcher(), resolvedInit, resolvedReduce, resolvedIter, slots);
			} finally {
				context.popScope();
			}
		}

		if (expr instanceof ForeachExpression) {
			ForeachExpression<JsonNode> fe = (ForeachExpression<JsonNode>) expr;
			Expression resolvedIter = resolveNonNull(env, context, fe.iterExpr());
			Expression resolvedInit = resolveNonNull(env, context, fe.initExpr());

			Set<String> varNames = new HashSet<>();
			collectVariableNames(fe.matcher(), varNames);
			Map<String, Integer> slots = new HashMap<>();
			context.pushLocalScope();
			try {
				for (String varName : varNames) {
					context.addLocalVariable(varName);
					slots.put(varName, context.getSlot(varName));
				}
				Expression resolvedUpdate = resolveNonNull(env, context, fe.updateExpr());
				Expression resolvedExtract = fe.extractExpr() != null ? resolve(env, context, fe.extractExpr()) : null;
				return new ForeachExpression<>(fe.matcher(), resolvedInit, resolvedUpdate, resolvedExtract, resolvedIter, slots);
			} finally {
				context.popScope();
			}
		}

		if (expr instanceof FormattingFilter) {
			FormattingFilter ff = (FormattingFilter) expr;
			String fname = ff.name().startsWith("@") ? ff.name() : "@" + ff.name();
			FunctionNameAndArity key = FunctionNameAndArity.of(fname, 0);
			FunctionFactory factory = env.getFunctionFactory(key);
			if (factory == null) {
				throw new JsonQueryException(String.format("Formatting operator %s does not exist", fname));
			}
			Function<JsonNode> fn = factory.createFunction(env.jsonProvider(), Collections.emptyList(), env.version());
			return new ResolvedFunctionCall<>(fname, fn);
		}

		if (expr instanceof net.thisptr.jackson.jq.v2.core.internal.tree.StringInterpolation) {
			net.thisptr.jackson.jq.v2.core.internal.tree.StringInterpolation si = (net.thisptr.jackson.jq.v2.core.internal.tree.StringInterpolation) expr;
			List<Pair<Integer, Expression>> resolvedInterpolations = new ArrayList<>();
			for (Pair<Integer, Expression> pair : si.interpolations()) {
				Expression resExpr = resolveNonNull(env, context, pair._2);
				resolvedInterpolations.add(Pair.of(pair._1, resExpr));
			}
			Expression resolvedFormatter = resolve(env, context, si.formatter());
			return new net.thisptr.jackson.jq.v2.core.internal.tree.StringInterpolation(si.template(), resolvedInterpolations, resolvedFormatter);
		}

		if (expr instanceof BracketFieldAccess) {
			BracketFieldAccess bfa = (BracketFieldAccess) expr;
			Expression target = resolveNonNull(env, context, bfa.target());
			@Var Expression start = resolve(env, context, bfa.startExpr());
			@Var Expression end = resolve(env, context, bfa.endExpr());
			if (start == null)
				start = new net.thisptr.jackson.jq.v2.core.internal.tree.literal.NullLiteral();
			if (end == null)
				end = new net.thisptr.jackson.jq.v2.core.internal.tree.literal.NullLiteral();
			if (bfa.isRange()) {
				return new BracketFieldAccess(target, start, end, bfa.permissive());
			} else {
				return new BracketFieldAccess(target, start, bfa.permissive());
			}
		}

		if (expr instanceof IdentifierFieldAccess) {
			IdentifierFieldAccess ifa = (IdentifierFieldAccess) expr;
			Expression target = resolveNonNull(env, context, ifa.target());
			return new IdentifierFieldAccess(target, ifa.field(), ifa.permissive());
		}

		if (expr instanceof StringFieldAccess) {
			StringFieldAccess sfa = (StringFieldAccess) expr;
			Expression target = resolveNonNull(env, context, sfa.target());
			Expression key = resolveNonNull(env, context, sfa.key());
			return new StringFieldAccess(target, key, sfa.permissive());
		}

		if (expr instanceof BracketExtractFieldAccess) {
			BracketExtractFieldAccess befa = (BracketExtractFieldAccess) expr;
			Expression target = resolveNonNull(env, context, befa.target());
			return new BracketExtractFieldAccess(target, befa.permissive());
		}

		if (expr instanceof FunctionDefinition) {
			FunctionDefinition fd = (FunctionDefinition) expr;
			FunctionNameAndArity key = FunctionNameAndArity.of(fd.fname(), fd.args().size());
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
			Expression resolvedBody = resolveNonNull(env, fnContext, fd.body());
			ClosureSpec closureSpec = fnContext.getClosureSpec();

			FunctionFactory envFactory = new FunctionFactory() {
				@Override
				public <N> Function<N> createFunction(net.thisptr.jackson.jq.v2.json.JsonProvider<N> jsonProvider, List<Expression> fnArgs, net.thisptr.jackson.jq.v2.spi.Version version) {
					return createFunction(jsonProvider, (Closure<N>) null, fnArgs, version);
				}

				@Override
				@SuppressWarnings({"unchecked", "rawtypes"})
				public <N> Function<N> createFunction(net.thisptr.jackson.jq.v2.json.JsonProvider<N> jsonProvider, @Nullable Closure<N> callingClosure, List<Expression> fnArgs, net.thisptr.jackson.jq.v2.spi.Version version) {
					return (runtimeScope, input, path, output) -> {
						ExecutionStack<N>.Frame parentFrame = runtimeScope.getExecutionFrame();
						ExecutionStack<N>.Frame fnFrame = parentFrame != null
								? parentFrame.getStack().pushFrame(parentFrame, fnSize)
								: new ExecutionStack<N>().pushFrame(parentFrame, fnSize);
						fnFrame.setClosure((Closure) callingClosure);
						Scope<N> fnScope = Scope.newChildScopeWithFrame(runtimeScope, fnFrame);
						try {
							bindAndApply(runtimeScope, fnScope, fd.args(), paramSlots, fnArgs, input, path, output, (execScope) -> {
								resolvedBody.apply(execScope, input, path, output, false);
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
			return new net.thisptr.jackson.jq.v2.core.internal.tree.ResolvedFunctionDefinition(slot, closureSpec, fnSize, fd.args(), paramSlots, resolvedBody);
		}

		return expr;
	}

	public static <N> void bindAndApply(Scope<N> callerScope, Scope<N> currentScope, List<String> paramNames, List<Integer> paramSlots, List<Expression> fnArgs, N in, net.thisptr.jackson.jq.v2.spi.path.@org.jspecify.annotations.Nullable Path<N> path, net.thisptr.jackson.jq.v2.spi.PathOutput<N> output, java.util.function.Consumer<Scope<N>> bodyTask) throws JsonQueryException {
		for (int i = 0; i < paramNames.size(); i++) {
			String pName = paramNames.get(i);
			int slot = paramSlots.get(i);
			Expression pExpr = fnArgs.get(i);
			if (!pName.startsWith("$")) {
				currentScope.setFunctionFactory(slot, new FunctionFactory() {
					@Override
					@SuppressWarnings({"unchecked", "rawtypes"})
					public <N1> Function<N1> createFunction(net.thisptr.jackson.jq.v2.json.JsonProvider<N1> jp, List<Expression> emptyArgs, net.thisptr.jackson.jq.v2.spi.Version v) {
						return (s, inVal, pVal, outVal) -> pExpr.apply((Scope) callerScope, inVal, pVal, outVal, false);
					}
				});
			}
		}
		bindValueParams(callerScope, currentScope, paramNames, paramSlots, fnArgs, 0, in, path, output, bodyTask);
	}

	private static <N> void bindValueParams(Scope<N> callerScope, Scope<N> currentScope, List<String> paramNames, List<Integer> paramSlots, List<Expression> fnArgs, int index, N in, net.thisptr.jackson.jq.v2.spi.path.@org.jspecify.annotations.Nullable Path<N> path, net.thisptr.jackson.jq.v2.spi.PathOutput<N> output, java.util.function.Consumer<Scope<N>> bodyTask) throws JsonQueryException {
		if (index >= paramNames.size()) {
			bodyTask.accept(currentScope);
			return;
		}
		String argName = paramNames.get(index);
		Expression argExpr = fnArgs.get(index);
		int slot = paramSlots.get(index);
		if (argName.startsWith("$")) {
			argExpr.apply(callerScope, in, path, (val, p) -> {
				currentScope.setValue(slot, val);
				bindValueParams(callerScope, currentScope, paramNames, paramSlots, fnArgs, index + 1, in, path, output, bodyTask);
			}, false);
		} else {
			bindValueParams(callerScope, currentScope, paramNames, paramSlots, fnArgs, index + 1, in, path, output, bodyTask);
		}
	}

	private static void collectVariableNames(PatternMatcher<?> matcher, Set<String> out) {
		if (matcher instanceof ValueMatcher) {
			out.add(((ValueMatcher<?>) matcher).name());
		} else if (matcher instanceof ArrayMatcher) {
			for (PatternMatcher<?> m : ((ArrayMatcher<?>) matcher).matchers()) {
				collectVariableNames(m, out);
			}
		} else if (matcher instanceof ObjectMatcher) {
			for (ObjectMatcher.FieldMatcher<?> fm : ((ObjectMatcher<?>) matcher).matchers()) {
				if (fm.dollar() && fm.name() instanceof StringLiteral) {
					out.add(((StringLiteral) fm.name()).value());
				}
				if (fm.rawMatcher() != null) {
					collectVariableNames(fm.rawMatcher(), out);
				}
			}
		}
	}

	public static <JsonNode> Expression resolveNonNull(Environment<JsonNode> env, CompileContext context, Expression expr) throws JsonQueryException {
		Expression resolved = resolve(env, context, expr);
		if (resolved == null)
			throw new JsonQueryException("Cannot resolve null expression");
		return resolved;
	}
}
