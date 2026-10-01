package net.thisptr.jackson.jq.v2.core.internal.builtins.datetime;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.commons.strings.UnicodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.ExpressionPropertiesUtils;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;
import net.thisptr.jackson.jq.v2.spi.version.Version;

/**
 * Writes a time out against a C {@code strftime} format, which is what {@code strftime} and
 * {@code strflocaltime} differ only in the time zone of.
 * <p>
 * The input is either a number of seconds since the epoch or an already broken-down time. jq 1.8.0
 * settles a broken-down time onto the calendar first, so that a 13th month is written as January and the
 * weekday matches the date; before it, the fields were written out as they came.
 */
abstract class AbstractStrFTimeFunction implements Function {
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(FunctionType.of(
					UnionType.of(NumericType.getInstance(), BrokenDownTimes.INPUT_TYPE),
					StringType.getInstance(),
					FilterType.of(StringType.getInstance(), StringType.getInstance()))));

	private final String name;

	private final boolean localZone;

	AbstractStrFTimeFunction(String name, boolean localZone) {
		this.name = name;
		this.localZone = localZone;
	}

	@Override
	public List<TypeScheme<FunctionType>> types(Version jqVersion, int totalArguments) {
		return TYPE_SCHEMES;
	}

	/**
	 * Every call reads the local time zone, whether or not it is the zone the time itself is written in:
	 * {@code %s} counts the broken-down fields as a local wall-clock time, and the format is not known
	 * until the call is made.
	 */
	@Override
	public ExpressionProperties analyze(Version jqVersion, List<ExpressionProperties> arguments) {
		return ExpressionPropertiesUtils.forwardAll(Cardinality.ONE, true, true, arguments);
	}

	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindCtx, List<Expression<Context, JsonNode>> args) {
		JsonProvider<JsonNode> jsonProvider = bindCtx.getJsonProvider();
		Version version = bindCtx.getJqVersion();
		String parsedDatetimeRequired = name + "/1 requires parsed datetime inputs";
		String unknownSystemFailure = name + "/1: unknown system failure";
		// The C library reports the same nothing for a result that does not fit the buffer and for one of
		// no length at all, and jq took either as a failure until 1.8.0 told them apart. Only an empty
		// format writes nothing, so this is what makes strftime("") a failure on the versions before it.
		boolean emptyResultFails = version.compareTo(Versions.JQ_1_8_0) < 0;
		return (scope, in, ipath, output) -> {
			args.get(0).apply(scope, in, UntrackedPath.getInstance(), (format, formatPath) -> {
				if (!jsonProvider.isNumber(in) && !jsonProvider.isArray(in))
					throw new JsonQueryException(parsedDatetimeRequired);
				if (!jsonProvider.isString(format))
					throw new JsonQueryException(name + "/1 requires a string format");

				ZoneId zone = ZoneId.systemDefault();
				@Var BrokenDownTime time;
				try {
					if (jsonProvider.isNumber(in)) {
						double epochSeconds = jsonProvider.getNumberAsDoubleRounded(in);
						time = localZone ? BrokenDownTime.ofEpochSeconds(epochSeconds, zone) : BrokenDownTime.ofEpochSecondsUtc(epochSeconds);
					} else {
						time = BrokenDownTimes.read(jsonProvider, in, version, parsedDatetimeRequired);
						if (version.compareTo(Versions.JQ_1_8_0) >= 0)
							time = settle(time, zone);
					}
					// Before jq 1.8.0 the time handed to the C library named no zone of its own,
					// whatever time was being written: %z wrote +0000, %Z the local zone's standard
					// name, and %s read the fields back as standard time even in the middle of summer.
					if (version.compareTo(Versions.JQ_1_8_0) < 0) {
						time.gmtOffset = 0;
						time.zoneAbbreviation = standardZoneAbbreviation(zone);
						time.daylightSaving = BrokenDownTime.DaylightSaving.NOT_IN_EFFECT;
					}
				} catch (DateTimeException e) {
					throw new JsonQueryException(parsedDatetimeRequired, e);
				}
				String formatString = jsonProvider.getString(format);
				String written = CStrftime.format(formatString, time, zone, bufferSize(formatString));
				if (written == null || (emptyResultFails && written.isEmpty()))
					throw new JsonQueryException(unknownSystemFailure);
				output.emit(jsonProvider.createString(written), UntrackedPath.getInstance());
			});
		};
	}

	/**
	 * The buffer jq gives the C library to write into: room for the format itself and a hundred bytes
	 * more. A result the C library cannot fit in it is the system failure {@code strftime} reports,
	 * whatever the format asked for -- a field width of its own, or simply more conversions than there
	 * is room for.
	 */
	private static int bufferSize(String format) {
		return (int) Math.min(UnicodeUtils.lengthUtf8(format) + 100L, Integer.MAX_VALUE);
	}

	private static String standardZoneAbbreviation(ZoneId zone) {
		TimeZone timeZone = TimeZone.getTimeZone(zone);
		return BrokenDownTime.abbreviate(timeZone.getDisplayName(false, TimeZone.SHORT, Locale.ENGLISH), timeZone.getRawOffset() / 1000);
	}

	/**
	 * Puts a broken-down time back on the calendar, as jq 1.8.0 onwards does before writing one out: the
	 * fields carry, and the weekday, the day of the year and the zone all come from the date that results.
	 */
	private BrokenDownTime settle(BrokenDownTime time, ZoneId zone) {
		if (!localZone) {
			time.normalize();
			time.zoneAbbreviation = "GMT";
			time.gmtOffset = 0;
			return time;
		}
		// jq leaves the daylight-saving field for the zone to work out when it settles one of these, so a
		// wall-clock time on a summer day is the summer time of that day rather than an hour off it.
		time.daylightSaving = BrokenDownTime.DaylightSaving.UNKNOWN;
		return BrokenDownTime.ofEpochSeconds(time.toEpochSeconds(zone), zone);
	}
}
