package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.core.internal.FunctionBody;
import net.thisptr.jackson.jq.v2.core.internal.misc.Preconditions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

@AutoService(Function.class)
@FunctionRegistration(name = "todateiso8601", nargs = 0)
public class ToDateIso8601Function implements Function {

	// GregorianCalendar requires java.util.Date for setGregorianChange to configure pure proleptic Gregorian calendar.
	@SuppressWarnings("JavaUtilDate")
	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return FunctionBody.builder(args).usesInput(true).cardinality(Cardinality.ONE).build((scope, in, ipath, output) -> {
			Preconditions.checkInputType(jsonProvider, "todateiso8601", in, JsonNodeType.NUMBER, JsonNodeType.ARRAY);

			if (jsonProvider.getNodeType(in) == JsonNodeType.NUMBER) {
				Long epochSeconds = jsonProvider.getNumberAsLongTruncated(in);
				if (epochSeconds == null) // NaN, an infinity, or beyond long range.
					throw new JsonQueryException("error converting number of seconds since epoch to datetime");
				try {
					String iso8601String = Instant.ofEpochSecond(epochSeconds).toString();
					output.emit(jsonProvider.createString(iso8601String), UntrackedPath.getInstance());
				} catch (DateTimeException e) {
					throw new JsonQueryException("error converting number of seconds since epoch to datetime", e);
				}
			} else if (jsonProvider.getNodeType(in) == JsonNodeType.ARRAY) {
				int size = jsonProvider.size(in);
				if (version.compareTo(Versions.JQ_1_8_0) < 0) {
					if (size < 8)
						throw new JsonQueryException("strftime/1 requires parsed datetime inputs");
					long[] fields = new long[6];
					for (int i = 0; i < 8; i++) {
						JsonNode elem = jsonProvider.getArrayElement(in, i);
						if (elem == null || jsonProvider.getNodeType(elem) != JsonNodeType.NUMBER)
							throw new JsonQueryException("strftime/1 requires parsed datetime inputs");
						double rawVal = jsonProvider.getNumberAsDoubleRounded(elem);
						double val = Double.isNaN(rawVal) ? Integer.MIN_VALUE : rawVal;
						if (i < 6) {
							fields[i] = (long) val;
						}
					}
					String iso8601String = String.format(Locale.ROOT, "%d-%02d-%02dT%02d:%02d:%02dZ",
							fields[0], fields[1] + 1, fields[2], fields[3], fields[4], fields[5]);
					output.emit(jsonProvider.createString(iso8601String), UntrackedPath.getInstance());
				} else {
					int[] fields = new int[8];
					int checkLen = Math.min(size, 8);
					for (int i = 0; i < checkLen; i++) {
						JsonNode elem = jsonProvider.getArrayElement(in, i);
						if (elem == null || jsonProvider.getNodeType(elem) != JsonNodeType.NUMBER)
							throw new JsonQueryException("strftime/1 requires parsed datetime inputs");
						double val = jsonProvider.getNumberAsDoubleRounded(elem);
						if (Double.isNaN(val))
							throw new JsonQueryException("strftime/1 requires parsed datetime inputs");
						double clamped = Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, val));
						fields[i] = (int) clamped;
					}
					int y = (size > 0) ? fields[0] : 1900;
					int m = fields[1];
					int d = fields[2];
					int H = fields[3];
					int M = fields[4];
					int S = fields[5];

					GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.ROOT);
					cal.setGregorianChange(new Date(Long.MIN_VALUE));
					cal.clear();
					cal.set(GregorianCalendar.ERA, y <= 0 ? GregorianCalendar.BC : GregorianCalendar.AD);
					cal.set(GregorianCalendar.YEAR, y <= 0 ? 1 - y : y);
					cal.set(GregorianCalendar.MONTH, m);
					cal.set(GregorianCalendar.DAY_OF_MONTH, d);
					cal.set(GregorianCalendar.HOUR_OF_DAY, H);
					cal.set(GregorianCalendar.MINUTE, M);
					cal.set(GregorianCalendar.SECOND, S);

					int normYear = cal.get(GregorianCalendar.ERA) == GregorianCalendar.BC ? 1 - cal.get(GregorianCalendar.YEAR) : cal.get(GregorianCalendar.YEAR);
					int normMonth = cal.get(GregorianCalendar.MONTH) + 1;
					int normDay = cal.get(GregorianCalendar.DAY_OF_MONTH);
					int normHour = cal.get(GregorianCalendar.HOUR_OF_DAY);
					int normMin = cal.get(GregorianCalendar.MINUTE);
					int normSec = cal.get(GregorianCalendar.SECOND);

					String iso8601String = String.format(Locale.ROOT, "%d-%02d-%02dT%02d:%02d:%02dZ",
							normYear, normMonth, normDay, normHour, normMin, normSec);
					output.emit(jsonProvider.createString(iso8601String), UntrackedPath.getInstance());
				}
			}
		});
	}
}
