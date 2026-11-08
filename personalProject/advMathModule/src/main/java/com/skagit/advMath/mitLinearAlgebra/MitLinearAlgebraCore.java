package com.skagit.advMath.mitLinearAlgebra;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

public class MitLinearAlgebraCore {

	protected final static double _Threshold = 5.0e-11;

	protected final double _threshold;

	MitLinearAlgebraCore() {
		this(_Threshold);
	}

	MitLinearAlgebraCore(final double threshold) {
		_threshold = threshold > 0d ? threshold : _Threshold;
	}

	/** Static routines for double[][]. */
	static int getNVectors(final double[][] vectors) {
		final int n = vectors == null ? 0 : vectors.length;
		/** The 1st null one indicates how many we really have. */
		for (int j = 0; j < n; ++j) {
			if (vectors[j] == null) {
				return j;
			}
		}
		return n;
	}

	static double[] getClmn(final double[][] a, final int j) {
		final int m = a == null ? 0 : a.length;
		final int n = (m == 0 || a[0] == null) ? 0 : a[0].length;
		if (j < 0 || j >= n) {
			return null;
		}
		final double[] clmn = new double[m];
		for (int i = 0; i < m; ++i) {
			clmn[i] = a[i][j];
		}
		return clmn;
	}

	/**
	 * Only works for a positive semi-definite matrix. Hence, we call the incoming
	 * argument aTa.
	 */
	final static double[] getRealEigenvalues(final double[][] aTa) {
		final Matrix aTaMatrix = new Matrix(aTa);
		final EigenvalueDecomposition eig = aTaMatrix.eig();
		return eig.getRealEigenvalues();
	}

	/** Straightforward; expand across the top row. */
	final public static double getExpandAcrossTopRowDeterminant(final double[][] a) {
		final int m = a.length, n = a[0].length;
		if (m != n) {
			return 0d;
		}
		if (m == 1) {
			return a[0][0];
		}
		double d = 0d;
		for (int j = 0; j < n; ++j) {
			final double[][] aPrime = new double[n - 1][];
			for (int i = 0; i < m - 1; ++i) {
				aPrime[i] = new double[n - 1];
				for (int j1 = 0; j1 < n; ++j1) {
					if (j1 < j) {
						aPrime[i][j1] = a[i + 1][j1];
					} else if (j1 > j) {
						aPrime[i][j1 - 1] = a[i + 1][j1];
					}
				}
			}
			d += a[0][j] * (j % 2 == 0 ? 1 : -1) * getExpandAcrossTopRowDeterminant(aPrime);
		}
		return d;
	}

}
