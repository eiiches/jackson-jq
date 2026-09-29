package net.thisptr.jackson.jq.v2.core.internal.builtins.datetime;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.zone.ZoneRules;
import java.util.Locale;


/**
 * A calendar date and time split into its parts, the way jq's datetime builtins pass one around.
 * <p>
 * jq inherits this representation from the C library's {@code struct tm}: the same fields, the same
 * zero-based month, and the same willingness to hold a combination no calendar has, such as a day 0 or a
 * month 13. {@code gmtime} hands one to jq as the array
 * {@code [year, month, mday, hour, min, sec, wday, yday]}, and {@code mktime}, {@code strftime} and
 * {@code strflocaltime} read one back. The fields are therefore mutable and public: this is a parameter
 * block that {@link CStrptime} fills in and {@link CStrftime} reads, not a value object.
 * <p>
 * {@link #year} is the full year, not the C offset from 1900.
 */
final class BrokenDownTime {
	/**
	 * The weekday jq reports for a time whose date it never learned.
	 */
	public static final int UNKNOWN_WEEK_DAY = 8;

	/**
	 * The day of the year jq reports for a time whose date it never learned.
	 */
	public static final int UNKNOWN_YEAR_DAY = 367;

	/**
	 * The largest number of seconds either side of the epoch that a date can be written for.
	 */
	private static final long EPOCH_SECONDS_LIMIT = 31556889864403199L;

	/**
	 * The short zone name {@code %Z} writes. The C library takes the abbreviation straight from the zone
	 * database, which names no language; the closest Java has is the English short name, which is that
	 * abbreviation for every zone that has one. See {@link #abbreviate} for the zones that have none.
	 */
	private static final DateTimeFormatter ZONE_ABBREVIATION = DateTimeFormatter.ofPattern("zzz", Locale.ENGLISH);

	/**
	 * What Java writes for a zone with no abbreviation of its own.
	 */
	private static final String OFFSET_NAME_PREFIX = "GMT";

	/**
	 * Days from the start of the year to the start of each month, in a common year.
	 */
	private static final int[] MONTH_START_DAY = { 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334 };

	public int year = 1900;
	public int month;
	public int mday;
	public int hour;
	public int min;
	public int sec;

	/**
	 * The sub-second part of {@link #sec}, in {@code [0, 1)}. jq keeps the fraction of the epoch seconds
	 * {@code gmtime} was given, and drops it everywhere a C {@code struct tm} would.
	 */
	public double secFraction;

	public int wday = UNKNOWN_WEEK_DAY;
	public int yday = UNKNOWN_YEAR_DAY;

	/** Seconds east of UTC, as {@code %z} writes it. */
	public int gmtOffset;

	/** The time zone abbreviation {@code %Z} writes. */
	public String zoneAbbreviation = "GMT";

	/**
	 * Whether daylight saving is in effect, which decides how {@code %s} reads these fields back as a
	 * wall-clock time. A time that came from {@code localtime} knows; one that came from {@code gmtime} or
	 * out of an array says {@link DaylightSaving#NOT_IN_EFFECT}, because jq leaves the C field zeroed.
	 */
	public DaylightSaving daylightSaving = DaylightSaving.NOT_IN_EFFECT;

	/**
	 * The C {@code tm_isdst} field: whether daylight saving was in effect at the time the other fields
	 * name. It decides which offset reads them back as a number of seconds since the epoch, which is why a
	 * time the C library was never told about reads against the zone's standard offset even on a day
	 * daylight saving is running.
	 */
	public enum DaylightSaving {
		IN_EFFECT,
		NOT_IN_EFFECT,
		/** The C {@code -1}: work it out from the zone. */
		UNKNOWN
	}

	/**
	 * Splits a number of seconds since the epoch into the parts of a UTC time, as {@code gmtime} does.
	 * <p>
	 * A fraction of a second stays with the seconds field. jq settles the two ends of a negative fractional
	 * epoch separately -- the date from the second the value falls short of, the fraction from the second
	 * below it -- and this does the same.
	 *
	 * @throws DateTimeException if the instant is outside the range a date can be written for
	 */
	public static BrokenDownTime ofEpochSecondsUtc(double epochSeconds) {
		BrokenDownTime time = split(epochSeconds, ZoneOffset.UTC);
		time.zoneAbbreviation = "GMT";
		return time;
	}

	/**
	 * Splits a number of seconds since the epoch into the parts of a time in {@code zone}, as
	 * {@code localtime} does.
	 *
	 * @throws DateTimeException if the instant is outside the range a date can be written for
	 */
	public static BrokenDownTime ofEpochSeconds(double epochSeconds, ZoneId zone) {
		return split(epochSeconds, zone);
	}

	private static BrokenDownTime split(double epochSeconds, ZoneId zone) {
		double whole = epochSeconds < 0 ? Math.ceil(epochSeconds) : Math.floor(epochSeconds);
		if (!(whole >= -EPOCH_SECONDS_LIMIT && whole <= EPOCH_SECONDS_LIMIT))
			throw new DateTimeException("seconds since the epoch out of range: " + epochSeconds);
		ZonedDateTime zoned = Instant.ofEpochSecond((long) whole).atZone(zone);

		BrokenDownTime time = new BrokenDownTime();
		time.year = zoned.getYear();
		time.month = zoned.getMonthValue() - 1;
		time.mday = zoned.getDayOfMonth();
		time.hour = zoned.getHour();
		time.min = zoned.getMinute();
		time.sec = zoned.getSecond();
		time.secFraction = epochSeconds - Math.floor(epochSeconds);
		time.wday = zoned.getDayOfWeek().getValue() % 7;
		time.yday = zoned.getDayOfYear() - 1;
		time.gmtOffset = zoned.getOffset().getTotalSeconds();
		time.zoneAbbreviation = abbreviate(ZONE_ABBREVIATION.format(zoned), time.gmtOffset);
		time.daylightSaving = zone.getRules().isDaylightSavings(zoned.toInstant())
				? DaylightSaving.IN_EFFECT
				: DaylightSaving.NOT_IN_EFFECT;
		return time;
	}

