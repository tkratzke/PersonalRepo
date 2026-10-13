package com.skagit.euler;

import com.skagit.euler.utils.Eratosthenes;

public class Euler0010 {

    final static private int _N = 2000000;

    public static void main(final String[] args) {
	long sum = 0;
	for (int p = 2; 0 <= p && p < _N; p = Eratosthenes.nextPrimeAfter(p + 1)) {
	    sum += p;
	}
	System.out.printf("Sum = %d.", sum);
    }
}
