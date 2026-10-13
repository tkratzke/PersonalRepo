package com.skagit.advMath.mitLinearAlgebra;

import java.util.Arrays;
import java.util.BitSet;

import com.skagit.utils.NumericalRoutines;

class UpperTriangularSolver extends MitLinearAlgebraCore {

	final double[][] _u;
	final double[][] _rhsRows;
	final double[][] _xRows;

	/**
	 * <pre>
	 * u must be upper triangular, but more than just "zeroes below the main
	 * diagonal." In addition, the 1st non-zero entry of each row must come
	 * strictly after the 1st non-zero of the previous row. This implies that
	 * all zero rows must be at the bottom, and the rank will be the number of
	 * non-zero rows.
	 *
	 * rhsRows == null is a flag that tells us to compute the nullSpace.
	 * </pre>
	 */
	UpperTriangularSolver(final double[][] u, final double[][] rhsRows) {
		this(_Threshold, u, rhsRows);
	}

	UpperTriangularSolver(final double threshold, final double[][] u, final double[][] rhsRows) {
		super(threshold);
		_u = u;
		if (_u == null) {
			_xRows = _rhsRows = null;
			return;
		}
		final int nRhsRows = rhsRows == null ? 0 : rhsRows.length;
		final int m = _u.length, n = _u[0].length;
		if (rhsRows != null && nRhsRows != m) {
			_xRows = _rhsRows = null;
			return;
		}
		_rhsRows = rhsRows;
		final boolean computingNullSpace = _rhsRows == null;

		/**
		 * Find the pivot variables for each non-zero row. The number of these is the
		 * rank, and the number of free variables is the number of variables minus the
		 * rank.
		 */
		final int[] pivotClmns = getPivotClmns(u);
		final int rank = pivotClmns.length;
		final int nFree = n - rank;
		final int nRhs = computingNullSpace ? nFree : _rhsRows[0].length;

		/** Initialize the answer. */
		_xRows = new double[n][];
		for (int j0 = 0, i0 = 0; j0 < n; ++j0) {
			_xRows[j0] = new double[nRhs];
			/**
			 * 0-fill unless j0 is the pivot for row i0, in which case, we fill with the
			 * rhs.
			 */
			if (!computingNullSpace && i0 < m && j0 == pivotClmns[i0]) {
				System.arraycopy(_rhsRows[i0], 0, _xRows[j0], 0, nRhs);
				++i0;
			} else {
				Arrays.fill(_xRows[j0], 0d);
			}
		}

		/** Compute badRhs and set _xRows for those columns. */
		final BitSet badRhs;
		if (computingNullSpace) {
			badRhs = null;
		} else {
			badRhs = new BitSet(nRhs);
			badRhs.clear();
			for (int kRhs = 0; kRhs < nRhs; ++kRhs) {
				for (int i = 0; i < m; ++i) {
					final double v = _rhsRows[i][kRhs];
					if (Double.isNaN(v) || (i >= rank && Math.abs(v) > threshold)) {
						badRhs.set(kRhs);
						for (int j = 0; j < n; ++j) {
							_xRows[j][kRhs] = Double.NaN;
						}
						break;
					}
				}
			}
		}

		/** Do the backsubstitution. */
		for (int i0 = rank - 1, j0 = n - 1, jFree = nFree - 1; j0 >= 0; --j0) {
			/**
			 * If we've run out of rows or j0 is not the pivot index for this row, j0 is a
			 * free variable.
			 */
			if (i0 < 0 || j0 != pivotClmns[i0]) {
				/**
				 * j0 is a free variable. If !computingNullSpace, it's already set to 0 or Nan.
				 */
				if (computingNullSpace) {
					/**
					 * j0 is the jFree-th free variable, and is associated with the jFree-th basis
					 * vector of the nullspace. Since the value for this variable is 0 for the
					 * nullspace basis vectors that correspond to the other free variables, we only
					 * update the jFree-th RHS. Moreover, the value of the variable is 1 so there is
					 * no multiplication, just the value of the coefficient itself. Finally, note
					 * that we must update for i <= i0, not just i < i0.
					 */
					_xRows[j0][jFree] = 1d;
					for (int i = 0; i <= i0; ++i) {
						_xRows[pivotClmns[i]][jFree] -= _u[i][j0];
					}
				}
				/** Move jFree back, but stay on row i0. */
				--jFree;
			} else {
				/** j0 is the pivot variable for row i0. */
				for (int kRhs = 0; kRhs < nRhs; ++kRhs) {
					if (computingNullSpace || !badRhs.get(kRhs)) {
						_xRows[j0][kRhs] /= _u[i0][j0];
						/** Update the pivot variables for the rows before i0. */
						for (int i = 0; i < i0; ++i) {
							_xRows[pivotClmns[i]][kRhs] -= _u[i][j0] * _xRows[j0][kRhs];
						}
					}
				}
				/** Move i0 back, but stay on free variable jFree. */
				--i0;
			}
		}
	}

