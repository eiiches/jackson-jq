package net.thisptr.jackson.jq.v2.ext.binary;

import java.util.Iterator;
import java.util.List;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.BinaryType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.NumberKind;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.version.Version;

final class FromBytesFunction implements Function {
	private static final String FUNCTION = "binary::from_bytes";
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(FunctionType.of(ArrayType.of(NumericType.of(NumberKind.INT)), BinaryType.getInstance())));

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
		boolean binarySupported = BinarySupport.supportsBinary(jsonProvider);
		return (context, input, inputPath, output) ->
				output.emit(BinarySupport.createBinaryValue(jsonProvider, fromBytes(jsonProvider, input), binarySupported, context.getRuntimeLimits()), UntrackedPath.getInstance());
	}

	private static <JsonNode> byte[] fromBytes(JsonProvider<JsonNode> jsonProvider, JsonNode input) {
		JsonNodeType type = jsonProvider.getNodeType(input);
		if (type != JsonNodeType.ARRAY)
			throw new JsonQueryException(FUNCTION + " requires an array of byte integers, but got " + type);
		byte[] bytes = new byte[jsonProvider.getArrayLength(input)];
		Iterator<JsonNode> values = jsonProvider.getArrayElements(input);
		for (int i = 0; values.hasNext(); i++) {
			JsonNode value = values.next();
			Integer number = jsonProvider.isNumber(value) ? jsonProvider.getNumberAsIntExact(value) : null;
			if (number == null || number < 0 || number > 255)
				throw new JsonQueryException(FUNCTION + " element " + i + " must be an integer from 0 to 255");
			bytes[i] = number.byteValue();
		}
		return bytes;
	}
}
