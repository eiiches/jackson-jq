package net.thisptr.jackson.jq.v2.ext.binary;

import java.util.List;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.RuntimeLimits;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.type.BinaryType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.version.Version;

final class ToHexFunction implements Function {
	private static final String FUNCTION = "binary::to_hex";
	private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(FunctionType.of(BinaryType.getInstance(), StringType.getInstance())));

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
				output.emit(jsonProvider.createString(toHex(BinarySupport.getInputBytes(jsonProvider, FUNCTION, input), context.getRuntimeLimits())), UntrackedPath.getInstance());
	}

	private static String toHex(byte[] bytes, RuntimeLimits limits) {
		BinarySupport.checkStringLength(limits, 2L * bytes.length);
		char[] result = new char[2 * bytes.length];
		for (int i = 0; i < bytes.length; i++) {
			int value = bytes[i] & 0xff;
			result[2 * i] = HEX_DIGITS[value >>> 4];
			result[2 * i + 1] = HEX_DIGITS[value & 0x0f];
		}
		return new String(result);
	}
}
