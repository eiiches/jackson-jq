package net.thisptr.jackson.jq.v2.ext.binary;

import java.util.List;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.type.BinaryType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.version.Version;

final class FromHexFunction implements Function {
	private static final String FUNCTION = "binary::from_hex";
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(FunctionType.of(StringType.getInstance(), BinaryType.getInstance())));

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
				output.emit(BinarySupport.createBinaryValue(jsonProvider, fromHex(BinarySupport.getInputText(jsonProvider, FUNCTION, input)), binarySupported, context.getRuntimeLimits()), UntrackedPath.getInstance());
	}

	private static byte[] fromHex(String text) {
		if ((text.length() & 1) != 0)
			throw new JsonQueryException(FUNCTION + " input must contain an even number of hex digits");
		byte[] result = new byte[text.length() / 2];
		for (int i = 0; i < result.length; i++) {
			int high = hexDigit(text.charAt(2 * i));
			int low = hexDigit(text.charAt(2 * i + 1));
			if (high < 0 || low < 0)
				throw new JsonQueryException(FUNCTION + " input contains an invalid hex digit at byte " + i);
			result[i] = (byte) ((high << 4) | low);
		}
		return result;
	}

	private static int hexDigit(char digit) {
		if (digit >= '0' && digit <= '9')
			return digit - '0';
		if (digit >= 'a' && digit <= 'f')
			return digit - 'a' + 10;
		if (digit >= 'A' && digit <= 'F')
			return digit - 'A' + 10;
		return -1;
	}
}
