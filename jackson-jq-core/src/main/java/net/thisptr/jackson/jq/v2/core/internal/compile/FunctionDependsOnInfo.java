package net.thisptr.jackson.jq.v2.core.internal.compile;

import java.util.List;
import java.util.Set;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;

/**
 * Precomputed properties of a local {@code def}'s body, recorded (see
 * {@link CompileContext#recordFunctionDependsOnInfo}) once its {@code ResolvedFunctionDefinition} finishes
 * compiling, and consulted by later call sites in the same/nested scope (see
 * {@link CompileContext#getFunctionLocation}) so they don't have to conservatively assume the worst.
 * Filter arguments contribute dependencies only when the body may invoke their parameters.
 *
 * <p>{@code freeLocalSlots}/{@code hasOpaqueVariableReference} describe which variables invoking this
 * function depends on, numbered relative to the def's own <em>enclosing</em> frame (i.e. the same frame a
 * same-frame/local call site runs in) -- not the def's own body frame.
 */
public record FunctionDependsOnInfo(Cardinality cardinality, boolean dependsOnInput, boolean dependsOnExternalState,
									List<String> parameterNames, Set<String> usedFilterParameters,
									Set<Integer> freeLocalSlots,
									boolean hasOpaqueVariableReference) {

	public ExpressionProperties atCall(List<? extends AnalyzedExpression<?>> arguments) {
		@Var Cardinality resultCardinality = cardinality;
		@Var boolean resultDependsOnInput = dependsOnInput;
		@Var boolean resultDependsOnExternalState = dependsOnExternalState;
		for (int i = 0; i < arguments.size(); i++) {
			String parameter = parameterNames.get(i);
			AnalyzedExpression<?> argument = arguments.get(i);
			if (parameter.startsWith("$")) {
				resultCardinality = CardinalityUtils.multiply(resultCardinality, argument.getCardinality());
			} else if (!usedFilterParameters.contains(parameter)) {
				continue;
			}
			resultDependsOnInput |= argument.dependsOnInput();
			resultDependsOnExternalState |= argument.dependsOnExternalState();
		}
		return new ExpressionProperties(resultCardinality, resultDependsOnInput, resultDependsOnExternalState);
	}

}
