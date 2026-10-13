package com.skagit.euler;

import com.skagit.euler.utils.Eratosthenes;

public class Euler0012 {

    private static int getNDivisorsForTriangularNumber(final int k) {
	final int a, b;
	if (k % 2 == 0) {
	    a = k / 2;
	    b = k + 1;
	} else {
	    a = k;
	    b = (k + 1) / 2;
	}
	final long[] aDivisors = Eratosthenes.getAllDivisors(a);
	final long[] bDivisors = Eratosthenes.getAllDivisors(b);
	final int nADivisors = aDivisors.length;
	final int nBDivisors = bDivisors.length;
	return nADivisors * nBDivisors;
    }

    public static void main(final String[] args) {
	final int nDivisors0 = 500;
	for (int k = 1;; ++k) {
	    final int nDivisors = getNDivisorsForTriangularNumber(k);
	    System.out.printf("\nk=%d \t\tnDivisors=%d \t\ttri#=%d", k, nDivisors, (k * (k + 1L)) / 2L);
	    if (nDivisors > nDivisors0) {
		break;
	    }
	}
    }

}
