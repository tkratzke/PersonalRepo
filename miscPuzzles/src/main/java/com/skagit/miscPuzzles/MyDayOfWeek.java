package com.skagit.miscPuzzles;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.util.Locale;

/**
 * <pre>
 * https://www.calculator.net/day-of-the-week-calculator.html?today=12%2F12%2F1900&x=27&y=24
 * https://en.wikipedia.org/wiki/Doomsday_rule
 * </pre>
 */

public class MyDayOfWeek {

	final private static int[] _KeyDaysOfMonth = new int[] { -1, // Dummy so we can use 1-12 to index this array.
			31, // (or 32; Last day of January, assuming 32 days in a leap year).
			28, // (or 29; Last day of February).
			0, // (Just remember this one).
			4, // (4/4, 6/6, 8/8, 10/10, and 12/12).
			9, // (9-5 at the 7-11).
			6, // (4/4, 6/6, 8/8, 10/10, and 12/12).
			11, // (9-5 at the 7-11).
			8, // (4/4, 6/6, 8/8, 10/10, and 12/12).
			5, // (9-5 at the 7-11).
			10, // (4/4, 6/6, 8/8, 10/10, and 12/12).
			7, // (9-5 at the 7-11).
			12, // (4/4, 6/6, 8/8, 10/10, and 12/12).
	};

	final private static DayOfWeek[] _Anchors = { DayOfWeek.TUESDAY, DayOfWeek.SUNDAY, DayOfWeek.FRIDAY,
			DayOfWeek.WEDNESDAY };

	/** Monday=1, ... Sunday=7. */
	public static int GetOneToSeven(final int yyyy, final int oneToTwelve, final int dd) {

		/** Compute the shift from dayOfMonth. */
		int keyDayOfMonth = _KeyDaysOfMonth[oneToTwelve];
		if (oneToTwelve <= 2 && (yyyy % 4 == 0) && (yyyy % 100 != 0 || yyyy % 400 == 0)) {
			++keyDayOfMonth;
		}
		final int dayOfMonthShift = dd - keyDayOfMonth;

		/** Compute the shift from year within century. */
		final int yy = yyyy % 100;
		final int q1 = yy / 12, r1 = yy % 12, r2 = r1 / 4;
		final int twelveShift = q1 + r1 + r2;

		/** Use the century anchor and the above two shifts to set oneToSeven. */
		final int anchor = _Anchors[(yyyy / 100) % 4].get(ChronoField.DAY_OF_WEEK);
		int oneToSeven = anchor + dayOfMonthShift + twelveShift;
		oneToSeven = (oneToSeven - 1) % 7 + 1;
		return oneToSeven + (oneToSeven > 0 ? 0 : 7);
	}

	private static final DateTimeFormatter _Formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	public static void main(final String[] args) {
		final String[] dateStrings = new String[] { "1900-01-01", //
				"1900-01-02", //
				"1900-02-28", //
				"1900-03-01", //
				"2000-02-29", //
				"1941-12-07", //
				"1948-04-04", //
				"1953-10-29", //
				"1955-04-18", //
				"1963-11-22", //
				"1979-12-29", //
				"1980-05-18", //
				"1982-08-13", //
				"1985-03-02", //
				"1991-01-05", //
				"2000-01-01", //
				"2001-09-11", //
				"2023-03-03", //
		};
		final int nDateStrings = dateStrings.length;

		for (int k = 0; k < nDateStrings; ++k) {
			final String dateString = dateStrings[k];
			final LocalDate localDate = LocalDate.parse(dateString, _Formatter);
			final DayOfWeek dayOfWeek0 = localDate.getDayOfWeek();
			final DayOfWeek dayOfWeek1 = DayOfWeek.of(GetOneToSeven( //
					localDate.getYear(), localDate.getMonth().getValue(), localDate.getDayOfMonth() //
			));
			if (dayOfWeek0 != dayOfWeek1) {
				System.err.printf("\nFail: %s yielded %s instead of %s.", dateString,
						dayOfWeek1.getDisplayName(TextStyle.FULL, Locale.US),
						dayOfWeek0.getDisplayName(TextStyle.FULL, Locale.US));
			} else {
				System.out.printf("\n%s: %s.", dateString, dayOfWeek1.getDisplayName(TextStyle.FULL, Locale.US));
			}
		}
	}
}
