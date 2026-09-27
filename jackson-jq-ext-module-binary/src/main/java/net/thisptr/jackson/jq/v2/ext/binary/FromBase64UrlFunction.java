package net.thisptr.jackson.jq.v2.ext.binary;

import java.util.Base64;
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

final class FromBase64UrlFunction implements Function {
	private static final String FUNCTION = "binary::from_base64url";
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
				output.emit(BinarySupport.createBinaryValue(jsonProvider, decodeBase64Url(BinarySupport.getInputText(jsonProvider, FUNCTION, input)), binarySupported, context.getRuntimeLimits()), UntrackedPath.getInstance());
	}

	private static byte[] decodeBase64Url(String text) {
		try {
			return Base64.getUrlDecoder().decode(text);
		} catch (IllegalArgumentException e) {
			throw new JsonQueryException(FUNCTION + " input must be valid Base64URL", e);
		}
	}
}
