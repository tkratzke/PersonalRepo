package com.skagit.euler.euler0781;

import java.util.Arrays;

public class Euler0781 {
    final static int _Modulo = 1000000007;

    public static int feynmanF(final int n) {
	final int[] alfa = new int[n + 1];
	Arrays.fill(alfa, 1);
	for (int bravoN = n - 2; bravoN > 0; bravoN -= 2) {
	    int cum = 0;
	    for (int i = 1; i <= bravoN; ++i) {
		cum = (cum + alfa[i]) % _Modulo;
		alfa[i] = (int) ((alfa[i + 2] * (i + 1L) + cum) % _Modulo);
	    }
	}
	return alfa[2];
    }

    public static void main(final String[] args) {
	final int n = 50000;
	final long start = System.currentTimeMillis();
	final int feynmanF = feynmanF(n);
	final long stop = System.currentTimeMillis();
	System.out.printf("n = %d, FeynmanF = %d, took %d millis.", n, feynmanF, stop - start);
    }
}