	private int getPivotInfo(final double[][] u, final int[] pivotClmns) {
		final int m = u.length, n = u[0].length;
		int i = 0;
		for (int j = 0; j < n; ++j) {
			if (Math.abs(u[i][j]) > _threshold) {
				if (pivotClmns != null) {
					pivotClmns[i] = j;
				}
				if (++i == m) {
					return m;
				}
			}
		}
		return i;
	}

	private int[] getPivotClmns(final double[][] u) {
		if (u == null || u.length == 0 || u[0] == null) {
			return null;
		}
		final int rank = getPivotInfo(u, /* pivotClmns= */null);
		final int[] pivotClmns = new int[rank];
		getPivotInfo(u, pivotClmns);
		return pivotClmns;
	}

	/** Debugging and testing. */
	String getString() {
		final String sUOctave = NumericalRoutines.getString(_u);
		final String sXOctave = NumericalRoutines.getString(_xRows);
		final String s0;
		if (_rhsRows != null) {
			final String sRhsOctave = NumericalRoutines.getString(_rhsRows);
			s0 = String.format("CUP(Octave):\n%s\nRhs(Octave):\n%s\nX's:\n%s", sUOctave, sRhsOctave, sXOctave);
		} else {
			s0 = String.format("CUP(Octave):\n%s\nNullSpace:\n%s", sUOctave, sXOctave);
		}
		/** Checks. */
		final double[][] diff = multiply(_u, _xRows);
		final String s1;
		if (_rhsRows != null) {
			pointwiseSubtract(diff, _rhsRows);
			final String s_diff = NumericalRoutines.getString(diff);
			s1 = String.format("\n\nUX - RHS: %s\n", s_diff);
		} else {
			final String s_UTimesNs = NumericalRoutines.getString(diff);
			s1 = String.format("\n\nU*NS: %s\n", s_UTimesNs);

		}
		return s0 + s1;
	}

	@Override
	public String toString() {
		return getString();
	}

	private static void process(final double[][] a, final double[][] rhsRows) {
		final UpperTriangularSolver uts1 = new UpperTriangularSolver(a, rhsRows);
		System.out.printf("\n%s", uts1.getString());
		if (rhsRows != null) {
			final UpperTriangularSolver uts2 = new UpperTriangularSolver(a, /* rhsRows= */null);
			System.out.printf("\n%s", uts2.getString());
		}
	}

	public static void main(final String[] args) {
		final RandomMatrixBuilder rmb = new RandomMatrixBuilder();
		final double[][] u = rmb.buildMatrix(//
				/* m= */6, /* n= */4, //
				/* indClmnArraySpec= */new int[] { 0, 3 }, //
				/* upperTriangular= */true);
		final double[][] rhsRows = rmb.buildMatrix(//
				/* m= */u.length, /* n= */3, //
				/* indClmnArraySpec= */new int[] { 0, 1 }, //
				/* upperTriangular= */false);
		final UpperTriangularSolver uts1 = new UpperTriangularSolver(u, rhsRows);
		final int rank = uts1.getPivotInfo(u, /* pivotClmns= */null);
		for (int i = rank; i < u.length; ++i) {
			Arrays.fill(rhsRows[i], 0d);
		}
		process(u, rhsRows);
	}
}
