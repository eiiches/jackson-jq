package net.thisptr.jackson.jq.v2.ext.binary;

import java.util.ArrayList;
import java.util.List;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.RuntimeLimits;
import net.thisptr.jackson.jq.v2.spi.exception.RuntimeLimitExceededException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.BinaryType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.NumberKind;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.version.Version;

final class ToBytesFunction implements Function {
	private static final String FUNCTION = "binary::to_bytes";
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(FunctionType.of(BinaryType.getInstance(), ArrayType.of(NumericType.of(NumberKind.INT)))));

	@Override
	public List<TypeScheme<FunctionType>> types(Version jqVersion, int totalArguments) {
		return totalArguments == 0 ? TYPE_SCHEMES : List.of();
	}

	@Override
	public ExpressionProperties analyze(Version jqVersion, List<ExpressionProperties> arguments) {
		return new ExpressionProperties(Cardinality.ONE, true, false);
	}

	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindContext, List<Expression<Context, JsonNode>> arguments) {
		JsonProvider<JsonNode> jsonProvider = bindContext.getJsonProvider();
		return (context, input, inputPath, output) ->
				output.emit(toBytes(jsonProvider, BinarySupport.getInputBytes(jsonProvider, FUNCTION, input), context.getRuntimeLimits()), UntrackedPath.getInstance());
	}

	private static <JsonNode> JsonNode toBytes(JsonProvider<JsonNode> jsonProvider, byte[] bytes, RuntimeLimits limits) {
		int maximum = limits.getMaxArrayLength();
		if (bytes.length > maximum)
			throw new RuntimeLimitExceededException("Array of " + bytes.length + " elements exceeds the maximum array length of " + maximum);
		List<JsonNode> values = new ArrayList<>(bytes.length);
		for (byte value : bytes)
			values.add(jsonProvider.createNumber(value & 0xff));
		return jsonProvider.createArray(values);
	}
}
