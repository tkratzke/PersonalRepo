package com.skagit.advMath.mitLinearAlgebra;

import java.util.Arrays;

import com.skagit.utils.CombinatoricTools;
import com.skagit.utils.NumericalRoutines;

public class PaqLu extends MitLinearAlgebraCore {

	public enum Pivoting {
		Minimal, // Switch rows only, and use 1st non-zero.
		RowsOnly, // Switch rows only, but search for the best row.
		Full // Switch rows and columns searching for best entry.
	}

	final Pivoting _pivoting;
	final double[][] _a;
	final int[] _p, _q;
	final double[][] _ell, _u;
	final int _rank;
	final double _determinant;
	final int[] _pivotClmns;

	/**
	 * <pre>
	 * We will compute rank and then:
	 *   _ell will be m x rank (m is forced).
	 *   _u will be rank by n (both forced).
	 *   _ell will have 1's on its diagonal.
	 *   _u will be upper triangular with every row's 1st non-zero
	 *      occurring strictly after the previous row's 1st non-zero.
	 *   _p will be a permutation vector of 0, ..., m-1.
	 *   _q will be a permutation vector of 0, ..., n-1
	 *      if using full pivoting, and null otherwise.
	 *   _pivotClmns will identify the 1st non-zero entry of each row of _u.
	 *      We don't need _pivotClmns, but it is useful for generating
	 *      a random vector that is independent of a given set of vectors.
	 * We store a pointer to a.  We don't destroy a, and use our pointer
	 *    only for checking later on.
	 * </pre>
	 */
	public PaqLu(final double[][] a) {
		this(a, Pivoting.Minimal);
	}

	public PaqLu(final double[][] a, final Pivoting pivoting) {
		this(_Threshold, a, pivoting);
	}

	public PaqLu(final double threshold, final double[][] a, final Pivoting pivoting) {
		super(threshold);
		_pivoting = pivoting == null ? Pivoting.Minimal : pivoting;
		_a = a;
		final int m = _a.length, n = _a[0].length;

		/**
		 * While working, we store the updates in the matrix ellU, thus storing both ell
		 * and u in this matrix. It's more convenient for switching rows.
		 */
		final double[][] ellU = deepClone(_a);

		/** Initialize pivotClmns0; we know we won't have more than m. */
		final int[] pivotClmns0 = new int[m];
		Arrays.fill(pivotClmns0, -1);

		/** Use rnk as a row counter. rank will be the highest rnk we get to (+1). */
		int rnk = 0;
		/** Store the permutations; to start with, they're trivial. */
		_p = CombinatoricTools.getInitial(m);
		_q = CombinatoricTools.getInitial(n);
		int nFlips = 0;
		for (int j0 = 0; j0 < n && rnk < m; ++j0) {
			/** Find the pivot for row rnk, starting with column j0. */
			int iWin = -1, jWin = -1;
			double winValue = _threshold;
			I_LOOP: for (int i = rnk; i < m; ++i) {
				final int stopJ = _pivoting != Pivoting.Full ? j0 + 1 : n;
				for (int j = j0; j < stopJ; ++j) {
					final double thisValue = Math.abs(ellU[i][j]);
					if (thisValue > winValue) {
						winValue = thisValue;
						iWin = i;
						jWin = j;
						if (_pivoting == Pivoting.Minimal) {
							break I_LOOP;
						}
					}
				}
			}

			if (iWin == -1) {
				/**
				 * j0 is not a pivot for row rnk. If we are using full pivoting, the rest of the
				 * matrix is 0's and we're done. If we're using partial pivoting, our
				 * multipliers are all 0 and we move onto the next column.
				 */
				if (_pivoting == Pivoting.Full) {
					/** Full pivoting. We're done. */
					break;
				}
				/**
				 * Partial or minimal pivoting. Store the 0's in the rnk-th (not j0-th) column
				 * of ell.
				 */
				for (int i = rnk + 1; i < m; ++i) {
					ellU[i][rnk] = 0d;
				}
				/** Stay on row rnk, but move on to the next column. */
				continue;
			}

			/**
			 * j0 is a pivot for row rnk, but we might have to switch rows and/or columns.
			 */
			if (rnk != iWin) {
				final double[] row = ellU[iWin];
				ellU[iWin] = ellU[rnk];
				ellU[rnk] = row;
				final int i = _p[rnk];
				_p[rnk] = _p[iWin];
				_p[iWin] = i;
				++nFlips;
			}
			if (j0 != jWin) {
				for (int i = 0; i < m; ++i) {
					final double z = ellU[i][j0];
					ellU[i][j0] = ellU[i][jWin];
					ellU[i][jWin] = z;
				}
				final int j = _q[j0];
				_q[j0] = _q[jWin];
				_q[jWin] = j;
				++nFlips;
			}

			/** Do row operations to zero out column j0 below row rnk. */
			final double[] row = ellU[rnk];
			final double pivotValue = row[j0];
			for (int i = rnk + 1; i < m; ++i) {
				final double mult = ellU[i][j0] / pivotValue;
				for (int j = j0 + 1; j < n; ++j) {
					ellU[i][j] -= mult * row[j];
				}
				/**
				 * Store the multiplier for row i in column rnk, NOT column j0. This is the meat
				 * of ell.
				 */
				ellU[i][rnk] = mult;
			}

			/**
			 * We have the pivot column for row rnk and are hence done with rnk.
			 */
			pivotClmns0[rnk++] = j0;
		}

		/** We now have _rank and pivot clmns. */
		_rank = rnk;
		_pivotClmns = new int[_rank];
		System.arraycopy(pivotClmns0, 0, _pivotClmns, 0, _rank);

		/** Populate _ell and _u from ellU. */
		_ell = new double[m][];
		for (int i = 0; i < _rank; ++i) {
			_ell[i] = new double[_rank];
			Arrays.fill(_ell[i], 0d);
			System.arraycopy(ellU[i], 0, _ell[i], 0, i);
			_ell[i][i] = 1d;
		}
		for (int i = _rank; i < m; ++i) {
			_ell[i] = new double[_rank];
			System.arraycopy(ellU[i], 0, _ell[i], 0, _rank);
		}

		_u = new double[_rank][];
		for (int i0 = 0; i0 < _rank; ++i0) {
			_u[i0] = new double[n];
			for (int j0 = 0; j0 < n; ++j0) {
				if (j0 < _pivotClmns[i0]) {
					_u[i0][j0] = 0d;
				} else {
					_u[i0][j0] = ellU[i0][j0];
				}
			}
		}

		/** Compute _determinant. */
		if (_rank < m || _rank < n) {
			_determinant = 0d;
		} else {
			double determinant = 1d;
			for (int k = 0; k < m; ++k) {
				determinant *= _u[k][k];
			}
			_determinant = determinant * (nFlips % 2 == 0 ? 1d : -1d);
		}
	}

