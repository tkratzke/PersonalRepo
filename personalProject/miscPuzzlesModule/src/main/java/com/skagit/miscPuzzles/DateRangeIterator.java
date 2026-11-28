package com.skagit.miscPuzzles;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class DateRangeIterator {

	public static void main(final String[] args) {
		final LocalDate startDate = LocalDate.of(1900, 1, 1);
		final LocalDate endDate = LocalDate.now(); // Today's date

		final long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
		for (long i = 0; i <= daysBetween; i++) {
			final LocalDate currentDate = startDate.plusDays(i);
			System.out.println(currentDate);
		}
	}
}
