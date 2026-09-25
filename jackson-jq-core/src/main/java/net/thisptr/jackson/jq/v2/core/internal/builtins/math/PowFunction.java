package net.thisptr.jackson.jq.v2.core.internal.builtins.math;

import java.util.List;
import java.util.Map;

import net.thisptr.jackson.jq.v2.core.internal.builtins.AbstractPureJsonArgumentFunction;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.Preconditions;
import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.type.TypeVariable;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@FunctionRegistration(name = "pow", nargs = 2)
public class PowFunction extends AbstractPureJsonArgumentFunction {
	private static final TypeVariable INPUT = TypeVariable.of("Input");
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(Map.of(INPUT, AnyType.getInstance()), FunctionType.of(INPUT, NumericType.getInstance(), FilterType.of(INPUT, NumericType.getInstance()), FilterType.of(INPUT, NumericType.getInstance()))));

	@Override
	public List<TypeScheme<FunctionType>> types(Version jqVersion, int totalArguments) {
		return TYPE_SCHEMES;
	}

	@Override
	protected <JsonNode> JsonNode fn(JsonProvider<JsonNode> jsonProvider, List<JsonNode> args) throws JsonQueryException {
		Preconditions.checkArgumentType(jsonProvider, "pow/2", 0, args.get(0), JsonNodeType.NUMBER);
		Preconditions.checkArgumentType(jsonProvider, "pow/2", 1, args.get(1), JsonNodeType.NUMBER);
		return JsonNodeUtils.asNumericNode(jsonProvider, Math.pow(jsonProvider.getNumberAsDoubleRounded(args.get(0)), jsonProvider.getNumberAsDoubleRounded(args.get(1))));
	}
}
