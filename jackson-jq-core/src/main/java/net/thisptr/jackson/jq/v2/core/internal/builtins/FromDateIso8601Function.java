package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.function.utils.ExpressionPropertiesUtils;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.Preconditions;
import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@FunctionRegistration(name = "fromdateiso8601", nargs = 0)
public class FromDateIso8601Function implements Function {
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(FunctionType.of(StringType.getInstance(), NumericType.getInstance())));

	@Override
	public List<TypeScheme<FunctionType>> types(Version jqVersion, int totalArguments) {
		return TYPE_SCHEMES;
	}

	@Override
	public ExpressionProperties analyze(Version jqVersion, List<ExpressionProperties> arguments) {
		return ExpressionPropertiesUtils.forwardAll(Cardinality.ONE, true, false, arguments);
	}

	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindCtx, List<Expression<Context, JsonNode>> args) {
		JsonProvider<JsonNode> jsonProvider = bindCtx.getJsonProvider();
		return (scope, in, ipath, output) -> {
			Preconditions.checkInputType(jsonProvider, "fromdateiso8601", in, JsonNodeType.STRING);
			try {
				String iso8601String = jsonProvider.getString(in);
				// In future versions of JQ, it may need to be revisited due to fractional support: https://github.com/jqlang/jq/issues/1409
				if (iso8601String.length() > 20) {
					throw new JsonQueryException(String.format("date \"%s\" does not match format \"%%Y-%%m-%%dT%%H:%%M:%%SZ\"", iso8601String));
				}
				long epochSeconds = Instant.parse(iso8601String).getEpochSecond();
				output.emit(JsonNodeUtils.asNumericNode(jsonProvider, epochSeconds), UntrackedPath.getInstance());
			} catch (DateTimeParseException e) {
				throw new JsonQueryException(e);
			}
		};
	}
}
