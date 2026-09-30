package net.thisptr.jackson.jq.v2.core.internal.builtins.datetime;

import java.util.ArrayList;
import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;
import net.thisptr.jackson.jq.v2.spi.version.Version;

/**
 * The array jq's datetime builtins pass a {@link BrokenDownTime} around in:
 * {@code [year, month, mday, hour, min, sec, wday, yday]}, with a zero-based month and day of the year.
 * <p>
 * Which arrays count as one changed in jq 1.8.0. Before it, a shorter array was not a broken-down time at
 * all, and a field that was not a number was read as whatever the C cast made of it; since it, the missing
 * fields are zero and a field that is not a number is an error.
 */
final class BrokenDownTimes {
	/**
	 * What {@code gmtime} and {@code localtime} hand back.
	 */
	static final Type TYPE = ArrayType.of(List.of(
			NumericType.getInstance(), NumericType.getInstance(), NumericType.getInstance(), NumericType.getInstance(),
			NumericType.getInstance(), NumericType.getInstance(), NumericType.getInstance(), NumericType.getInstance()));

	/**
	 * What the builtins that read one accept. It is wider than {@link #TYPE} because {@code strptime} adds
	 * the text it did not parse to the end of the array, and feeding that straight to {@code mktime} is the
	 * usual way to turn a date back into a number.
	 */
	static final Type INPUT_TYPE = ArrayType.of(UnionType.of(NumericType.getInstance(), StringType.getInstance()));

	/**
	 * What {@code strptime} hands back: a broken-down time, and the text it did not parse.
	 */
	static final Type PARSED_TYPE = ArrayType.of(List.of(
					NumericType.getInstance(), NumericType.getInstance(), NumericType.getInstance(), NumericType.getInstance(),
					NumericType.getInstance(), NumericType.getInstance(), NumericType.getInstance(), NumericType.getInstance()),
			StringType.getInstance());

	private BrokenDownTimes() {
	}

	/**
	 * Reads a broken-down time out of an array.
	 *
	 * @param message the error every way the array can fail to be one is reported with, which jq words for
	 * the builtin that was called
	 */
	static <JsonNode> BrokenDownTime read(JsonProvider<JsonNode> jsonProvider, JsonNode in, Version version, String message) {
		boolean fieldsMayBeMissing = version.compareTo(Versions.JQ_1_8_0) >= 0;
		int size = jsonProvider.getArrayLength(in);
		if (!fieldsMayBeMissing && size < 8)
			throw new JsonQueryException(message);

		int[] fields = new int[8];
		for (int i = 0; i < Math.min(size, fields.length); i++) {
			JsonNode element = jsonProvider.getArrayElement(in, i);
			if (!jsonProvider.isNumber(element))
				throw new JsonQueryException(message);
			double value = jsonProvider.getNumberAsDoubleRounded(element);
			if (Double.isNaN(value)) {
				if (fieldsMayBeMissing)
					throw new JsonQueryException(message);
				fields[i] = Integer.MIN_VALUE;
			} else {
				fields[i] = i == 0 ? readYear(value, fieldsMayBeMissing)
						: (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, value));
			}
		}

		BrokenDownTime time = new BrokenDownTime();
		if (size > 0)
			time.year = fields[0];
		time.month = fields[1];
		time.mday = fields[2];
		time.hour = fields[3];
		time.min = fields[4];
		time.sec = fields[5];
		time.wday = fields[6];
		time.yday = fields[7];
		return time;
	}

	private static int readYear(double value, boolean jq18OrLater) {
		if (!jq18OrLater) {
			// Older jq converts an out-of-range year directly to the C int minimum.
			return value < Integer.MIN_VALUE || value > Integer.MAX_VALUE ? Integer.MIN_VALUE : (int) value;
		}
		// jq 1.8 first stores the year relative to 1900 in a C int, then adds 1900 back.
		// The addition can wrap even when the input itself fits in an int.
		double relativeYear = value - 1900;
		int tmYear = (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, relativeYear));
		return tmYear + 1900;
	}

	/**
	 * Writes a broken-down time out as an array, with the text a parse did not consume after it.
	 */
	static <JsonNode> JsonNode write(JsonProvider<JsonNode> jsonProvider, BrokenDownTime time, String unparsed) {
		List<JsonNode> fields = new ArrayList<>(9);
		fields.add(JsonNodeUtils.asNumericNode(jsonProvider, time.year));
		fields.add(JsonNodeUtils.asNumericNode(jsonProvider, time.month));
		fields.add(JsonNodeUtils.asNumericNode(jsonProvider, time.mday));
		fields.add(JsonNodeUtils.asNumericNode(jsonProvider, time.hour));
		fields.add(JsonNodeUtils.asNumericNode(jsonProvider, time.min));
		fields.add(JsonNodeUtils.asNumericNode(jsonProvider, time.sec + time.secFraction));
		fields.add(JsonNodeUtils.asNumericNode(jsonProvider, time.wday));
		fields.add(JsonNodeUtils.asNumericNode(jsonProvider, time.yday));
		if (!unparsed.isEmpty())
			fields.add(jsonProvider.createString(unparsed));
		return jsonProvider.createArray(fields);
	}
}
