package com.skagit.euler;

public class Euler0006 {

    /** Find (Sum(k): k)-squared minus Sum(k): k-squared. */
    public static void main(final String[] args) {
	final int n = 100;
	final int s1 = s1(n), s2 = s2(n), diff = Math.abs(s1 - s2);
	System.out.printf("\nn[%d] s1[%d] s2[%d], diff[%d]", n, s1, s2, diff);
    }

    private static int s1(final int n) {
	int sum = 0;
	for (int k = 1; k <= n; ++k) {
	    sum += k * k;
	}
	return sum;
    }

    private static int s2(final int n) {
	int sum = 0;
	for (int k = 1; k <= n; ++k) {
	    sum += k;
	}
	return sum * sum;
    }
}
