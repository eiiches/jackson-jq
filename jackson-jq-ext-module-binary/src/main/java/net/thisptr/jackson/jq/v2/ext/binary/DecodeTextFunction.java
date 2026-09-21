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

final class DecodeTextFunction implements Function {
	private static final String FUNCTION = "binary::decode_text";

	/**
	 * Indexed by argument count.
	 */
	private static final List<List<TypeScheme<FunctionType>>> TYPE_SCHEMES = List.of(
			List.of(TypeScheme.of(FunctionType.of(BinaryType.getInstance(), StringType.getInstance()))),
			List.of(TypeScheme.of(FunctionType.of(BinaryType.getInstance(), StringType.getInstance(), FilterType.of(BinaryType.getInstance(), BinarySupport.OPTIONS)))));

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
		Expression<Context, JsonNode> optionsExpression = arguments.isEmpty() ? null : arguments.get(0);
		return new Expression<>() {


			@Override
			public void apply(Context context, JsonNode input, Path<JsonNode> inputPath, Output<JsonNode> output) throws JsonQueryException {
				byte[] bytes = BinarySupport.getInputBytes(jsonProvider, FUNCTION, input);
				if (optionsExpression == null) {
					String text = BinarySupport.decodeText(FUNCTION, bytes, StandardCharsets.UTF_8, context.getRuntimeLimits());
					output.emit(jsonProvider.createString(text), UntrackedPath.getInstance());
					return;
				}
				optionsExpression.apply(context, input, inputPath, (optionsNode, optionsPath) -> {
					Charset charset = BinarySupport.parseCharset(jsonProvider, FUNCTION, optionsNode);
					String text = BinarySupport.decodeText(FUNCTION, bytes, charset, context.getRuntimeLimits());
					output.emit(jsonProvider.createString(text), UntrackedPath.getInstance());
				});
			}
		};
	}
}
