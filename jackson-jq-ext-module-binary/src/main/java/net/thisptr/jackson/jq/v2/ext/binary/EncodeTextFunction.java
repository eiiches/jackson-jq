package net.thisptr.jackson.jq.v2.ext.binary;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.type.BinaryType;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.version.Version;

final class EncodeTextFunction implements Function {
	private static final String FUNCTION = "binary::encode_text";

	/**
	 * Indexed by argument count.
	 */
	private static final List<List<TypeScheme<FunctionType>>> TYPE_SCHEMES = List.of(
			List.of(TypeScheme.of(FunctionType.of(StringType.getInstance(), BinaryType.getInstance()))),
			List.of(TypeScheme.of(FunctionType.of(StringType.getInstance(), BinaryType.getInstance(), FilterType.of(StringType.getInstance(), BinarySupport.OPTIONS)))));

	@Override
	public List<TypeScheme<FunctionType>> types(Version jqVersion, int totalArguments) {
		if (totalArguments < 0 || totalArguments > 1)
			return List.of();
		return TYPE_SCHEMES.get(totalArguments);
	}

	@Override
	public ExpressionProperties analyze(Version jqVersion, List<ExpressionProperties> arguments) {
		Cardinality cardinality = arguments.isEmpty() ? Cardinality.ONE : arguments.get(0).cardinality();
		boolean external = !arguments.isEmpty() && arguments.get(0).dependsOnExternalState();
		return new ExpressionProperties(cardinality, true, external);
	}

	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindContext, List<Expression<Context, JsonNode>> arguments) {
		JsonProvider<JsonNode> jsonProvider = bindContext.getJsonProvider();
		boolean binarySupported = BinarySupport.supportsBinary(jsonProvider);
		Expression<Context, JsonNode> optionsExpression = arguments.isEmpty() ? null : arguments.get(0);
		return new Expression<>() {


			@Override
			public void apply(Context context, JsonNode input, Path<JsonNode> inputPath, Output<JsonNode> output) throws JsonQueryException {
				String text = BinarySupport.getInputText(jsonProvider, FUNCTION, input);
				if (optionsExpression == null) {
					byte[] bytes = BinarySupport.encodeText(FUNCTION, text, StandardCharsets.UTF_8);
					JsonNode result = BinarySupport.createBinaryValue(jsonProvider, bytes, binarySupported, context.getRuntimeLimits());
					output.emit(result, UntrackedPath.getInstance());
					return;
				}
				optionsExpression.apply(context, input, inputPath, (optionsNode, optionsPath) -> {
					Charset charset = BinarySupport.parseCharset(jsonProvider, FUNCTION, optionsNode);
					byte[] bytes = BinarySupport.encodeText(FUNCTION, text, charset);
					JsonNode result = BinarySupport.createBinaryValue(jsonProvider, bytes, binarySupported, context.getRuntimeLimits());
					output.emit(result, UntrackedPath.getInstance());
				});
			}
		};
	}
}
