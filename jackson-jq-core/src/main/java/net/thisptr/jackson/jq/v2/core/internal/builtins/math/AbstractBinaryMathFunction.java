package net.thisptr.jackson.jq.v2.core.internal.builtins.math;

import java.util.List;
import java.util.Map;

import net.thisptr.jackson.jq.v2.core.internal.builtins.AbstractPureJsonArgumentFunction;
import net.thisptr.jackson.jq.v2.core.internal.exception.ExceptionMessages;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.type.TypeVariable;
import net.thisptr.jackson.jq.v2.spi.version.Version;

/**
 * A math builtin that takes its two operands as arguments rather than as input, the counterpart of
 * {@link MathFunctions.AbstractMathFunction} for jq's two-argument libm wrappers.
 * <p>
 * jq does not look at the input at all, so neither does this.
 */
public abstract class AbstractBinaryMathFunction extends AbstractPureJsonArgumentFunction {
	private static final TypeVariable INPUT = TypeVariable.of("Input");
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(Map.of(INPUT, AnyType.getInstance()), FunctionType.of(INPUT, NumericType.getInstance(), FilterType.of(INPUT, NumericType.getInstance()), FilterType.of(INPUT, NumericType.getInstance()))));

	@Override
	public List<TypeScheme<FunctionType>> types(Version jqVersion, int totalArguments) {
		return TYPE_SCHEMES;
	}

	@Override
	protected <JsonNode> JsonNode fn(JsonProvider<JsonNode> jsonProvider, Version version, JsonNode in, List<JsonNode> args) throws JsonQueryException {
		for (JsonNode arg : args) {
			if (jsonProvider.getNodeType(arg) != JsonNodeType.NUMBER) {
				// jq 1.5 names the input in the error; jq 1.6 and later name the argument they rejected.
				JsonNode reported = version.compareTo(Versions.JQ_1_6) < 0 ? in : arg;
				throw new JsonQueryTypeException("%s number required", ExceptionMessages.describe(jsonProvider, version, reported));
			}
		}
		return JsonNodeUtils.asNumericNode(jsonProvider, f(jsonProvider.getNumberAsDoubleRounded(args.get(0)), jsonProvider.getNumberAsDoubleRounded(args.get(1))));
	}

	protected abstract double f(double a, double b);
}
