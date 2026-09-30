package net.thisptr.jackson.jq.v2.core.internal.builtins.datetime;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.chrono.IsoChronology;
import java.util.Locale;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

/**
 * Writes a {@link BrokenDownTime} out the way the C library's {@code strftime} does in the C locale,
 * which is the contract jq's {@code strftime} and {@code strflocaltime} pass on to their callers.
 * <p>
 * A conversion is {@code %}, then any number of the flags {@code _ - 0 ^ #}, then an optional minimum
 * field width, then an optional {@code E} or {@code O} modifier, then the conversion character. Anything
 * the C library does not recognise -- an unknown conversion character, or a modifier that character does
 * not take -- is written out as it was read, padded to the requested width.
 * <p>
 * Every rule here was established by running the C library through jq rather than by reading its source.
 */
final class CStrftime {
	private static final String[] WEEK_DAY_NAMES = { "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };

	private static final String[] MONTH_NAMES = { "January", "February", "March", "April", "May", "June",
			"July", "August", "September", "October", "November", "December" };

	/**
	 * Conversions the {@code ^} flag upper-cases. Every other one is written as it comes.
	 */
	private static final String UPPER_CASED = "aAbBchpZ";

	/**
	 * Conversions the {@code #} flag writes in the other case.
	 */
	private static final String SWAP_CASED = "aAbBhpZ";

	/**
	 * Conversions written as a number, whose sign a zero-padded field keeps in front.
	 */
	private static final String NUMERIC = "CdegGHIjklmMsSuUVwWyY";

	/**
	 * Conversions the {@code #} flag upper-cases even where the modifier in front of them means nothing is
	 * written: the C library settles the case of a month name before it notices it cannot use the modifier.
	 */
	private static final String UPPER_CASED_WHEN_UNWRITABLE = "bBh";

	/**
	 * Conversions that do not take the {@code E} modifier.
	 */
	private static final String REJECTS_E = "aAbBdDeFfgGhHiIjklmMNoqSUvVwW";

	/** Conversions that do not take the {@code O} modifier. */
	private static final String REJECTS_O = "aAcDfFiNoqvxXY";

	private CStrftime() {
	}

	/**
	 * Formats {@code time} against {@code format}.
	 *
	 * @param zone the zone {@code %s} reads the broken-down fields as, which is the local zone whether or
	 * not the time itself is a UTC one -- as the C library does
	 */
	public static String format(String format, BrokenDownTime time, ZoneId zone) {
		StringBuilder out = new StringBuilder();
		@Var int i = 0;
		while (i < format.length()) {
			char c = format.charAt(i);
			if (c != '%') {
				out.append(c);
				i++;
				continue;
			}

			int start = i++;
			@Var boolean noPad = false;
			@Var char padFlag = 0;
			@Var boolean upperCase = false;
			@Var boolean swapCase = false;
			for (; i < format.length(); i++) {
				char flag = format.charAt(i);
				if (flag == '_') {
					padFlag = ' ';
					noPad = false;
				} else if (flag == '-') {
					padFlag = ' ';
					noPad = true;
				} else if (flag == '0') {
					padFlag = '0';
					noPad = false;
				} else if (flag == '^') {
					upperCase = true;
				} else if (flag == '#') {
					swapCase = true;
				} else {
					break;
				}
			}

			@Var int width = 0;
			while (i < format.length() && format.charAt(i) >= '0' && format.charAt(i) <= '9')
				width = width * 10 + (format.charAt(i++) - '0');

			@Var char modifier = 0;
			if (i + 1 < format.length() && (format.charAt(i) == 'E' || format.charAt(i) == 'O'))
				modifier = format.charAt(i++);

			if (i >= format.length()) {
				out.append(pad(format.substring(start), width, padFlag != 0 ? padFlag : ' ', false));
				break;
			}

			char conversion = format.charAt(i++);
			if (conversion == 'z') {
				out.append(zoneOffset(time.gmtOffset, width, noPad, padFlag));
				continue;
			}

			@Var String converted = rejects(modifier, conversion) ? null : convert(conversion, time, zone);
			if (converted == null) {
				@Var String unrecognized = format.substring(start, i);
				if (upperCase || (swapCase && UPPER_CASED_WHEN_UNWRITABLE.indexOf(conversion) >= 0))
					unrecognized = unrecognized.toUpperCase(Locale.ROOT);
				out.append(pad(unrecognized, width, padFlag != 0 ? padFlag : ' ', false));
				continue;
			}
			if (upperCase && UPPER_CASED.indexOf(conversion) >= 0) {
				converted = converted.toUpperCase(Locale.ROOT);
			} else if (swapCase && SWAP_CASED.indexOf(conversion) >= 0) {
				String upper = converted.toUpperCase(Locale.ROOT);
				converted = converted.equals(upper) ? converted.toLowerCase(Locale.ROOT) : upper;
			}
			out.append(pad(converted, Math.max(width, noPad ? 1 : defaultWidth(conversion)),
					padFlag != 0 ? padFlag : defaultPadChar(conversion), NUMERIC.indexOf(conversion) >= 0));
		}
		return out.toString();
	}

	private static boolean rejects(char modifier, char conversion) {
		if (modifier == 'E')
			return REJECTS_E.indexOf(conversion) >= 0;
		if (modifier == 'O')
			return REJECTS_O.indexOf(conversion) >= 0;
		return false;
	}

	private static int defaultWidth(char conversion) {
		return switch (conversion) {
			case 'd', 'e', 'g', 'H', 'I', 'k', 'l', 'm', 'M', 'S', 'U', 'V', 'W', 'y' -> 2;
			case 'j' -> 3;
			default -> 1;
		};
	}

	private static char defaultPadChar(char conversion) {
		return switch (conversion) {
			case 'e', 'k', 'l' -> ' ';
			case 'a', 'A', 'b', 'B', 'c', 'D', 'F', 'h', 'n', 'p', 'P', 'r', 'R', 't', 'T', 'x', 'X', 'Z', '%' -> ' ';
			default -> '0';
		};
	}

	private static @Nullable String convert(char conversion, BrokenDownTime time, ZoneId zone) {
		return switch (conversion) {
			case 'a' -> abbreviate(name(WEEK_DAY_NAMES, time.wday));
			case 'A' -> name(WEEK_DAY_NAMES, time.wday);
			case 'b', 'h' -> abbreviate(name(MONTH_NAMES, time.month));
			case 'B' -> name(MONTH_NAMES, time.month);
			case 'c' -> format("%a %b %e %H:%M:%S %Y", time, zone);
			case 'C' -> number(Math.floorDiv(time.year, 100));
			case 'd' -> number(time.mday);
			case 'D', 'x' -> format("%m/%d/%y", time, zone);
			case 'e' -> number(time.mday);
			case 'F' -> format("%Y-%m-%d", time, zone);
			case 'g' -> number(Math.floorMod(isoWeekBasedYear(time), 100));
			case 'G' -> number(isoWeekBasedYear(time));
			case 'H', 'k' -> number(time.hour);
			case 'I', 'l' -> number(hour12(time.hour));
			case 'j' -> number(time.yday + 1);
			case 'm' -> number(time.month + 1);
			case 'M' -> number(time.min);
			case 'n' -> "\n";
			case 'p' -> time.hour < 12 ? "AM" : "PM";
			case 'P' -> time.hour < 12 ? "am" : "pm";
			case 'r' -> format("%I:%M:%S %p", time, zone);
			case 'R' -> format("%H:%M", time, zone);
			case 's' -> number(time.toEpochSeconds(zone));
			case 'S' -> number(time.sec);
			case 't' -> "\t";
			case 'T', 'X' -> format("%H:%M:%S", time, zone);
			case 'u' -> number(Math.floorMod(time.wday - 1, 7) + 1);
			case 'U' -> number(Math.floorDiv(time.yday + 7 - time.wday, 7));
			case 'V' -> number(isoWeekOfYear(time));
			case 'w' -> number(time.wday);
			case 'W' -> number(Math.floorDiv(time.yday + 7 - Math.floorMod(time.wday + 6, 7), 7));
			case 'y' -> number(Math.floorMod(time.year, 100));
			case 'Y' -> number(time.year);
			case 'Z' -> time.zoneAbbreviation;
			case '%' -> "%";
			default -> null;
		};
	}

	/**
	 * The hour on a 12-hour clock. Midnight reads as 12 and an afternoon hour has 12 taken off it, which
	 * leaves an hour that is not one of the day's as it was.
	 */
	private static int hour12(int hour) {
		if (hour > 12)
			return hour - 12;
		return hour == 0 ? 12 : hour;
	}

	/**
	 * The name of a weekday or a month, or {@code "?"} for a number that names none -- which a broken-down
	 * time can hold on the jq versions that write one out without settling it onto the calendar first.
	 */
	private static String name(String[] names, int index) {
		return index >= 0 && index < names.length ? names[index] : "?";
	}

	private static String abbreviate(String name) {
		return name.length() > 3 ? name.substring(0, 3) : name;
	}

	private static String number(long value) {
		return Long.toString(value);
	}

	/**
	 * Writes a zone offset as {@code +hhmm}, which the C library pads unlike any other conversion: a field
	 * width widens the digits after the sign, and pads what is in front of it as well.
	 */
	private static String zoneOffset(int offsetSeconds, int width, boolean noPad, char padFlag) {
		int magnitude = Math.abs(offsetSeconds);
		String digits = Integer.toString(magnitude / 3600 * 100 + magnitude / 60 % 60);
		return String.valueOf(padFlag == '0' ? '0' : ' ').repeat(Math.max(width - 1, 0))
				+ (offsetSeconds < 0 ? '-' : '+')
				+ pad(digits, Math.max(width, noPad ? 1 : 4), padFlag != 0 ? padFlag : '0', false);
	}

	/**
	 * The ISO 8601 week the time falls in, counted the way the C library does: from the day of the year and
	 * the weekday the {@link BrokenDownTime} carries, not from the calendar date its other fields name.
	 */
	private static int isoWeekOfYear(BrokenDownTime time) {
		int week = isoWeek(time);
		if (week < 1)
			return isoWeeksInYear(time.year - 1);
		if (week > isoWeeksInYear(time.year))
			return 1;
		return week;
	}

	private static int isoWeekBasedYear(BrokenDownTime time) {
		int week = isoWeek(time);
		if (week < 1)
			return time.year - 1;
		if (week > isoWeeksInYear(time.year))
			return time.year + 1;
		return time.year;
	}

	private static int isoWeek(BrokenDownTime time) {
		int isoWeekDay = Math.floorMod(time.wday + 6, 7) + 1;
		return Math.floorDiv(time.yday + 1 - isoWeekDay + 10, 7);
	}

	private static int isoWeeksInYear(int year) {
		if (year < LocalDate.MIN.getYear() || year > LocalDate.MAX.getYear())
			return 52;
		int firstDay = LocalDate.of(year, 1, 1).getDayOfWeek().getValue();
		return firstDay == 4 || (firstDay == 3 && IsoChronology.INSTANCE.isLeapYear(year)) ? 53 : 52;
	}

	/**
	 * Widens {@code value} to {@code width}. Zeroes go after the minus sign of a number, so that a negative
	 * year stays a number; everywhere else they go in front of whatever is there.
	 */
	private static String pad(String value, int width, char padChar, boolean numeric) {
		if (value.length() >= width)
			return value;
		StringBuilder padded = new StringBuilder(width);
		boolean signed = numeric && padChar == '0' && !value.isEmpty() && value.charAt(0) == '-';
		if (signed)
			padded.append('-');
		padded.append(String.valueOf(padChar).repeat(width - value.length()));
		padded.append(signed ? value.substring(1) : value);
		return padded.toString();
	}
}
