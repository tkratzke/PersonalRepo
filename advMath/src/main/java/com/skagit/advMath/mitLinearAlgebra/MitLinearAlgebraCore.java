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
			if (vectors[j] == null)
				return j;
		}
		return n;
	}

	static double[] getClmn(final double[][] a, final int j) {
		final int m = a == null ? 0 : a.length;
		final int n = (m == 0 || a[0] == null) ? 0 : a[0].length;
		if (j < 0 || j >= n)
			return null;
		final double[] clmn = new double[m];
		for (int i = 0; i < m; ++i) {
			clmn[i] = a[i][j];
		}
		return clmn;
	}

	static double[][] deepClone(final double[][] a) {
		if (a == null)
			return null;
		final int n = a.length;
		final double[][] clone = new double[n][];
		for (int i = 0; i < n; ++i) {
			clone[i] = a[i].clone();
		}
		return clone;
	}

	static void pointwiseSubtract(final double[][] mtx1, final double[][] mtx2) {
		final int m = Math.min(mtx1.length, mtx2.length);
		final int n = Math.min(mtx1[0].length, mtx2[0].length);
		for (int i = 0; i < m; ++i) {
			for (int j = 0; j < n; ++j) {
				mtx1[i][j] -= mtx2[i][j];
			}
		}
	}

	/** Multiply routines. */
	static void multiply(final double scalar, final double[] a) {
		final int n = a == null ? 0 : a.length;
		for (int i = 0; i < n; ++i) {
			a[i] *= scalar;
		}
	}

	static double[][] multiply(final double[][] a, final double[][] b) {
		final int m = a == null ? 0 : a.length;
		if (m == 0)
			return new double[][] { {} };
		final int n = (b == null || b.length == 0) ? 0 : b[0].length;
		if (n == 0)
			return new double[][] { {} };
		final int mn = a[0].length;
		if (mn == 0 || mn != b.length)
			return null;
		final double[][] result = new double[m][];
		for (int i = 0; i < m; ++i) {
			result[i] = new double[n];
			for (int j = 0; j < n; ++j) {
				result[i][j] = 0d;
				for (int k = 0; k < mn; ++k) {
					result[i][j] += a[i][k] * b[k][j];
				}
			}
		}
		return result;
	}

	static double[][] multiply(final double[][] a, final double[][] b, final double[][] c) {
		final int m = a == null ? 0 : a.length;
		if (m == 0)
			return new double[][] { {} };
		final int n = (c == null || c.length == 0) ? 0 : c[0].length;
		if (n == 0)
			return new double[][] { {} };
		final int middle1 = a[0].length;
		if (middle1 == 0)
			return new double[][] { {} };
		final int nBRows = b == null ? 0 : b.length;
		if (nBRows != middle1)
			return null;
		final int middle2 = (b == null || b.length == 0) ? 0 : b[0].length;
		final int nCRows = c == null ? 0 : c.length;
		if (nCRows != middle2)
			return null;
		final double[][] result = new double[m][n];
		for (int i = 0; i < m; ++i) {
			for (int j = 0; j < n; ++j) {
				result[i][j] = 0;
				for (int k1 = 0; k1 < middle1; ++k1) {
					for (int k2 = 0; k2 < middle2; ++k2) {
						result[i][j] += a[i][k1] * b[k1][k2] * c[k2][j];
					}
				}
			}
		}
		return result;
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
	final public static double getDeterminant(final double[][] a) {
		final int m = a.length, n = a[0].length;
		if (m != n)
			return 0d;
		if (m == 1)
			return a[0][0];
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
			d += a[0][j] * (j % 2 == 0 ? 1 : -1) * getDeterminant(aPrime);
		}
		return d;
	}

}
