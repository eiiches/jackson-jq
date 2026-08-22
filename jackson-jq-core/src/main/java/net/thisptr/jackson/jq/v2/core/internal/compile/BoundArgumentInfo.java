package net.thisptr.jackson.jq.v2.core.internal.compile;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.tree.FreeVariables;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.ConstantExpression;
import net.thisptr.jackson.jq.v2.spi.Expression;

/** Compile-time dependency facts and delegate for a bound jq-library parameter. */
public final class BoundArgumentInfo {
	private final Expression<?, ?> expression;
	private final boolean dependsOnInput;
	private final boolean dependsOnExternalState;
	private final boolean dependsOnVariables;
	private final boolean evaluableWithoutFrame;
	private final Cardinality cardinality;

	public BoundArgumentInfo(Expression<?, ?> expression, boolean evaluableWithoutFrame) {
		this.expression = expression;
		this.dependsOnInput = expression.dependsOnInput();
		this.dependsOnExternalState = expression.dependsOnExternalState();
		this.dependsOnVariables = FreeVariables.dependsOnVariables(expression);
		this.evaluableWithoutFrame = evaluableWithoutFrame;
		this.cardinality = expression.getCardinality();
	}

	public Expression<?, ?> expression() {
		return expression;
	}

	public boolean dependsOnInput() {
		return dependsOnInput;
	}

	public boolean dependsOnExternalState() {
		return dependsOnExternalState;
	}

	public boolean dependsOnVariables() {
		return dependsOnVariables;
	}

	public Cardinality getCardinality() {
		return cardinality;
	}

	public boolean isConstant() {
		return !dependsOnInput && !dependsOnExternalState && !dependsOnVariables;
	}

	public boolean isEvaluableWithoutFrame() {
		return isConstant() && evaluableWithoutFrame;
	}

	public @Nullable Expression<?, ?> precomputedExpression() {
		return isEvaluableWithoutFrame() && expression instanceof ConstantExpression<?, ?> ? expression : null;
	}
}
