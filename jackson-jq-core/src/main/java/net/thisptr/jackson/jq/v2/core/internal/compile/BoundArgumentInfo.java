package net.thisptr.jackson.jq.v2.core.internal.compile;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;

/**
 * Compile-time dependency facts and delegate for a bound jq-library parameter.
 */
public final class BoundArgumentInfo {
	private final AnalyzedExpression<?> expression;
	private final boolean dependsOnInput;
	private final boolean dependsOnExternalState;
	private final boolean dependsOnVariables;
	private final boolean evaluableWithoutFrame;
	private final Cardinality cardinality;

	public BoundArgumentInfo(AnalyzedExpression<?> expression, boolean evaluableWithoutFrame) {
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

	/**
	 * The caller's own argument, to compile a reference to this parameter as, or {@code null} when the
	 * parameter has to be read out of a frame slot at run time.
	 * <p>
	 * Being evaluable without a frame is the whole requirement: there is then nothing a slot could hold
	 * that the expression does not already carry, and because such an argument depends on neither the
	 * input nor any variable, evaluating it at the reference site rather than the call site means the
	 * same thing.
	 * <p>
	 * It need not have folded. Folding runs after type checking, so at this point nothing has, and the
	 * body's references are what the rewrite later folds -- every reference compiles as this one
	 * expression, so folding it replaces all of them at once.
	 */
	public @Nullable Expression<?, ?> precomputedExpression() {
		return isEvaluableWithoutFrame() ? expression : null;
	}
}
