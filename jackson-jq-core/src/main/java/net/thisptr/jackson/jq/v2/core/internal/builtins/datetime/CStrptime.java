package net.thisptr.jackson.jq.v2.core.internal.builtins.datetime;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.chrono.IsoChronology;
import java.util.function.IntConsumer;

import com.google.errorprone.annotations.Var;

/**
 * Reads a {@link BrokenDownTime} back out of a string the way the C library's {@code strptime} does in the
 * C locale, which is the contract jq's {@code strptime} passes on to its callers.
 * <p>
 * A conversion is {@code %}, then an optional maximum field width, then an optional {@code E} or {@code O}
 * modifier, then the conversion character. Whitespace in the format matches any run of whitespace in the
 * input, including none; every other character has to match itself. Parsing never backtracks: a conversion
 * takes as much as it can, and what follows either matches what is left or the whole parse fails.
 * <p>
 * What a parse leaves behind matters to the caller, so this reports where in the input it stopped rather
 * than only whether it succeeded.
 * <p>
 * Every rule here was established by running the C library through jq rather than by reading its source.
 */
final class CStrptime {
	/**
	 * Reported instead of an offset when the input does not match the format.
	 */
	public static final int NO_MATCH = -1;

	private static final String[] WEEK_DAY_NAMES = { "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };

	private static final String[] MONTH_NAMES = { "January", "February", "March", "April", "May", "June",
			"July", "August", "September", "October", "November", "December" };

	/**
	 * Conversions that take the {@code E} modifier. Every other one fails with it in front.
	 */
	private static final String ACCEPTS_E = "cCxXY";

	/**
	 * Conversions that take the {@code O} modifier.
	 */
	private static final String ACCEPTS_O = "bBdehHImMSUVwWy";

	/**
	 * Days from the start of the year to the start of each month, in a common year.
	 */
	private static final int[] MONTH_START_DAY = { 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334 };

	private CStrptime() {
	}

	/**
	 * Parses {@code input} against {@code format}, filling in {@code time}.
	 *
	 * @param zone the zone {@code %s} reads its epoch seconds as, which is the local zone -- as the C
	 * library does
	 * @return the offset in {@code input} the parse stopped at, or {@link #NO_MATCH}
	 */
	public static int parse(String input, String format, BrokenDownTime time, ZoneId zone) {
		Parser parser = new Parser(input, time, zone);
		if (!parser.parse(format))
			return NO_MATCH;
		parser.finish();
		return parser.position();
	}

	private static final class Parser {
		private final String input;
		private final BrokenDownTime time;
		private final ZoneId zone;
		private int position;

		private boolean haveYear;
		private boolean haveMonth;
		private boolean haveDay;
		private boolean haveWeekDay;
		private boolean haveYearDay;

		private int century = Integer.MIN_VALUE;
		private int yearOfCentury = Integer.MIN_VALUE;
		private int hourOfHalfDay = Integer.MIN_VALUE;
		private boolean afternoon;
		private int weekNumber = Integer.MIN_VALUE;
		private boolean weekStartsOnMonday;

		Parser(String input, BrokenDownTime time, ZoneId zone) {
			this.input = input;
			this.time = time;
			this.zone = zone;
		}

		int position() {
			return position;
		}

		boolean parse(String format) {
			@Var int i = 0;
			while (i < format.length()) {
				char c = format.charAt(i);
				if (Character.isWhitespace(c)) {
					skipWhitespace();
					i++;
					continue;
				}
				if (c != '%') {
					if (position >= input.length() || input.charAt(position) != c)
						return false;
					position++;
					i++;
					continue;
				}

				i++;
				@Var int width = 0;
				while (i < format.length() && format.charAt(i) >= '0' && format.charAt(i) <= '9')
					width = width * 10 + (format.charAt(i++) - '0');
				@Var char modifier = 0;
				if (i < format.length() && (format.charAt(i) == 'E' || format.charAt(i) == 'O'))
					modifier = format.charAt(i++);
				if (i >= format.length())
					return false;
				char conversion = format.charAt(i++);
				if (modifier == 'E' && ACCEPTS_E.indexOf(conversion) < 0)
					return false;
				if (modifier == 'O' && ACCEPTS_O.indexOf(conversion) < 0)
					return false;
				if (!convert(conversion, width))
					return false;
			}
			return true;
		}

		private boolean convert(char conversion, int width) {
			return switch (conversion) {
				case 'a', 'A' -> weekDayName();
				case 'b', 'B', 'h' -> monthName();
				case 'c' -> parse("%a %b %e %H:%M:%S %Y");
				case 'C' -> field(width, 2, 0, 99, value -> century = value);
				case 'd', 'e' -> field(width, 2, 1, 31, value -> {
					time.mday = value;
					haveDay = true;
				});
				case 'D', 'x' -> parse("%m/%d/%y");
				case 'F' -> parse("%Y-%m-%d");
				case 'g' -> field(width, 2, 0, 99, value -> ignore());
				case 'G' -> weekBasedYear();
				case 'H', 'k' -> field(width, 2, 0, 23, value -> {
					time.hour = value;
					hourOfHalfDay = Integer.MIN_VALUE;
				});
				case 'I', 'l' -> field(width, 2, 1, 12, value -> hourOfHalfDay = value);
				case 'j' -> field(width, 3, 1, 366, value -> {
					time.yday = value - 1;
					haveYearDay = true;
				});
				case 'm' -> field(width, 2, 1, 12, value -> {
					time.month = value - 1;
					haveMonth = true;
				});
				case 'M' -> field(width, 2, 0, 59, value -> time.min = value);
				case 'n', 't' -> {
					skipWhitespace();
					yield true;
				}
				case 'p', 'P' -> halfDay();
				case 'r' -> parse("%I:%M:%S %p");
				case 'R' -> parse("%H:%M");
				case 's' -> epochSeconds();
				case 'S' -> field(width, 2, 0, 61, value -> time.sec = value);
				case 'T', 'X' -> parse("%H:%M:%S");
				case 'u' -> field(width, 1, 1, 7, value -> {
					time.wday = value % 7;
					haveWeekDay = true;
				});
				case 'U' -> field(width, 2, 0, 53, value -> {
					weekNumber = value;
					weekStartsOnMonday = false;
				});
				case 'V' -> field(width, 2, 0, 53, value -> ignore());
				case 'W' -> field(width, 2, 0, 53, value -> {
					weekNumber = value;
					weekStartsOnMonday = true;
				});
				case 'w' -> field(width, 1, 0, 6, value -> {
					time.wday = value;
					haveWeekDay = true;
				});
				case 'y' -> field(width, 2, 0, 99, value -> yearOfCentury = value);
				case 'Y' -> field(width, 4, 0, 9999, value -> {
					time.year = value;
					century = Integer.MIN_VALUE;
					yearOfCentury = Integer.MIN_VALUE;
					haveYear = true;
				});
				case 'z' -> zoneOffset();
				case 'Z' -> zoneName();
				case '%' -> literal('%');
				default -> false;
			};
		}

		/**
		 * A field the C library reads and then has no room for: the week of the year, and the year a week
		 * number is counted within, say nothing a {@code struct tm} can hold.
		 */
		private void ignore() {
		}

		/**
		 * Reads a number of at most {@code width} digits, or {@code defaultWidth} if the format did not say,
		 * and hands it to {@code field}.
		 * <p>
		 * Reading stops early once another digit could not fit in the field at all, which is what lets
		 * {@code %m%d} read {@code "318"} as the 18th of March. It does not stop because a digit happens not
		 * to fit: {@code %m} reads both digits of {@code "14"} and then fails, since a month could have
		 * started {@code 1}.
		 */
		private boolean field(int width, int defaultWidth, int min, int max, IntConsumer field) {
			skipWhitespace();
			int digits = width > 0 ? width : defaultWidth;
			@Var int end = position;
			@Var int value = 0;
			while (end < input.length() && end - position < digits && input.charAt(end) >= '0' && input.charAt(end) <= '9') {
				if (end > position && value > max / 10)
					break;
				value = value * 10 + (input.charAt(end++) - '0');
			}
			if (end == position || value < min || value > max)
				return false;
			position = end;
			field.accept(value);
			return true;
		}

		private boolean weekDayName() {
			skipWhitespace();
			for (int day = 0; day < WEEK_DAY_NAMES.length; day++) {
				if (matchesIgnoreCase(WEEK_DAY_NAMES[day]) || matchesIgnoreCase(WEEK_DAY_NAMES[day].substring(0, 3))) {
					time.wday = day;
					haveWeekDay = true;
					return true;
				}
			}
			return false;
		}

		private boolean monthName() {
			skipWhitespace();
			for (int month = 0; month < MONTH_NAMES.length; month++) {
				if (matchesIgnoreCase(MONTH_NAMES[month]) || matchesIgnoreCase(MONTH_NAMES[month].substring(0, 3))) {
					time.month = month;
					haveMonth = true;
					return true;
				}
			}
			return false;
		}

		private boolean halfDay() {
			skipWhitespace();
			if (matchesIgnoreCase("AM")) {
				afternoon = false;
				return true;
			}
			if (matchesIgnoreCase("PM")) {
				afternoon = true;
				return true;
			}
			return false;
		}

		private boolean epochSeconds() {
			skipWhitespace();
			@Var int end = position;
			int firstDigit = end;
			while (end < input.length() && input.charAt(end) >= '0' && input.charAt(end) <= '9')
				end++;
			if (end == firstDigit)
				return false;

			BrokenDownTime split;
			try {
				split = BrokenDownTime.ofEpochSeconds(Long.parseLong(input, position, end, 10), zone);
			} catch (NumberFormatException | DateTimeException e) {
				return false;
			}
			time.year = split.year;
			time.month = split.month;
			time.mday = split.mday;
			time.hour = split.hour;
			time.min = split.min;
			time.sec = split.sec;
			haveYear = true;
			haveMonth = true;
			haveDay = true;
			hourOfHalfDay = Integer.MIN_VALUE;
			position = end;
			return true;
		}

		/**
		 * Reads a zone offset and throws it away, as jq does: the time it hands back is the one that was
		 * written, not the instant that offset makes of it.
		 */
		private boolean zoneOffset() {
			skipWhitespace();
			if (position < input.length() && input.charAt(position) == 'Z') {
				position++;
				return true;
			}
			if (position >= input.length() || (input.charAt(position) != '+' && input.charAt(position) != '-'))
				return false;
			@Var int end = position + 1;
			if (!twoDigitsAt(end))
				return false;
			end += 2;
			int minutes = end < input.length() && input.charAt(end) == ':' ? end + 1 : end;
			if (twoDigitsAt(minutes))
				end = minutes + 2;
			position = end;
			return true;
		}

		/**
		 * Reads the year a week number is counted within and throws it away, as the C library does. It is
		 * the one numeric field with no width of its own, so it takes every digit it is given.
		 */
		private boolean weekBasedYear() {
			skipWhitespace();
			@Var int end = position;
			while (end < input.length() && input.charAt(end) >= '0' && input.charAt(end) <= '9')
				end++;
			if (end == position)
				return false;
			position = end;
			return true;
		}

		/** Reads a zone name and throws it away: a {@code struct tm} has nowhere to put one. */
		private boolean zoneName() {
			skipWhitespace();
			while (position < input.length() && !Character.isWhitespace(input.charAt(position)))
				position++;
			return true;
		}

		private boolean literal(char expected) {
			if (position >= input.length() || input.charAt(position) != expected)
				return false;
			position++;
			return true;
		}

		private boolean twoDigitsAt(int at) {
			return at + 1 < input.length()
					&& input.charAt(at) >= '0' && input.charAt(at) <= '9'
					&& input.charAt(at + 1) >= '0' && input.charAt(at + 1) <= '9';
		}

		private boolean matchesIgnoreCase(String candidate) {
			if (!input.regionMatches(true, position, candidate, 0, candidate.length()))
				return false;
			position += candidate.length();
			return true;
		}

		private void skipWhitespace() {
			while (position < input.length() && Character.isWhitespace(input.charAt(position)))
				position++;
		}

		/**
		 * Settles the fields that only make sense once the whole format has been read: the year that the
		 * century and the year within it name between them, the hour that a 12-hour clock and its half of
		 * the day name between them, and the weekday and day of the year the date implies.
		 */
		void finish() {
			if (century != Integer.MIN_VALUE) {
				time.year = century * 100 + (yearOfCentury != Integer.MIN_VALUE ? yearOfCentury : 0);
				haveYear = true;
			} else if (yearOfCentury != Integer.MIN_VALUE) {
				time.year = yearOfCentury >= 69 ? 1900 + yearOfCentury : 2000 + yearOfCentury;
				haveYear = true;
			}

			if (hourOfHalfDay != Integer.MIN_VALUE)
				time.hour = hourOfHalfDay % 12 + (afternoon ? 12 : 0);

			boolean weekGivesDate = weekNumber != Integer.MIN_VALUE && haveWeekDay && !haveYearDay;
			if (weekGivesDate)
				deriveYearDayFromWeek();

			if (!haveYear && !haveMonth && !haveDay && !weekGivesDate)
				return;
			if (haveYearDay || weekGivesDate) {
				deriveFromYearDay();
				time.computeWeekDayAndYearDay(!haveWeekDay, false);
			} else {
				time.computeWeekDayAndYearDay(!haveWeekDay, true);
			}
		}

		/**
		 * Counts out the day of the year that a week of the year and a weekday name between them, once the
		 * format has given both. Weeks are numbered from the first {@code %U} Sunday or {@code %W} Monday of
		 * the year, so the days before it fall in week zero.
		 */
		private void deriveYearDayFromWeek() {
			int firstDayOfYear = LocalDate.of(clampYear(time.year), 1, 1).getDayOfWeek().getValue() % 7;
			int weekStart = Math.floorMod((weekStartsOnMonday ? 8 : 7) - firstDayOfYear, 7);
			int dayOfWeek = weekStartsOnMonday ? Math.floorMod(time.wday + 6, 7) : time.wday;
			time.yday = (weekNumber - 1) * 7 + dayOfWeek + weekStart;
		}

		private static int clampYear(int year) {
			return Math.max(LocalDate.MIN.getYear(), Math.min(LocalDate.MAX.getYear(), year));
		}

		/**
		 * Reads the month and the day of the month off the day of the year, for whichever of the two the
		 * format did not give. The last month absorbs whatever is left over, so the 366th day of a common
		 * year is written as the 32nd of December rather than rolled into the year after.
		 */
		private void deriveFromYearDay() {
			int leapDay = IsoChronology.INSTANCE.isLeapYear(time.year) ? 1 : 0;
			@Var int month = MONTH_START_DAY.length - 1;
			for (int candidate = 1; candidate < MONTH_START_DAY.length; candidate++) {
				if (time.yday < MONTH_START_DAY[candidate] + (candidate >= 2 ? leapDay : 0)) {
					month = candidate - 1;
					break;
				}
			}
			if (!haveDay)
				time.mday = time.yday - (MONTH_START_DAY[month] + (month >= 2 ? leapDay : 0)) + 1;
			if (!haveMonth)
				time.month = month;
		}
	}
}
