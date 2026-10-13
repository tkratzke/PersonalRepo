package com.skagit.euler;

public class Euler0002 {

    /** Find the sum of the even Fibonacci numbers that are at most 4,000,000. */

    public static void main(final String[] args) {
	/**
	 * Build Finonacci numbers until we find one bigger than maxFib, and add the
	 * even ones.
	 */
	final int maxFib = 4000000;
	int sum = 0;
	for (long fib1 = 1, fib2 = 1, k = 0; fib2 <= Long.MAX_VALUE - fib1; ++k) {
	    final long fib = fib1 + fib2;
	    if (fib > maxFib) {
		break;
	    }
	    if (fib % 2 == 0) {
		sum += fib;
		System.out.printf("\nFib # %d, Fib Sub %d", k, sum);
	    }
	    fib1 = fib2;
	    fib2 = fib;
	}
    }
}
