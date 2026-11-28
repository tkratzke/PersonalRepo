package com.skagit.miscPuzzles;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * <pre>
 * https://www.calculator.net/day-of-the-week-calculator.html?today=12%2F12%2F1900&x=27&y=24
 * https://en.wikipedia.org/wiki/Doomsday_rule
 * </pre>
 */

public class MyDayOfWeek {
	private static final DateTimeFormatter _Formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	final private static int[] _KeyDaysOfMonth = { -1, // Dummy so we can use 1-12 to index this array.
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

	final private static DayOfWeek[] _Anchors = { //
			DayOfWeek.TUESDAY, // 2000
			DayOfWeek.SUNDAY, // 1700
			DayOfWeek.FRIDAY, // 1800
			DayOfWeek.WEDNESDAY // 1900
	};

	final LocalDate _localDate;
	final int _anchorInt;
	final int _dd;
	final int _keyDayOfMonth;
	final int _q, _r0, _r1;
	final DayOfWeek _answer;

	private MyDayOfWeek(final LocalDate localDate) {
		_localDate = localDate;
		final int yyyy = _localDate.getYear();
		final int oneToTwelve = _localDate.getMonth().getValue();
		_dd = _localDate.getDayOfMonth();

		_anchorInt = _Anchors[(yyyy / 100) % 4].get(ChronoField.DAY_OF_WEEK);

		/** Get keyDayOfMonth and the shift due to leap years: */
		final boolean isLeapYear;
		if (yyyy % 4 != 0) {
			isLeapYear = false;
		} else if (yyyy % 100 == 0) {
			isLeapYear = yyyy % 400 == 0;
		} else {
			isLeapYear = true;
		}
		final boolean needAdjustment = isLeapYear && oneToTwelve <= 2;
		_keyDayOfMonth = _KeyDaysOfMonth[oneToTwelve] + (needAdjustment ? 1 : 0);

		/** Component from year within century: */
		final int yy = yyyy % 100;
		_q = yy / 12;
		_r0 = yy % 12;
		_r1 = _r0 / 4;

		/** Grand sum: */
		final int grandSum0 = _anchorInt + _dd - _keyDayOfMonth + _q + _r0 + _r1;
		final int grandSum1 = (grandSum0 - 1) % 7 + 1;
		final int oneToSeven = grandSum1 <= 0 ? (grandSum1 + 7) : grandSum1;
		_answer = DayOfWeek.of(oneToSeven);
	}

	private String getString() {
		final int yyyy = _localDate.getYear();
		final int oneToTwelve = _localDate.getMonth().getValue();
		final int dd = _localDate.getDayOfMonth();
		final String anchor = DayOfWeek.of(_anchorInt).getDisplayName(TextStyle.SHORT, Locale.US);

		return String.format("%s. Anchr[%s] dd/key[%2d/%2d:%d] q/r0,r1[%2d/%2d/%d:%d]:: %s!", //
				_Formatter.format(_localDate), //
				anchor, //
				_dd, _keyDayOfMonth, getDdEffect(yyyy, oneToTwelve, dd), //
				_q, _r0, _r1, (_q + _r0 + _r1) % 7, //
				_answer.getDisplayName(TextStyle.SHORT, Locale.US) //
		);
	}

	private static int getDdEffect(final int yyyy, final int oneToTwelve, final int dd) {
		final int keyDayOfMonth = _KeyDaysOfMonth[oneToTwelve];
		int shift = 0;
		if (yyyy % 4 == 0 && oneToTwelve <= 2) {
			if ((yyyy % 100 != 0) || (yyyy % 400 == 0)) {
				shift = 1;
			}
		}
		return (7 + (dd - keyDayOfMonth - shift) % 7) % 7;
	}

	@Override
	public String toString() {
		return getString();
	}

	private static void processLocalDate(final LocalDate localDate) {
		final String dateString = _Formatter.format(localDate);
		final DayOfWeek dayOfWeek0 = localDate.getDayOfWeek();
		final MyDayOfWeek myDayOfWeek = new MyDayOfWeek(localDate);
		final DayOfWeek dayOfWeek1 = myDayOfWeek._answer;
		System.out.printf("\n%s", myDayOfWeek.getString());
		if (dayOfWeek0 != dayOfWeek1) {
			System.err.printf("\nFail: %s yielded %s instead of %s.", dateString,
					dayOfWeek1.getDisplayName(TextStyle.SHORT, Locale.US),
					dayOfWeek0.getDisplayName(TextStyle.SHORT, Locale.US));
		}
	}

	public static void main0(final String[] args) {
		final String[] dateStrings = { //
				"1900-01-01", //
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
			processLocalDate(localDate);
		}
	}

	public static void main(final String[] args) {
		final LocalDate startDate = LocalDate.of(1900, 1, 1);
		final LocalDate endDate = LocalDate.now();
		final long nDays = ChronoUnit.DAYS.between(startDate, endDate);
		for (long k = 0; k <= nDays; k++) {
			processLocalDate(startDate.plusDays(k));
		}
	}
}
