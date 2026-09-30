package net.thisptr.jackson.jq.v2.core.internal.builtins.datetime;

import java.time.ZoneId;
import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.function.utils.ExpressionPropertiesUtils;
import net.thisptr.jackson.jq.v2.core.version.Versions;
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
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.version.Version;

/**
 * {@code strptime(fmt)}, a date and a time read out of a string against a C {@code strptime} format.
 * <p>
 * The format does not have to account for the whole string, but what it leaves has to start with
 * whitespace; that remainder is handed back as a ninth element of the array, which the builtins that read
 * a broken-down time ignore.
 */
@FunctionRegistration(name = "strptime", nargs = 1)
public class StrPTimeFunction implements Function {
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(FunctionType.of(
					StringType.getInstance(),
					BrokenDownTimes.PARSED_TYPE,
					FilterType.of(StringType.getInstance(), StringType.getInstance()))));

	@Override
	public List<TypeScheme<FunctionType>> types(Version jqVersion, int totalArguments) {
		return TYPE_SCHEMES;
	}

	@Override
	public ExpressionProperties analyze(Version jqVersion, List<ExpressionProperties> arguments) {
		return ExpressionPropertiesUtils.forwardAll(Cardinality.ONE, true, true, arguments);
	}

	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindCtx, List<Expression<Context, JsonNode>> args) {
		JsonProvider<JsonNode> jsonProvider = bindCtx.getJsonProvider();
		Version version = bindCtx.getJqVersion();
		return (scope, in, ipath, output) -> {
			args.get(0).apply(scope, in, UntrackedPath.getInstance(), (format, formatPath) -> {
				if (!jsonProvider.isString(in) || !jsonProvider.isString(format))
					throw new JsonQueryException("strptime/1 requires string inputs and arguments");

				String text = jsonProvider.getString(in);
				String pattern = jsonProvider.getString(format);
				BrokenDownTime time = new BrokenDownTime();
				int end = CStrptime.parse(text, pattern, time, ZoneId.systemDefault());
				if (end == CStrptime.NO_MATCH || (end < text.length() && !Character.isWhitespace(text.charAt(end))))
					throw new JsonQueryException(String.format("date \"%s\" does not match format \"%s\"", text, pattern));
				if (version.compareTo(Versions.JQ_1_6) < 0)
					settleUnknownDate(time);

				output.emit(BrokenDownTimes.write(jsonProvider, time, text.substring(end)), UntrackedPath.getInstance());
			});
		};
	}

	/**
	 * jq 1.5 reports a weekday and a day of the year it never learned as zero rather than as a value out of
	 * range, and settles a parse that learned neither onto the calendar: a format naming only a time of day
	 * comes back as that time on the last day of 1899, the day a zeroth of January falls on.
	 */
	private static void settleUnknownDate(BrokenDownTime time) {
		if (time.wday == BrokenDownTime.UNKNOWN_WEEK_DAY)
			time.wday = 0;
		if (time.yday == BrokenDownTime.UNKNOWN_YEAR_DAY)
			time.yday = 0;
		if (time.wday == 0 && time.yday == 0)
			time.normalize();
	}
}
