package net.thisptr.jackson.jq.v2.core.internal.misc;

import net.thisptr.jackson.jq.v2.core.internal.tree.FreeVariables;
import net.thisptr.jackson.jq.v2.spi.Expression;

public class ExpressionUtils {

	/**
	 * True if the given expression always evaluates to the same result(s), independent of its
	 * input, external state, and free variables. Safe to evaluate once ahead of time via
	 * {@code apply(null, jsonProvider.createNull(), null, output)} to get the constant value(s).
	 */
	public static boolean isConstantExpression(Expression<?, ?> expr) {
		return !expr.dependsOnInput() && !expr.dependsOnExternalState() && !FreeVariables.dependsOnVariables(expr);
	}
}
