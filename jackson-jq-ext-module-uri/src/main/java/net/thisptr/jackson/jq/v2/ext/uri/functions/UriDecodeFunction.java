package net.thisptr.jackson.jq.v2.ext.uri.functions;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import net.thisptr.jackson.jq.v2.ext.uri.internal.misc.Preconditions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public class UriDecodeFunction implements Function {
	@Override
	// Suppress JdkObsolete because URLDecoder.decode(String, Charset) is not available in Java 8 target.
	@SuppressWarnings("JdkObsolete")
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return new Expression<Context, JsonNode>() {
			@Override
			public Cardinality getCardinality() {
				return Cardinality.ONE;
			}

			@Override
			public boolean dependsOnExternalState() {
				return false;
			}

			@Override
			public void apply(Context context, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
				Preconditions.checkInputType(jsonProvider, "urldecode", in, JsonNodeType.STRING);
				try {
					output.emit(jsonProvider.createString(URLDecoder.decode(jsonProvider.asString(in), StandardCharsets.UTF_8.name())), UntrackedPath.getInstance());
				} catch (UnsupportedEncodingException e) {
					throw new JsonQueryException(e);
				}
			}
		};
	}
}
