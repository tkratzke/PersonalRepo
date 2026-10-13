package com.skagit.advMath.mitLinearAlgebra;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;

import com.skagit.utils.CombinatoricTools;
import com.skagit.utils.NumericalRoutines;
import com.skagit.utils.PermutationTools;

public class RandomMatrixBuilder extends MitLinearAlgebraCore {

	final static int _LoMult = -2;
	final static int _HiMult = 2;
	final static int _LoNudge = -3;
	final static int _HiNudge = 3;

	final private Random _random;
	final private int _loMult, _hiMult;
	final private int _loNudge, _hiNudge;

	public RandomMatrixBuilder(final long seed, final int loMult, final int hiMult, final int loNudge,
			final int hiNudge) {
		_random = new Random();
		_random.setSeed(seed);
		_loMult = loMult;
		_hiMult = hiMult;
		_loNudge = loNudge;
		_hiNudge = hiNudge;
	}

	public Random getRng() {
		return _random;
	}

	public RandomMatrixBuilder(final long seed) {
		this(seed, _LoMult, _HiMult, _LoNudge, _HiNudge);
	}

	public RandomMatrixBuilder() {
		this(/* seed= */371L);
	}

	/**
	 * <pre>
	 * Returns a random m x n matrix with "new independent columns"
	 * specified by indClmnsArray.  If upperTriangular is true,
	 * then the returned matrix will satisfy the following:
	 *    1st non-zero in each row will be strictly after the
	 *    1st non-zero in the previous row.
	 * </pre>
	 */
	public double[][] buildMatrix(final int mIn, final int nIn, final int[] indClmnsArrayIn,
			final boolean upperTriangular) {
		final int[] indClmnsArray;
		if (indClmnsArrayIn == null) {
			indClmnsArray = CombinatoricTools.getInitial(Math.min(mIn, nIn));
		} else {
			indClmnsArray = indClmnsArrayIn.clone();
			Arrays.sort(indClmnsArray);
		}
		final int rank = indClmnsArray.length;
		final int maxSpec = indClmnsArray[rank - 1];
		final int m = Math.max(mIn, rank);
		final int n = Math.max(nIn, Math.max(rank, maxSpec + 1));

		final double[][] clmns = new double[n][];
		for (int j = 0; j < n; ++j) {
			final double[][] tempClmns = new double[j][];
			System.arraycopy(clmns, 0, tempClmns, 0, j);
			/** Fill in the dependent columns until the next independent one. */
			final int lubIdx = CombinatoricTools.getLubIndex(indClmnsArray, j);
			final int lub = lubIdx == indClmnsArray.length ? n : indClmnsArray[lubIdx];
			for (; j < lub; ++j) {
				clmns[j] = getRandomLinearCombination(tempClmns, m);
			}
			if (j < n) {
				if (upperTriangular) {
					clmns[j] = getRandomLinearCombination(tempClmns, m);
					clmns[j][lubIdx] = randomNonZeroInt(_loNudge, _hiNudge);
				} else {
					clmns[j] = getRandomIndependentVector(tempClmns, m);
				}
			}
		}
		return NumericalRoutines.transpose(clmns);
	}

	public double[][] getRandomOgMatrix(final int m) {
		final double[][] mtx0 = buildMatrix(m, m, /* indClmns= */CombinatoricTools.getInitial(m),
				/* upperTriangular= */false);
		final Qr qrU = new Qr(mtx0);
		final double[][] q = qrU.getQ();
		final int[] perm = PermutationTools.randomPermutation(m, _random);
		final double[][] mtx = new double[m][];
		for (int k = 0; k < m; ++k) {
			mtx[k] = q[perm[k]];
		}
		return mtx;
	}

	public double[][] getRandomMatrixFromSvd(final int m, final int n, final double[] sigmas) {
		final double[][] u = getRandomOgMatrix(m);
		final double[][] vT = getRandomOgMatrix(n);
		final int nSigmas = Math.min(m, sigmas.length);
		/** Form u x sigma, an m x n matrix. */
		final double[][] uSigma = new double[m][n];
		for (int j = 0; j < n; ++j) {
			/** Form the jth clmn of u x sigma. */
			final double sigma = j < nSigmas ? sigmas[j] : 0d;
			for (int i = 0; i < m; ++i) {
				uSigma[i][j] = sigma != 0d ? (u[i][j] * sigma) : 0d;
			}
		}
		return multiply(uSigma, vT);
	}

	public double[] getRandomLinearCombination(final double[][] vectors, final int m) {
		final int n = getNVectors(vectors);
		final double[] vector = new double[m];
		Arrays.fill(vector, 0d);
		for (int j = 0; j < n; ++j) {
			final double coef = randomNonZeroInt(_loMult, _hiMult);
			for (int i = 0; i < m; ++i) {
				vector[i] += coef * vectors[j][i];
			}
		}
		return vector;
	}

	public double[] getRandomIndependentVector(final double[][] vectors, final int m) {
		final int n = getNVectors(vectors);
		if (n == 0) {
			final double[] vector = new double[m];
			for (int i = 0; i < m; ++i) {
				vector[i] = randomNonZeroInt(_loNudge, _hiNudge);
			}
			return vector;
		}

		/** We're thinking of vectors as columns. rowReduce to find the pivots. */
		final PaqLu paqLu = new PaqLu(NumericalRoutines.transpose(vectors), PaqLu.Pivoting.Full);
		final int[][] pivotPairs = paqLu.getPivotPairs();

		/**
		 * If we have m pivots, we cannot get an independent column. After that check,
		 * record the pivot rows and build an array of columns to take a linear
		 * combination of.
		 */
		final int rank = pivotPairs.length;
		if (rank == m) {
			return null;
		}
		final BitSet pivotRows = new BitSet(m);
		final double[][] tempClmns = new double[rank][];
		for (int k = 0; k < rank; ++k) {
			pivotRows.set(pivotPairs[k][0]);
			tempClmns[k] = vectors[pivotPairs[k][1]];
		}

		/**
		 * Get a random linear combination and then nudge the non-pivot rows off of 0;
		 * this guarantees an independent column, even if we nudged only one entry.
		 */
		final double[] clmn = getRandomLinearCombination(tempClmns, m);
		for (int i = pivotRows.nextClearBit(0); i < m; i = pivotRows.nextClearBit(i + 1)) {
			clmn[i] += randomNonZeroInt(_loNudge, _hiNudge);
		}
		return clmn;
	}

	private int randomNonZeroInt(final int lo, final int hi) {
		final int range = hi - lo + 1;
		final boolean forbidOnes = hi > 1 || lo < -1;
		int randomInt = 0;
		for (int k = 0; k < 10; ++k) {
			randomInt = _random.nextInt(range) + lo;
			if (randomInt == 0) {
				continue;
			}
			if (!forbidOnes || randomInt > 1 || randomInt < -1) {
				return randomInt;
			}
		}
		return randomInt;
	}

}