	/**
	 * The name the zone database gives an offset that has no abbreviation of its own, where Java writes one
	 * of its own making. The database writes such a zone as its offset -- {@code +14} on the hour,
	 * {@code +1245} otherwise -- and that is what {@code %Z} is expected to produce.
	 */
	static String abbreviate(String zoneName, int offsetSeconds) {
		if (!zoneName.startsWith(OFFSET_NAME_PREFIX) || zoneName.length() == OFFSET_NAME_PREFIX.length())
			return zoneName;
		int magnitude = Math.abs(offsetSeconds);
		String sign = offsetSeconds < 0 ? "-" : "+";
		int minutes = magnitude / 60 % 60;
		return minutes == 0
				? String.format(Locale.ROOT, "%s%02d", sign, magnitude / 3600)
				: String.format(Locale.ROOT, "%s%02d%02d", sign, magnitude / 3600, minutes);
	}

	/**
	 * Carries every out-of-range field into the one above it, the way {@code mktime} does, and recomputes
	 * {@link #wday} and {@link #yday} for the date that results.
	 *
	 * @throws DateTimeException if the carried date is outside the range a date can be written for
	 */
	public void normalize() {
		long minutes = min + Math.floorDiv(sec, 60);
		int secondsOfMinute = Math.floorMod(sec, 60);
		long hours = hour + Math.floorDiv(minutes, 60);
		int minutesOfHour = Math.floorMod(minutes, 60);
		long days = mday + Math.floorDiv(hours, 24);
		int hoursOfDay = Math.floorMod(hours, 24);

		LocalDate date = dateAt(year, month, days);

		year = date.getYear();
		month = date.getMonthValue() - 1;
		mday = date.getDayOfMonth();
		hour = hoursOfDay;
		min = minutesOfHour;
		sec = secondsOfMinute;
		wday = date.getDayOfWeek().getValue() % 7;
		yday = date.getDayOfYear() - 1;
	}

	/**
	 * Fills in {@link #wday} and {@link #yday} for the date the other fields name, as a parse does once it
	 * knows enough of the date to say.
	 * <p>
	 * Only the weekday is read off the calendar. The day of the year is counted from the start of the year
	 * the fields name, so a day 0 counts as {@code -1} rather than as the last day of the year before.
	 */
	public void computeWeekDayAndYearDay(boolean weekDay, boolean yearDay) {
		if (weekDay)
			wday = dateAt(year, month, mday).getDayOfWeek().getValue() % 7;
		if (yearDay)
			yday = MONTH_START_DAY[month] + (month >= 2 && IsoChronology.INSTANCE.isLeapYear(year) ? 1 : 0) + mday - 1;
	}

	/**
	 * Fills in {@link #month} and {@link #mday} from {@link #yday}, for a parse that was given a day of the
	 * year instead. Either field is left alone if the parse set it itself.
	 */
	public void deriveMonthAndDayFromYearDay(boolean setMonth, boolean setDay) {
		LocalDate date = LocalDate.ofYearDay(year, yday + 1);
		if (setMonth)
			month = date.getMonthValue() - 1;
		if (setDay)
			mday = date.getDayOfMonth();
	}

	/**
	 * The number of seconds since the epoch this time names, read as a wall-clock time in {@code zone}.
	 * <p>
	 * Out-of-range fields carry as {@link #normalize()} describes, without changing this time.
	 * {@link #daylightSaving} decides which of a zone's offsets reads them, so a time that never went
	 * through {@code localtime} reads against the standard one even in the middle of summer.
	 *
	 * @throws DateTimeException if the carried date is outside the range a date can be written for
	 */
	public long toEpochSeconds(ZoneId zone) {
		long minutes = min + Math.floorDiv(sec, 60);
		int secondsOfMinute = Math.floorMod(sec, 60);
		long hours = hour + Math.floorDiv(minutes, 60);
		int minutesOfHour = Math.floorMod(minutes, 60);
		long days = mday + Math.floorDiv(hours, 24);
		int hoursOfDay = Math.floorMod(hours, 24);

		LocalDateTime local = LocalDateTime.of(dateAt(year, month, days), LocalTime.of(hoursOfDay, minutesOfHour, secondsOfMinute));
		if (daylightSaving == DaylightSaving.UNKNOWN)
			return local.atZone(zone).toEpochSecond();

		ZoneRules rules = zone.getRules();
		Instant around = local.toInstant(rules.getOffset(local));
		ZoneOffset standard = rules.getStandardOffset(around);
		ZoneOffset offset = daylightSaving == DaylightSaving.IN_EFFECT
				? ZoneOffset.ofTotalSeconds(standard.getTotalSeconds() + (int) rules.getDaylightSavings(around).getSeconds())
				: standard;
		return local.toEpochSecond(offset);
	}

	/**
	 * The date {@code day} days into the {@code month}th month after the start of {@code year}, where
	 * neither the month nor the day has to be one the calendar has.
	 */
	private static LocalDate dateAt(long year, long month, long day) {
		long carriedYear = year + Math.floorDiv(month, 12);
		int monthOfYear = Math.floorMod(month, 12);
		if (carriedYear < LocalDate.MIN.getYear() || carriedYear > LocalDate.MAX.getYear())
			throw new DateTimeException("year out of range: " + carriedYear);
		return LocalDate.of((int) carriedYear, monthOfYear + 1, 1).plusDays(day - 1);
	}
}
