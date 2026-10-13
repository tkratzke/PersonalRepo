package com.skagit.advMath.mitLinearAlgebra;

import java.util.ArrayList;

import com.skagit.utils.NumericalRoutines;

public class Qr extends MitLinearAlgebraCore {

	final public double[][] _a;
	final public double[][] _q;
	final public double[][] _r;

	/**
	 * <pre>
	 * a is input and is m x n.
	 * _q will be m x rank
	 *    The m is forced, and we use rank for the intermediate dimension.
	 * _r will be rank x n
	 *    At this point, both dimensions are forced.
	 * Does not destroy a.
	 * </pre>
	 */
	public Qr(final double[][] a) {
		this(_Threshold, a);
	}

	public Qr(final double threshold, final double[][] a) {
		super(threshold);
		_a = a;
		final int m = _a.length, n = _a[0].length;
		final ArrayList<double[]> qClmns = new ArrayList<>();
		final double[][] rClmns = new double[n][];
		for (int j0 = 0; j0 < n; ++j0) {
			/** aj0 is a copy of _a's j0-th column, but stored as a single row. */
			final double[] aj0 = getClmn(_a, j0);
			/** Deflate aj0 with the preceding q's until we have a new q. */
			final int nQClmns = qClmns.size();
			final double[] rClmn = rClmns[j0] = new double[nQClmns + 1];
			for (int j = 0; j < nQClmns; ++j) {
				final double[] qj = qClmns.get(j);
				final double aJ0DotQj = NumericalRoutines.dotProduct(aj0, qj);
				rClmn[j] = aJ0DotQj;
				for (int i = 0; i < m; ++i) {
					aj0[i] -= rClmn[j] * qj[i];
				}
			}
			final double magnitude = NumericalRoutines.convertToUnitLength(aj0);
			if (magnitude > _threshold) {
				rClmn[nQClmns] = magnitude;
				qClmns.add(aj0);
			} else {
				/**
				 * aj0 is not worth having its own QClmn so we do not add a new QClmn. rClmn was
				 * allocated room to hold a coefficient for such a QClmn and we need to put a 0
				 * in it since it would refer to the next QClmn.
				 */
				rClmn[nQClmns] = 0d;
			}
		}

		/** Transpose qClmns into _q and create _r. */
		final int rank = qClmns.size();
		final double[][] qT = qClmns.toArray(new double[rank][]);
		_q = NumericalRoutines.transpose(qT);
		_r = new double[rank][n];
		for (int j = 0; j < n; ++j) {
			final double[] rClmn = rClmns[j];
			for (int k = 0; k < rank; ++k) {
				_r[k][j] = k < rClmn.length ? rClmn[k] : 0d;
			}
		}
	}

	public double[][] getNullSpace() {
		final UpperTriangularSolver uts = new UpperTriangularSolver(_threshold, _r, //
				/* rhsRows= */ null);
		return uts._xRows;
	}

	/** Linear Least Squares solve for Ax ~ b. */
	public double[][] solve(final double[][] rhsRows) {
		final int m = _q.length;
		if (rhsRows == null || rhsRows.length != m || rhsRows[0] == null || rhsRows[0].length == 0) {
			return new double[0][];
		}

		/**
		 * <pre>
		 * Multiply qT * rhs. Form qT_rhs manually rather than using
		 * transpose(_q). Note that qT_rhs will have rank rows and nRhs columns.
		 *
		 * The (k,kRhs)-th entry of qT_rhs will be the dot product
		 * of the k-th column of _q with the kRhs-th column of rhsRows.
		 * </pre>
		 */
		final int rank = _q[0].length;
		final int nRhs = rhsRows[0].length;
		final double[][] qT_rhs = new double[rank][];
		for (int k = 0; k < rank; ++k) {
			qT_rhs[k] = new double[nRhs];
			for (int kRhs = 0; kRhs < nRhs; ++kRhs) {
				double v = 0d;
				for (int i = 0; i < m; ++i) {
					v += _q[i][k] * rhsRows[i][kRhs];
				}
				qT_rhs[k][kRhs] = v;
			}
		}
		final UpperTriangularSolver uts = new UpperTriangularSolver(_threshold, _r, qT_rhs);
		return uts._xRows;
	}

	public double[][] getQ() {
		return _q;
	}

	public String getString() {
		final String s_a = NumericalRoutines.getString(_a);
		final double[][] qr = multiply(_q, _r);
		final String s_qr = NumericalRoutines.getString(qr);
		final double[][] qrMinusA = deepClone(qr);
		pointwiseSubtract(qrMinusA, _a);
		final String s_diff = NumericalRoutines.getString(qrMinusA);
		final double[][] nullSpace = getNullSpace();
		final String s_aTimesNullSpace = NumericalRoutines.getString(multiply(_a, nullSpace));
		final double[][] qTq = multiply(NumericalRoutines.transpose(_q), _q);
		final String s_qTq = NumericalRoutines.getString(qTq);
		final String s0 = String.format("OFFSET_T:%s\nQR:%s\nQR-OFFSET_T:%s\nA*NS:%s\nQTQ:%s", //
				s_a, s_qr, s_diff, s_aTimesNullSpace, s_qTq);
		final String s_q = NumericalRoutines.getString(_q);
		final String s_r = NumericalRoutines.getString(_r);
		final String s_nullSpace = NumericalRoutines.getString(nullSpace);
		/** Checks. */
		final String s1 = String.format("\nAlso: BLACK:%s\nR:%s\nNullSpace:%s", s_q, s_r, s_nullSpace);
		return s0 + s1;
	}

	@Override
	public String toString() {
		return getString();
	}

	private static void process(final double[][] a) {
		final Qr qr = new Qr(a);
		final String s0 = qr.getString();
		/** Check for Ax = b. */
		final RandomMatrixBuilder rmb = new RandomMatrixBuilder();
		final int m = a.length;
		final double[][] aT = NumericalRoutines.transpose(a);
		final double[] rhsClmn0 = rmb.getRandomLinearCombination(aT, m);
		final double[] rhsClmn1 = rmb.getRandomIndependentVector(aT, m);
		final double[] rhsClmn2 = rmb.getRandomLinearCombination(aT, m);
		final double[][] rhsRows = NumericalRoutines.transpose(new double[][] { rhsClmn0, rhsClmn1, rhsClmn2 });
		final double[][] xRows = qr.solve(rhsRows);
		final String s_rhs = NumericalRoutines.getString(rhsRows);
		final String s_x = NumericalRoutines.getString(xRows);
		/** Checks. */
		final double[][] aTimesX = multiply(a, xRows);
		final double[][] axMinusB = aTimesX.clone();
		pointwiseSubtract(aTimesX, rhsRows);
		final String s_diff = NumericalRoutines.getString(axMinusB);
		final String s1 = String.format("\nSolve: RHS%s\nX:%s\nAX-RHS:%s", s_rhs, s_x, s_diff);
		System.out.printf(s0 + s1);
	}

	public static void main(final String[] args) {
		final RandomMatrixBuilder rmb = new RandomMatrixBuilder();
		final double[][] a = rmb.buildMatrix(/* m= */8, /* n= */6, /* indClmnArraySpec= */new int[] { 1, 2, 4 },
				/* upperTriangular= */false);
		process(a);
	}
}
