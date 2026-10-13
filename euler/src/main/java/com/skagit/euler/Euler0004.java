package com.skagit.euler;

import java.util.ArrayList;

public class Euler0004 {

    /**
     * Find the largest palindromic number that is the product of two 3 digit
     * numbers.
     */
    public static void main(final String[] args) {
	int k0Winner = 0, k1Winner = 0, winner = 0;
	for (int k00 = 1; k00 <= 9; ++k00) {
	    for (int k01 = 0; k01 <= 9; ++k01) {
		for (int k02 = 0; k02 <= 9; ++k02) {
		    final int k0 = k00 * 100 + k01 * 10 + k02;
		    for (int k10 = 1; k10 <= 9; ++k10) {
			for (int k11 = 0; k11 <= 9; ++k11) {
			    for (int k12 = 0; k12 <= 9; ++k12) {
				final int k1 = k10 * 100 + k11 * 10 + k12;
				final int product = k0 * k1;
				if (isPalindrome(product)) {
				    System.out.printf("\n%d * %d = %d", k0, k1, product);
				    if (product > winner) {
					k0Winner = k0;
					k1Winner = k1;
					winner = product;
				    }
				}
			    }
			}
		    }
		}
	    }
	}
	System.out.printf("\n\nGrand Winner:\n%d * %d = %d", k0Winner, k1Winner, winner);
    }

    public static void main1(final String[] args) {
	final int[] toCheck = { 132, 131, 1331, 1321 };
	for (final int n : toCheck) {
	    System.out.printf("\nn[%d] isPalindrome[%b]", n, isPalindrome(n));
	}
    }

    private static boolean isPalindrome(int n) {
	final ArrayList<Integer> digits = new ArrayList<>();
	while (n > 0) {
	    digits.add(n % 10);
	    n /= 10;
	}
	final int nDigits = digits.size();
	for (int k = 0; k < nDigits / 2; ++k) {
	    final int digitK = digits.get(k);
	    final int digitKPrime = digits.get(nDigits - 1 - k);
	    if (digitK != digitKPrime) {
		return false;
	    }
	}
	return true;
    }
}
