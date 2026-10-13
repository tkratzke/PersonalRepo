package com.skagit.advMath.mitLinearAlgebra;

import java.util.Arrays;

import com.skagit.utils.NumericalRoutines;

public class Svd extends MitLinearAlgebraCore {

	final public double[][] _a;
	final public double[][] _u;
	final public double[] _sigmas;
	final public double[][] _v;

	/**
	 * <pre>
	 * a is input and is m x n.
	 * _u will have orthonormal columns and be m x rank.
	 *    The m is forced, and we use rank for the intermediate dimension.
	 * _sigma will be a vector of length rank, and represent a
	 *    a rank x rank diagonal matrix.
	 * _v will be rank x n.
	 *    At this point, both dimensions are forced.
	 * Does not destroy a.
	 * </pre>
	 */
	public Svd(final double[][] a) {
		this(_Threshold, a);
	}

	public Svd(final double threshold, final double[][] a) {
		super(threshold);
		_a = a;
		final int n = _a[0].length;
		final double[][] aTa = multiply(NumericalRoutines.transpose(_a), _a);

		/**
		 * Note: aTa is n x n, and positive semi-definite, so the eigenvalues are
		 * non-negative, and there are n of them. Get them, sort them, and find the 1st
		 * non-zero. Its index is the nullity and the rank is simply n - nullity.
		 */
		final double[] sigmaSqs = getRealEigenvalues(aTa);
		Arrays.sort(sigmaSqs);
		final int nullity = NumericalRoutines.getLubIndex(sigmaSqs, _threshold * 1.001d);
		final int rank = n - nullity;

		/** Compute _v and _sigmas. */
		_v = new double[n][rank];
		_sigmas = new double[rank];
		for (int k = 0; k < rank;) {
			final double sigmaSq = sigmaSqs[nullity + k];
			/**
			 * Get and orthogonalize the eigenSpace for this eigenvalue sigmaSq of aTa. Then
			 * add its columns to _v.
			 */
			final double[][] aTaMinusSigmaSqI = deepClone(aTa);
			for (int j = 0; j < n; ++j) {
				aTaMinusSigmaSqI[j][j] -= sigmaSq;
			}
			final PaqLu paqLu = new PaqLu(aTaMinusSigmaSqI);
			final double[][] eigSpace = paqLu.getNullSpace();
			final Qr qr = new Qr(eigSpace);
			final double[][] q = qr.getQ();
			final int eigSpaceDim = q[0].length;
			/** Append each row of _v with the corresponding row of eigSpace. */
			for (int j = 0; j < n; ++j) {
				System.arraycopy(q[j], 0, _v[j], k, eigSpaceDim);
			}
			/** Add to _sigmas eigSpaceDim copies of sqrt(sigmaSq). */
			final double sigma = Math.sqrt(sigmaSq);
			for (int k1 = 0; k1 < eigSpaceDim; ++k1) {
				_sigmas[k++] = sigma;
			}
		}

		/**
		 * <pre>
		 * Compute _u:
		 * _a = _u * _sigma * _vT
		 * _u = _a * _v * sigmaInv
		 * Note that by making _sigma into a rank x rank,
		 * which we did by ignoring the 0 eigenvalues of
		 * aTa, _sigmaInv exists.
		 * </pre>
		 */
		final double[][] vSigmaInv = new double[n][];
		/** Multiply the jth column of _v by 1 / _sigmas[j] to get vSigmaInv. */
		for (int j = 0; j < n; ++j) {
			vSigmaInv[j] = new double[rank];
			for (int k = 0; k < rank; ++k) {
				vSigmaInv[j][k] = _v[j][k] / _sigmas[k];
			}
		}
		_u = multiply(_a, vSigmaInv);
	}

