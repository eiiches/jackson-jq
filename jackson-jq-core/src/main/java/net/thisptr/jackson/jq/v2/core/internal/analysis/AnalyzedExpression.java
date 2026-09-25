package net.thisptr.jackson.jq.v2.core.internal.analysis;

import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.ConstantExpression;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;

/**
 * Compiler-private static analysis for executable expressions.
 */
public interface AnalyzedExpression<JsonNode> extends Expression<StackFrame, JsonNode> {
	default List<TypeScheme<FilterType>> getTypeSchemes() {
		return List.of(TypeScheme.of(FilterType.of(AnyType.getInstance(), AnyType.getInstance())));
	}

	default Cardinality getCardinality() {
		return Cardinality.UNKNOWN;
	}

	default boolean dependsOnInput() {
		return true;
	}

	default boolean dependsOnExternalState() {
		return true;
	}

	static ExpressionProperties propertiesOf(Expression<?, ?> expression) {
		if (expression instanceof AnalyzedExpression<?> analyzed) {
			return new ExpressionProperties(analyzed.getCardinality(), analyzed.dependsOnInput(),
					analyzed.dependsOnExternalState());
		}
		if (expression instanceof ConstantExpression<?, ?> constant) {
			int size = constant.getConstantResults().size();
			Cardinality cardinality = size == 0 ? Cardinality.ZERO : size == 1 ? Cardinality.ONE : Cardinality.UNKNOWN;
			return new ExpressionProperties(cardinality, false, false);
		}
		return ExpressionProperties.UNKNOWN;
	}

	static List<TypeScheme<FilterType>> typeSchemesOf(Expression<?, ?> expression) {
		if (expression instanceof AnalyzedExpression<?> analyzed)
			return analyzed.getTypeSchemes();
		return List.of(TypeScheme.of(FilterType.of(AnyType.getInstance(), AnyType.getInstance())));
	}
}
