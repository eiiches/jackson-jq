package net.thisptr.jackson.jq.v2.core.internal.compile;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.internal.tree.AssignPipeComponent;
import net.thisptr.jackson.jq.v2.core.internal.tree.FunctionCall;
import net.thisptr.jackson.jq.v2.core.internal.tree.PipeComponent;
import net.thisptr.jackson.jq.v2.core.internal.tree.PipedQuery;
import net.thisptr.jackson.jq.v2.core.internal.tree.ResolvedFunctionCall;
import net.thisptr.jackson.jq.v2.core.internal.tree.ResolvedGlobalVariableAccess;
import net.thisptr.jackson.jq.v2.core.internal.tree.ResolvedLocalVariableAccess;
import net.thisptr.jackson.jq.v2.core.internal.tree.SemicolonOperator;
import net.thisptr.jackson.jq.v2.core.internal.tree.TopLevelExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.TransformPipeComponent;
import net.thisptr.jackson.jq.v2.core.internal.tree.VariableAccess;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.FunctionNameAndArity;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class AstResolver {

	public static <JsonNode> Expression resolve(Environment<JsonNode> env, Expression expr) throws JsonQueryException {
		CompileContext context = new CompileContext();
		Expression resolved = resolve(env, context, expr);
		if (resolved == null)
			throw new JsonQueryException("Cannot resolve null expression");
		return resolved;
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

			FunctionNameAndArity key = FunctionNameAndArity.of(call.name(), compiledArgs.size());
			FunctionFactory factory = env.getFunctionFactory(key);
			if (factory == null) {
				throw new JsonQueryException(String.format("Function %s/%d does not exist", call.name(), compiledArgs.size()));
			}

			Function fn = factory.createFunction(compiledArgs, env.version());
			return new ResolvedFunctionCall(call.name(), fn);
		}

		if (expr instanceof VariableAccess) {
			VariableAccess varAccess = (VariableAccess) expr;
			String varName = varAccess.name();

			if (context.isLocalVariable(varName)) {
				return new ResolvedLocalVariableAccess(varName);
			}

			Supplier<JsonNode> supplier = env.getVariable(varName);
			if (supplier != null) {
				return new ResolvedGlobalVariableAccess<>(varName, supplier);
			}

			throw new JsonQueryException(String.format("Variable $%s is not defined", varName));
		}

		if (expr instanceof TopLevelExpression) {
			TopLevelExpression<JsonNode> top = (TopLevelExpression<JsonNode>) expr;
			Expression resolvedInner = resolveNonNull(env, context, top.expr());
			return new TopLevelExpression<>(top.moduleDirective(), top.imports(), resolvedInner);
		}

		if (expr instanceof PipedQuery) {
			PipedQuery<JsonNode> piped = (PipedQuery<JsonNode>) expr;
			List<PipeComponent<JsonNode>> newComponents = new ArrayList<>();

			context.pushScope();
			try {
				for (PipeComponent<JsonNode> comp : piped.components()) {
					if (comp instanceof AssignPipeComponent) {
						AssignPipeComponent<JsonNode> assign = (AssignPipeComponent<JsonNode>) comp;
						Expression resolvedExpr = resolveNonNull(env, context, assign.expr);

						PatternMatcher<JsonNode> matcher = assign.matcher;
						String matcherStr = matcher.toString();
						if (matcherStr.startsWith("$")) {
							context.addLocalVariable(matcherStr.substring(1));
						}

						newComponents.add(new AssignPipeComponent<>(resolvedExpr, matcher));
					} else if (comp instanceof TransformPipeComponent) {
						TransformPipeComponent<JsonNode> transform = (TransformPipeComponent<JsonNode>) comp;
						Expression resolvedExpr = resolveNonNull(env, context, transform.expr);
						newComponents.add(new TransformPipeComponent<>(resolvedExpr));
					} else {
						newComponents.add(comp);
					}
				}
			} finally {
				context.popScope();
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

		return expr;
	}

	private static <JsonNode> Expression resolveNonNull(Environment<JsonNode> env, CompileContext context, Expression expr) throws JsonQueryException {
		Expression resolved = resolve(env, context, expr);
		if (resolved == null)
			throw new JsonQueryException("Cannot resolve null expression");
		return resolved;
	}
}