	public double[][] getNullSpace() {
		final int n = _a[0].length;
		final UpperTriangularSolver uts = new UpperTriangularSolver(_threshold, //
				_u, /* rhsRows= */ null);
		final double[][] xRows = uts._xRows;
		/** See how to use _q in the routine solve. */
		final double[][] xRowsOut = new double[n][];
		for (int j = 0; j < n; ++j) {
			xRowsOut[_q[j]] = xRows[j];
		}
		return xRowsOut;
	}

	public double[][] solve(final double[][] rhsRowsIn) {
		final int m = _a.length, n = _a[0].length;
		final int rank = _pivotClmns.length;
		final int nRhs = (rhsRowsIn == null || rhsRowsIn.length == 0 || rhsRowsIn[0] == null) ? 0 : rhsRowsIn[0].length;
		/**
		 * "_p[2] = 8" means that the 8th row ended in the 2nd position. Hence, we must
		 * move the 8th input row to the 2nd position.
		 */
		final double[][] rhsRows = new double[m][];
		final double[][] yRows = new double[rank][];
		for (int i = 0; i < m; ++i) {
			/** In the above explanation, i0 is 2 and i is 8. */
			rhsRows[i] = rhsRowsIn[_p[i]];
		}

		/** Allocate yRows. */
		for (int i = 0; i < rank; ++i) {
			yRows[i] = new double[nRhs];
		}

		/**
		 * Solve ELL'y = rhs for y, where ELL' is the 1st rank rows of ELL. We do a
		 * manual forward substitution here on the first rank rows of ELL.
		 */
		for (int i = 0; i < rank; ++i) {
			for (int kRhs = 0; kRhs < nRhs; ++kRhs) {
				double sum = rhsRows[i][kRhs];
				for (int j = 0; j < i; ++j) {
					sum -= yRows[j][kRhs] * _ell[i][j];
				}
				/** No scaling since _ell[rnk][rnk] is always 1. */
				yRows[i][kRhs] = sum;
			}
		}

		/** For each Rhs, check the rest of the rows to see if y works. */
		for (int kRhs = 0; kRhs < nRhs; ++kRhs) {
			for (int i0 = rank; i0 < m; ++i0) {
				double sum = rhsRows[i0][kRhs];
				for (int rnk = 0; rnk < rank; ++rnk) {
					sum -= yRows[rnk][kRhs] * _ell[i0][rnk];
				}
				if (Math.abs(sum) > _threshold) {
					/** kRhs is a bad RHS. */
					for (int i = 0; i < rank; ++i) {
						yRows[i][kRhs] = Double.NaN;
					}
					break;
				}
			}
		}

		/** Solve ux = y for x. */
		final UpperTriangularSolver uts = new UpperTriangularSolver(_threshold, _u, yRows);
		final double[][] xRows = uts._xRows;

		/**
		 * "_q[2] = 8" means that the 8th variable ended in the 2nd position. Hence, we
		 * must move the 2nd answer to the 8th output row.
		 */
		final double[][] xRowsOut = new double[n][];
		for (int j = 0; j < n; ++j) {
			xRowsOut[_q[j]] = xRows[j];
		}
		return xRowsOut;
	}

