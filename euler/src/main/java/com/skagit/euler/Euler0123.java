package com.skagit.euler;

import com.skagit.euler.utils.Eratosthenes;

public class Euler0123 {

    final private static int _PrimesCardinality = Eratosthenes.getNPrimes();

    public static void main(final String[] args) {
	final long bigR = 10000000000L;
	/**
	 * <pre>
	 * We're only interested in odd values of n. We binary search on k,
	 * and we will set n = 2*k + 1.
	 * </pre>
	 */
	int tooLowK = 5;
	int highEnoughK = (_PrimesCardinality - 1) / 2;
	while (highEnoughK >= tooLowK + 2) {
	    final int k = tooLowK + (highEnoughK - tooLowK) / 2;
	    final int n = 2 * k + 1;
	    final int pn = Eratosthenes.getKthPrime(n);
	    final long r = 2L * n * pn;
	    if (r >= bigR) {
		highEnoughK = k;
	    } else {
		tooLowK = k;
	    }
	}
	final int n = 2 * highEnoughK + 1;
	System.out.printf("\nn for %d is %d.", bigR, n);
    }

}
