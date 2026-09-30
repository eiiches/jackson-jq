package net.thisptr.jackson.jq.v2.core.internal.builtins.datetime;

import java.time.DateTimeException;
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
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.version.Version;

/**
 * {@code mktime}, the number of seconds since the epoch a broken-down time names in UTC.
 * <p>
 * The weekday and the day of the year are not read: the date comes from the year, month and day alone,
 * and a field outside its usual range carries into the one above it, so a 13th month is January of the
 * year after. A fraction of a second is dropped.
 */
@FunctionRegistration(name = "mktime", nargs = 0)
public class MkTimeFunction implements Function {
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(FunctionType.of(BrokenDownTimes.INPUT_TYPE, NumericType.getInstance())));

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
		Version version = bindCtx.getJqVersion();
		return (scope, in, ipath, output) -> {
			if (!jsonProvider.isArray(in))
				throw new JsonQueryException("mktime requires array inputs");
			BrokenDownTime time = BrokenDownTimes.read(jsonProvider, in, version, "mktime requires parsed datetime inputs");
			long epochSeconds;
			try {
				epochSeconds = time.toEpochSeconds(ZoneOffset.UTC);
			} catch (DateTimeException e) {
				throw new JsonQueryException("the broken-down time names a date too far from the epoch", e);
			}
			// jq treats timegm's -1 result as a conversion failure, even for a valid pre-epoch time.
			if (epochSeconds == -1)
				throw new JsonQueryException("invalid gmtime representation");
			output.emit(JsonNodeUtils.asNumericNode(jsonProvider, epochSeconds), UntrackedPath.getInstance());
		};
	}
}
