package net.thisptr.jackson.jq.v2.core.internal.compile;

import java.util.List;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.compile.opt.FoldPlanner;
import net.thisptr.jackson.jq.v2.core.internal.compile.resolved.ResolvedFunctionCall;
import net.thisptr.jackson.jq.v2.core.internal.compile.resolved.UnboundFunctionCall;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.tree.RewritableExpression;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

/**
 * Finalizes statically resolved Java calls after type checking.
 */
final class StaticCallFinalizer {
	private StaticCallFinalizer() {
	}

	static <N> AnalyzedExpression<N> run(Environment<N> env, FoldPlanner planner, AnalyzedExpression<N> root)
			throws JsonQueryException {
		try {
			return finalizeExpression(env, planner, root);
		} catch (FinalizationException e) {
			throw (JsonQueryException) e.getCause();
		}
	}

	private static <N> AnalyzedExpression<N> finalizeExpression(Environment<N> env, FoldPlanner planner,
																AnalyzedExpression<N> expression) throws JsonQueryException {
		if (expression instanceof UnboundFunctionCall<N> call) {
			List<AnalyzedExpression<N>> arguments = call.args().stream()
					.map(argument -> finalizeUnchecked(env, planner, argument))
					.map(argument -> planner.optimizeExpression(env, argument))
					.toList();
			List<ExpressionProperties> argumentProperties = arguments.stream()
					.map(AnalyzedExpression::propertiesOf)
					.toList();
			ExpressionProperties properties = call.factory().analyze(call.bindContext().getJqVersion(), argumentProperties);
			List<Expression<StackFrame, N>> executableArguments = List.copyOf(arguments);
			Expression<StackFrame, N> function =
					call.factory().bind(call.bindContext(), executableArguments);
			AnalyzedExpression<N> resolved = new ResolvedFunctionCall<>(function, properties, arguments);
			planner.transferMetadata(call, resolved);
			return resolved;
		}
		if (!(expression instanceof RewritableExpression<?> rewritableExpression))
			return expression;
		@SuppressWarnings("unchecked")
		RewritableExpression<N> rewritable = (RewritableExpression<N>) rewritableExpression;
		AnalyzedExpression<N> rewritten = rewritable.rewriteChildren(child -> finalizeUnchecked(env, planner, child));
		planner.transferMetadata(expression, rewritten);
		return rewritten;
	}

	private static <N> AnalyzedExpression<N> finalizeUnchecked(Environment<N> env, FoldPlanner planner,
															   AnalyzedExpression<N> expression) {
		try {
			return finalizeExpression(env, planner, expression);
		} catch (JsonQueryException e) {
			throw new FinalizationException(e);
		}
	}

	private static final class FinalizationException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		FinalizationException(JsonQueryException cause) {
			super(cause);
		}
	}
}
