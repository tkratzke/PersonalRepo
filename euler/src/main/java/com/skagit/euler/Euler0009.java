package com.skagit.euler;

public class Euler0009 {

    public static void main(final String[] args) {
	for (int a = 1;; ++a) {
	    final int aa = a * a;
	    for (int b = a + 1;; ++b) {
		final int aabb = aa + b * b;
		final int c = (int) Math.floor(Math.sqrt(aabb));
		final int sum = a + b + c;
		if (sum > 1000) {
		    /** b is too big. Break out of its loop. */
		    break;
		}
		if (sum == 1000 && c * c == aabb) {
		    System.out.printf("a=%d, b=%d, c=%d, abc = %d.", a, b, c, a * b * c);
		    System.exit(0);
		}
	    }
	}
    }
}
