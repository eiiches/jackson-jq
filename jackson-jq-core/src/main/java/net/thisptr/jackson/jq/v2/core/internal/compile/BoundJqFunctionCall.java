package net.thisptr.jackson.jq.v2.core.internal.compile;

import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.spi.FunctionParameter;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;

/**
 * A call to a jq-source function -- a builtin written in jq, or one a module brought along -- already bound
 * to the arguments at this call site, with the compiled body it will run.
 * <p>
 * Unlike a call to a Java {@code Function}, which stays an {@code UnboundFunctionCall} carrying only the
 * signatures the function publishes, this call has the whole body in hand. That is what lets an analysis
 * answer {@code [1, 2] | map(. + 1)} with the element type the argument actually produces, rather than
 * whatever a single signature covering every call site would have to say.
 *
 * @param <JsonNode> the JSON node type
 */
public interface BoundJqFunctionCall<JsonNode> extends AnalyzedExpression<JsonNode> {
	/**
	 * The name used at the call site, for diagnostics.
	 */
	String name();

	/**
	 * The function's formal parameters, in declaration order.
	 */
	List<FunctionParameter> parameters();

	/**
	 * The overloads the definition publishes, or an empty list when it publishes none. A call carrying
	 * schemes is checked against them instead of against the body, which is how a definition states the
	 * input it is written for rather than leaving that to whatever its body happens to do.
	 */
	List<TypeScheme<FunctionType>> typeSchemes();

	/**
	 * The argument expressions this call site passes, in the same order.
	 */
	List<AnalyzedExpression<JsonNode>> arguments();

	/**
	 * The compiled body, which reads its parameters out of frame slots from {@link #parameterBaseSlot}.
	 */
	AnalyzedExpression<JsonNode> body();

	/**
	 * The frame slot the first parameter occupies; parameter {@code i} is at this slot plus {@code i}.
	 */
	int parameterBaseSlot();
}
