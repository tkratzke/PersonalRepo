package com.skagit.advMath;

/**
 * <pre>
 * _m and _n do not need to be positive. Fills in _r, _s, and _gcd, where:
 *   _gcd = greatest common (positive) divisor, and
 *   _gcd = (_r * _m) + (_s * n).
 * </pre>
 */
public class Gcd {
	final long _m, _n;
	final long _r, _s, _gcd;

	public Gcd(final long m, final long n) {
		_m = m;
		_n = n;
		/** Find rs. */
		long[] rsAndRhs0 = { 1L, 0L, _m };
		long[] rsAndRhs1 = { 0L, 1L, _n };
		for (;;) {
			final long rhs0 = rsAndRhs0[2];
			final long rhs1 = rsAndRhs1[2];
			final long rhs = Math.floorMod(rhs0, rhs1);
			if (rhs == 0L) {
				break;
			}
			final long quot = (rhs0 - rhs) / rhs1;
			final long[] rsAndRhs = { //
					rsAndRhs0[0] - quot * rsAndRhs1[0], //
					rsAndRhs0[1] - quot * rsAndRhs1[1], //
					rhs //
			};
			rsAndRhs0 = rsAndRhs1;
			rsAndRhs1 = rsAndRhs;
		}
		/** We want gcd to be positive. */
		if (rsAndRhs1[2] < 0) {
			rsAndRhs1[0] = -rsAndRhs1[0];
			rsAndRhs1[1] = -rsAndRhs1[1];
			rsAndRhs1[2] = -rsAndRhs1[2];
		}
		_r = rsAndRhs1[0];
		_s = rsAndRhs1[1];
		_gcd = rsAndRhs1[2];
	}

	public long getR() {
		return _r;
	}

	public long getS() {
		return _s;
	}

	public long getGcd() {
		return _gcd;
	}

	/**
	 * <pre>
	 * Solves _m * x = target(mod _n) for x.
	 * The returned long[2] = [x0,period] specifies
	 *   the solution set {x0 + k*period | k in Z}.
	 * If target is not divisible by _gcd, this returns null.
	 * </pre>
	 */
	public long[] getX0AndPeriod(final long target) {
		final long gcd = getGcd();
		if (target % gcd != 0L) {
			return null;
		}
		/** Deflate target and work with n = _n/gcd. */
		final long period = _n / gcd;
		final long tgt = Math.floorMod(target / gcd, period);
		final long absPeriod = Math.abs(period);
		final long x0 = Math.floorMod(getR() * tgt, absPeriod);
		return new long[] { x0, absPeriod };
	}

	public String getString() {
		final String returnValue = String.format(//
				"m[%d] n[%d], gcd[%d] = %d*%d + %d*%d, check[%d]", //
				_m, _n, getGcd(), //
				getR(), _m, getS(), _n, getR() * _m + getS() * _n);
		return returnValue;
	}

	@Override
	public String toString() {
		return getString();
	}

	/** Used to create test _TestCaseStrings. */
	private static long factorsToLong(final long[] factors) {
		long k = 1;
		for (final long f : factors) {
			k *= f;
		}
		return k;
	}

	public static void main(final String[] args) {
		final long[] mFactors = { 2L, 3L };
		final long[] nFactors = { 2L, 3L, 5L, 11L, 17L, 23L };
		final long[] targetFactors = { 2L, 3L, 5L, 7L };
		final long m0 = factorsToLong(mFactors);
		final long n0 = factorsToLong(nFactors);
		final long target0 = factorsToLong(targetFactors);
		for (int k0 = 0; k0 < 8; ++k0) {
			final long m = k0 / 2 == 0 ? m0 : -m0;
			final long n = (k0 / 2) % 2 == 0 ? n0 : -n0;
			final long target = k0 % 2 == 0 ? target0 : -target0;
			final Gcd gcd = new Gcd(m, n);
			System.out.printf("\n\n%s", gcd.getString());
			final long[] x0AndPeriod = gcd.getX0AndPeriod(target);
			/** Test: */
			final long x0 = x0AndPeriod[0], period = x0AndPeriod[1];
			for (long k1 = 0; k1 < 2; ++k1) {
				final long preMod = (x0 + k1 * period) * m;
				final long mod = Math.floorMod(preMod, n);
				System.out.printf( //
						"\ntarget[%d] x0[%d] period[%d] k[%d], (x0 + k*period)*%d = %d followed by floorMod(*,%d) yields %d.", //
						Math.floorMod(target, n), x0, period, k1, m, preMod, n, mod //
				);
			}
		}
	}
}
