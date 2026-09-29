package net.thisptr.jackson.jq.v2.core.internal.builtins.datetime;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.function.utils.ExpressionPropertiesUtils;
import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
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

/**
 * {@code fromdateiso8601}, the number of seconds since the epoch an ISO 8601 datetime names.
 * <p>
 * It is {@code strptime("%Y-%m-%dT%H:%M:%SZ") | mktime} under another name, and reads its input on the
 * same terms: whitespace in front of a field is skipped, and what the format does not account for has to
 * start with whitespace.
 */
@FunctionRegistration(name = "fromdateiso8601", nargs = 0)
public class FromDateIso8601Function implements Function {
	private static final String FORMAT = "%Y-%m-%dT%H:%M:%SZ";

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
			if (!jsonProvider.isString(in))
				throw new JsonQueryException("strptime/1 requires string inputs and arguments");

			String text = jsonProvider.getString(in);
			BrokenDownTime time = new BrokenDownTime();
			int end = CStrptime.parse(text, FORMAT, time, ZoneId.systemDefault());
			if (end == CStrptime.NO_MATCH || (end < text.length() && !Character.isWhitespace(text.charAt(end))))
				throw new JsonQueryException(String.format("date \"%s\" does not match format \"%%Y-%%m-%%dT%%H:%%M:%%SZ\"", text));

			long epochSeconds;
			try {
				epochSeconds = time.toEpochSeconds(ZoneOffset.UTC);
			} catch (DateTimeException e) {
				throw new JsonQueryException("the datetime names a date too far from the epoch", e);
			}
			output.emit(JsonNodeUtils.asNumericNode(jsonProvider, epochSeconds), UntrackedPath.getInstance());
		};
	}
}