	public double[][] getAInverse() {
		final int m = _a.length, n = _a[0].length;
		final int rank = _pivotClmns.length;
		if (m != n || m != rank) {
			return null;
		}
		final double[][] rhsRows = new double[m][];
		for (int i0 = 0; i0 < m; ++i0) {
			rhsRows[i0] = new double[n];
			Arrays.fill(rhsRows[i0], 0d);
			rhsRows[i0][i0] = 1d;
		}
		return solve(rhsRows);
	}

	public double[][] getEll() {
		return _ell;
	}

	public double[][] getU() {
		return _u;
	}

	public double[][] getP() {
		final int m = _a.length;
		final double[][] p = new double[m][];
		for (int i = 0; i < m; ++i) {
			p[i] = new double[m];
			Arrays.fill(p[i], 0d);
		}
		for (int i = 0; i < m; ++i) {
			p[i][_p[i]] = 1d;
		}
		return p;
	}

	public double[][] getPInv() {
		final int m = _a.length;
		final double[][] p = new double[m][];
		for (int i = 0; i < m; ++i) {
			p[i] = new double[m];
			Arrays.fill(p[i], 0d);
		}
		for (int i = 0; i < m; ++i) {
			p[_p[i]][i] = 1d;
		}
		return p;
	}

	public double[][] getQ() {
		final int n = _a[0].length;
		final double[][] q = new double[n][];
		for (int j = 0; j < n; ++j) {
			q[j] = new double[n];
			Arrays.fill(q[j], 0d);
		}
		for (int j = 0; j < n; ++j) {
			q[_q[j]][j] = 1d;
		}
		return q;
	}

	public double[][] getQInv() {
		final int n = _a[0].length;
		final double[][] q = new double[n][];
		for (int j = 0; j < n; ++j) {
			q[j] = new double[n];
			Arrays.fill(q[j], 0d);
		}
		for (int j = 0; j < n; ++j) {
			q[j][_q[j]] = 1d;
		}
		return q;
	}

	/** Returns the (row,clmn) of _a of the pivots. */
	public int[][] getPivotPairs() {
		final int rank = _pivotClmns.length;
		final int[][] pivotPairs = new int[rank][];
		for (int i0 = 0; i0 < rank; ++i0) {
			final int i = _p[i0];
			final int j0 = _pivotClmns[i0];
			final int j = _q[j0];
			pivotPairs[i0] = new int[] { i, j };
		}
		/** We sort by column number, not row number. */
		Arrays.sort(pivotPairs, (rowClmn0, rowClmn1) -> rowClmn0[1] - rowClmn1[1]);
		return pivotPairs;
	}

