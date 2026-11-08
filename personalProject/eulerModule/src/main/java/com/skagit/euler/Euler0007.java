package com.skagit.euler;

import java.util.BitSet;

public class Euler0007 {

    static private int _N = 10001;

    static int getNthPrime(final int n) {
	/** From wiki, we get that a good guess for the nth prime is n*ln(n). */
	for (int k = (int) (n / Math.log(n));; k *= 2) {
	    final BitSet bitSet = getPrimesUpTo(k);
	    if (bitSet.cardinality() >= _N) {
		int prime = bitSet.nextSetBit(0);
		for (int j = 1; j < n; prime = bitSet.nextSetBit(prime + 1), ++j) {
		}
		return prime;
	    }
	}
    }

    /** The sieve. */
    private static BitSet getPrimesUpTo(final int n) {
	final BitSet primes = new BitSet(1 + n);
	primes.set(2, n + 1);
	for (int k = primes.nextSetBit(2); k >= 0; k = primes.nextSetBit(k + 1)) {
	    final int upperLimit = n - k;
	    for (int kk = k; kk <= upperLimit;) {
		kk += k;
		primes.clear(kk);
	    }
	}
	return primes;
    }

    public static void main(final String[] args) {
	final int answer = getNthPrime(_N);
	System.out.printf("%d-th prime is %d.", _N, answer);
    }

}
