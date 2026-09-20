package net.thisptr.jackson.jq.v2.ext.binary;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

final class EncodeTextFunction implements Function {
	private static final String FUNCTION = "binary::encode_text";

	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindContext, List<Expression<Context, JsonNode>> arguments) {
		JsonProvider<JsonNode> jsonProvider = bindContext.getJsonProvider();
		boolean binarySupported = BinarySupport.supportsBinary(jsonProvider);
		Expression<Context, JsonNode> optionsExpression = arguments.isEmpty() ? null : arguments.get(0);
		return new Expression<>() {
			@Override
			public Cardinality getCardinality() {
				return optionsExpression == null ? Cardinality.ONE : optionsExpression.getCardinality();
			}

			@Override
			public boolean dependsOnInput() {
				return true;
			}

			@Override
			public boolean dependsOnExternalState() {
				return optionsExpression != null && optionsExpression.dependsOnExternalState();
			}

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