	public String getString() {
		final double[][] ell = getEll();
		final double[][] u = getU();
		final double[][] pInv = getPInv();
		final double[][] qInv = getQInv();
		final String s_a = NumericalRoutines.getString(_a);
		double[][] pInvEllUQInvMinusA = multiply(pInv, ell, u);
		pInvEllUQInvMinusA = multiply(pInvEllUQInvMinusA, qInv);
		pointwiseSubtract(pInvEllUQInvMinusA, _a);
		final String s_pInvEllUQInvMinusA = NumericalRoutines.getString(pInvEllUQInvMinusA);
		final double[][] nullSpace = getNullSpace();
		final double[][] aNullSpace = multiply(_a, nullSpace);
		final String s_aNllSpc = NumericalRoutines.getString(aNullSpace);
		final String s0 = String.format("OFFSET_T:%s\nA-pInvEllUqInv:%s\nA*NS:%s", s_a, s_pInvEllUQInvMinusA,
				s_aNllSpc);

		final double[][] p = getP();
		final double[][] q = getQ();
		final int[][] pivotPairs = getPivotPairs();
		final String s_ell = NumericalRoutines.getString(ell);
		final String s_u = NumericalRoutines.getString(u);
		final String s_p = NumericalRoutines.getString(p);
		final String s_q = NumericalRoutines.getString(q);
		final String s_nullspace = NumericalRoutines.getString(nullSpace);
		final StringBuilder s_pivotPairs = new StringBuilder("[");
		final int nPivotPairs = pivotPairs.length;
		for (int k0 = 0; k0 < nPivotPairs; ++k0) {
			final int row = pivotPairs[k0][0], clmn = pivotPairs[k0][1];
			s_pivotPairs.append(k0 == 0 ? "[[" : " [").append(row).append(" ").append(clmn)
					.append(k0 == nPivotPairs - 1 ? "]]" : "]");
		}
		final String s1 = String.format("\nEll:%s\nU:%s\nP:%s\nQ:%s\nPivotPairs: %s\nNullSpace:%s", s_ell, s_u, s_p,
				s_q, s_pivotPairs.toString(), s_nullspace);
		return s0 + s1;
	}

	public static double getPaqLuDeterminant(final double[][] a) {
		return new PaqLu(a, Pivoting.Full)._determinant;
	}

	public static void main(final String[] args) {
		final RandomMatrixBuilder rmb = new RandomMatrixBuilder();
		for (int k0 = 0;; ++k0) {
			final int mIn, nIn;
			final int[] indClmnArraySpec;
			final Pivoting pivoting;
			final boolean upperTriangular;
			if (k0 == 0) {
				mIn = 8;
				nIn = 6;
				indClmnArraySpec = new int[] { 1, 2, 3, 5 };
				pivoting = Pivoting.RowsOnly;
			} else {
				mIn = nIn = -1;
				indClmnArraySpec = null;
				pivoting = null;
			}
			upperTriangular = false;
			final double[][] a = rmb.buildMatrix(mIn, nIn, indClmnArraySpec, upperTriangular);
			final PaqLu paqLu = new PaqLu(a, pivoting);

			/** Standard String: */
			final String s0 = paqLu.getString();

			/** String for AX-RHS. */
			final int m = a.length;
			final double[][] aT = NumericalRoutines.transpose(a);
			final double[] rhsClmn0 = rmb.getRandomLinearCombination(aT, m);
			final double[] rhsClmn1 = rmb.getRandomIndependentVector(aT, m);
			final double[] rhsClmn2 = rmb.getRandomLinearCombination(aT, m);
			final double[][] rhsClmns = { rhsClmn0, rhsClmn1, rhsClmn2 };
			for (int k1 = 0; k1 < rhsClmns.length; ++k1) {
				if (rhsClmns[k1] == null) {
					rhsClmns[k1] = new double[m];
					Arrays.fill(rhsClmns[k1], Double.NaN);
				}
			}
			final double[][] rhsRows = NumericalRoutines.transpose(rhsClmns);
			final String s_rhs = NumericalRoutines.getString(rhsRows);
			final double[][] xRows = paqLu.solve(rhsRows);
			final String s_x = NumericalRoutines.getString(xRows);
			final double[][] aXMinusRhs = multiply(a, xRows);
			pointwiseSubtract(aXMinusRhs, rhsRows);
			final String s_aTimesXMinusRhs = NumericalRoutines.getString(aXMinusRhs);
			final String s1 = String.format("\n\nRhs:%s\nX:%s\nAX-RHS:%s", s_rhs, s_x, s_aTimesXMinusRhs);

			/** String for the inverse. */
			final int n = a[0].length;
			final String s2;
			if (m == n && m == paqLu._pivotClmns.length) {
				final double[][] aInv = paqLu.getAInverse();
				final double[][] eye = multiply(a, aInv);
				final String s_AAInv = NumericalRoutines.getString(eye);
				s2 = String.format("\n\nA(inv(OFFSET_T)):%s", s_AAInv);
			} else {
				s2 = "";
			}
			System.out.print(s0 + s1 + s2);
			System.exit(33);
		}
	}

}