	/**
	 * <pre>
	 * Let CS and RS be the column space and row space of OFFSET_T.
	 * Suppose we're solving Ax = z for some z in CS, so that z = Ay
	 * for some y in Rn. We want to use x = PI(Ay), for some matrix PI.
	 * This means that PI must satisfy OFFSET_T*(PI*Ay) = Ay. This routine
	 * computes PI. Note that if OFFSET_T is m x n with rank r, PI will be
	 * n x m. and is a bijection from CS to RS.
	 * </pre>
	 */
	public double[][] getPseudoInverse() {
		final int m = _u.length, n = _v.length, rank = _u[0].length;
		final double[][] pi = new double[n][];
		for (int j = 0; j < n; ++j) {
			pi[j] = new double[m];
			for (int i = 0; i < m; ++i) {
				pi[j][i] = 0d;
				for (int k = 0; k < rank; ++k) {
					pi[j][i] += _v[j][k] / _sigmas[k] * _u[i][k];
				}
			}
		}
		return pi;
	}

	/** Debugging and testing. */
	public String getString() {
		final int m = _a.length, rank = _sigmas.length;

		/** Check the basic decomposition. */
		final double[][] vT = NumericalRoutines.transpose(_v);
		final double[][] uSigma = deepClone(_u);
		for (int j = 0; j < rank; ++j) {
			final double sigma = _sigmas[j];
			for (int i = 0; i < m; ++i) {
				uSigma[i][j] *= sigma;
			}
		}
		final double[][] uSigmaVtMinusA = multiply(uSigma, vT);
		pointwiseSubtract(uSigmaVtMinusA, _a);
		final String s_a = NumericalRoutines.getString(_a);
		final String s_uSigmaVtMinusA = NumericalRoutines.getString(uSigmaVtMinusA);

		/** Check that _u and _v have on clmns. */
		final double[][] uT = NumericalRoutines.transpose(_u);
		final double[][] uTu = multiply(uT, _u);
		final double[][] vTv = multiply(vT, _v);
		final String s_uTu = NumericalRoutines.getString(uTu);
		final String s_vTv = NumericalRoutines.getString(vTv);

		/** Check that OFFSET_T * PI * OFFSET_T = OFFSET_T. */
		final double[][] pi = getPseudoInverse();
		final double[][] aPiAMinusA = multiply(_a, pi, _a);
		pointwiseSubtract(aPiAMinusA, _a);
		final String s_pi = NumericalRoutines.getString(pi);
		final String s_aPiAMinusA = NumericalRoutines.getString(aPiAMinusA);

		/** The main checks. */
		final String s0 = String.format("OFFSET_T:%s\nUSigVt-OFFSET_T:%s\nPi:%s\nAPiA-OFFSET_T:%s\nuTu:%s\nvTv:%s", //
				s_a, s_uSigmaVtMinusA, s_pi, s_aPiAMinusA, s_uTu, s_vTv);

		/** Underlying information. */
		final String s_sigmas = NumericalRoutines.getString(_sigmas);
		final String s_u = NumericalRoutines.getString(_u);
		final String s_v = NumericalRoutines.getString(_v);
		final String s1 = String.format("\n\ns_sigmas:%s\nU:%s\nV:%s", s_sigmas, s_u, s_v);
		return s0 + s1;
	}

	@Override
	public String toString() {
		return getString();
	}

	public static void main(final String[] args) {
		final RandomMatrixBuilder rmb = new RandomMatrixBuilder();
		for (int k = 0;; ++k) {
			final double[][] a;
			if (k == 0) {
				a = rmb.getRandomMatrixFromSvd(/* m= */6, /* n= */8, /* sigmas= */ new double[] { 0, 0, 2, 2, 2, 3 });
			} else if (k == 1) {
				a = rmb.buildMatrix(/* m= */8, /* n= */6, /* indClmnArraySpec= */new int[] { 1, 2, 4 },
						/* upperTriangular= */false);
			} else if (k == 2) {
				a = new double[][] { //
						{ 2, 1, 0, 0 }, //
						{ 0, 2, 1, 0 }, //
						{ 0, 0, 2, 1 }, //
						{ 0, 0, 0, 2 } };
			} else if (k == 3) {
				a = rmb.getRandomMatrixFromSvd(/* m= */4, /* n= */4, /* sigmas= */ new double[] { 1, 2, 3, 4 });
			} else {
				a = null;
			}
			System.out.printf(new Svd(a).getString());
			System.exit(33);
		}
	}
}
