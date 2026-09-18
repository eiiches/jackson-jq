package net.thisptr.jackson.jq.v2.ext.binary;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.jspecify.annotations.Nullable;

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

final class DecodeTextFunction implements Function {
	private static final String FUNCTION = "binary::decode_text";

	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindContext, List<Expression<Context, JsonNode>> arguments) {
		JsonProvider<JsonNode> jsonProvider = bindContext.getJsonProvider();
		@Nullable Expression<Context, JsonNode> optionsExpression = arguments.isEmpty() ? null : arguments.get(0);
		return new Expression<Context, JsonNode>() {
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
