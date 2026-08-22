package net.thisptr.jackson.jq.v2.spi;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public interface Expression<JsonNode> {

	// TODO: Replace with 'boolean isConstantExpression()' and update the compiler to recursively set the flag.
	//       For an expression to be "constant", it must meet these requirements:
	//        1. the expression doesn't use inputs. any input returns the same value(s).
	//        2. any function used in the expression is pure, i.e.,
	//           (i) returns the same value(s) for the same arguments (no random or time-based values, etc.)
	//           (ii) doesn't use the inputs
	//       Functions probably need default boolean isPure() { return false; }.
	//       Anyone who needs the pre-evaluated constant value should call apply() with NullNode and a null Path.
	default @Nullable JsonNode evaluateConstantExpr() {
		return null;
	}

	void apply(@Nullable StackFrame frame, JsonNode in, @Nullable Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException;
}
