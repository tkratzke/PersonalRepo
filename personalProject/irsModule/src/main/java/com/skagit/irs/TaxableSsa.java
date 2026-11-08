package com.skagit.irs;

import java.util.Arrays;

public class TaxableSsa {
	final static double BcktClng0 = 32000, BcktClng1 = 44000;

	public static double GetTaxableSSA0(final double cmbndIncm, final double ttlSsa) {

		double taxable;
		if (cmbndIncm <= BcktClng0) {
			taxable = 0;
		} else if (cmbndIncm <= BcktClng1) {
			taxable = Math.min(0.5 * (cmbndIncm - BcktClng0), 0.5 * ttlSsa);
		} else {
			final double part1 = 0.5 * (BcktClng1 - BcktClng0);
			final double part2 = 0.85 * (cmbndIncm - BcktClng1);
			taxable = Math.min(part1 + part2, 0.85 * ttlSsa);
		}
		return taxable;
	}

	public static double GetTaxableSSA1(final double[] bcktClngs, final double[] rts, final double cmbndIncm,
			final double ttlSsa) {
		if (cmbndIncm <= 0d) {
			return 0d;
		}
		/** Find the bucket that cmbndIncm falls into. */
		int bcktIdx = Arrays.binarySearch(bcktClngs, cmbndIncm);
		if (bcktIdx < 0) {
			bcktIdx = -bcktIdx - 1;
		}
		/**
		 * txblDllrsBsdOnCmbndIncm has nothing to with ttlSsa; it's based purely on
		 * cmbndIncm.
		 */
		double txblDllrsBsdOnCmbndIncm = 0d;
		double thisRt = 0d;
		for (int k = 0; k <= bcktIdx; ++k) {
			final double bcktFloor = k == 0 ? 0d : bcktClngs[k - 1];
			final double ceiling = k == bcktIdx ? cmbndIncm : bcktClngs[k];
			final double amtForThisRt = Math.min(ttlSsa, ceiling - bcktFloor);
			thisRt = rts[k];
			txblDllrsBsdOnCmbndIncm += thisRt * amtForThisRt;
			if (ceiling == cmbndIncm) {
				break;
			}
		}
		final double txblDllrsBsdOnSsaAndRt = thisRt * ttlSsa;
		return Math.min(txblDllrsBsdOnSsaAndRt, txblDllrsBsdOnCmbndIncm);
	}

	public static void main(final String[] args) {
		final double[] ssa = { 20000d };
		final double[] addtl = { 40000d, 60000d };
		final double[] bcktClngs = { BcktClng0, BcktClng1 };
		final double[] rts = { 0d, 0.5d, 0.85d };
		for (int k0 = 0; k0 < ssa.length; ++k0) {
			final double ttlSsa = ssa[k0];
			for (int k1 = 0; k1 < addtl.length; ++k1) {
				final double addtlIncm = addtl[k1];
				final double cmbndIncm = ttlSsa / 2d + addtlIncm;
				final double taxable0 = GetTaxableSSA0(cmbndIncm, ttlSsa);
				final double taxable1 = GetTaxableSSA1(bcktClngs, rts, cmbndIncm, ttlSsa);
				System.out.printf("\nSSA[%.2f] AddlIncm[%.2f], taxable0[%.2f(%.2f%%)] taxable1[%.2f(%.2f%%)]", ttlSsa,
						addtlIncm, taxable0, 100d * taxable0 / ttlSsa, taxable1, 100d * taxable1 / ttlSsa);
			}
		}
	}
}
