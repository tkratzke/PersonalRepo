package com.skagit.linearSystemGenerator;

import java.util.Arrays;
import java.util.BitSet;

import com.skagit.advMath.mitLinearAlgebra.PaqLu;
import com.skagit.utils.CombinatoricTools;
import com.skagit.utils.StringUtilities;

class Statics {
	final private static String _EqutnNamesString = "ABCDEFGHIJKLMN";
	final private static String _VblNamesString = "xyzwqrsuv";

	static String getNominalEqutnName(final int m, final int i) {
		return getNominalName(_EqutnNamesString, m, i);
	}

	static String getNominalVblName(final int n, final int j) {
		return getNominalName(_VblNamesString, n, j);
	}

	static String getNominalName(final String names, final int m, final int i) {
		/** It is assumed that i < m. */
		final int nAtoms = names.length();
		if (m <= nAtoms) {
			return String.valueOf(names.charAt(i));
		}
		final int k0 = i / nAtoms;
		final int k1 = i % nAtoms;
		return String.valueOf(names.charAt(k0)) + String.valueOf(names.charAt(k1));
	}

	static String getSystemString(final double[][] coefsIn, final double[] rhsIn) {
		return getSystemString(null, null, null, null, null, coefsIn, rhsIn);
	}

	static String getSystemString(int[] orderToPrintEqutns, int[] orderToPrintVbls, String[] equtnNamesIn,
			String[] vblNamesIn, double[] guessedValuesIn, final double[][] coefsIn, final double[] rhsIn) {
		final int m0 = coefsIn.length;
		final int n0 = coefsIn[0].length;

		/** Set defaults so that a simple call is available. */
		if (orderToPrintEqutns == null) {
			orderToPrintEqutns = CombinatoricTools.getInitial(m0);
		}
		if (orderToPrintVbls == null) {
			orderToPrintVbls = CombinatoricTools.getInitial(n0);
		}
		if (equtnNamesIn == null) {
			equtnNamesIn = new String[m0];
			for (int i0 = 0; i0 < m0; ++i0) {
				equtnNamesIn[i0] = getNominalEqutnName(m0, i0);
			}
		} else {
			equtnNamesIn = equtnNamesIn.clone();
		}
		if (vblNamesIn == null) {
			vblNamesIn = new String[n0];
			for (int j0 = 0; j0 < n0; ++j0) {
				vblNamesIn[j0] = getNominalVblName(n0, j0);
			}
		} else {
			vblNamesIn = vblNamesIn.clone();
		}
		if (guessedValuesIn == null) {
			guessedValuesIn = new double[n0];
			Arrays.fill(guessedValuesIn, Double.NaN);
		} else {
			guessedValuesIn = guessedValuesIn.clone();
		}

		/**
		 * Set equtnNames0 and rhs0 as per orderToPrintEqutns, and set coefs0 as per
		 * both orderToPrintEqutns and orderToPrintVbls.
		 */
		final String[] equtnNames0 = new String[m0];
		final double[] rhs0 = new double[m0];
		final double[][] coefs0 = new double[m0][];
		for (int i0 = 0; i0 < m0; ++i0) {
			final int i = orderToPrintEqutns[i0];
			equtnNames0[i0] = equtnNamesIn[i];
			coefs0[i0] = new double[n0];
			for (int j0 = 0; j0 < n0; ++j0) {
				final int j = orderToPrintVbls[j0];
				coefs0[i0][j0] = coefsIn[i][j];
			}
			rhs0[i0] = rhsIn[i];
		}
		/** Set vblNames0 and guessedValues0 as per orderToPrintVbls. */
		final String[] vblNames0 = new String[n0];
		final double[] guessedValues0 = new double[n0];
		for (int j0 = 0; j0 < n0; ++j0) {
			final int j = orderToPrintVbls[j0];
			vblNames0[j0] = vblNamesIn[j];
			guessedValues0[j0] = guessedValuesIn[j];
		}

		/** Compute realVbls and their names. */
		final BitSet realVbls = new BitSet(n0);
		for (int j0 = 0; j0 < n0; ++j0) {
			realVbls.set(j0, !Double.isFinite(guessedValues0[j0]));
		}
		final int n = realVbls.cardinality();
		/** Name the real variables to retain their old names. */
		final String[] realVblNames = new String[n];
		for (int j0 = realVbls.nextSetBit(0), j = 0; j0 >= 0; j0 = realVbls.nextSetBit(j0 + 1)) {
			realVblNames[j++] = vblNames0[j0];
		}

		/**
		 * An equation is real if it has a nonzero coef with a real vble, or rhs0,
		 * adjusted by guessedValues, is not 0.
		 */
		final BitSet realEqutns = new BitSet(m0);
		LOOP0: for (int i0 = 0; i0 < m0; ++i0) {
			realEqutns.clear(i0);
			double rhsI0 = rhs0[i0];
			for (int j0 = 0; j0 < n0; ++j0) {
				if (!realVbls.get(j0)) {
					/** Update rhsI0 since guessedValues[j0] is real. */
					rhsI0 -= guessedValues0[j0] * coefs0[i0][j0];
				} else if (coefs0[i0][j0] != 0d) {
					/** j0 is a real variable and coefs0[i0][j] != 0d. i0 is a real equation. */
					realEqutns.set(i0);
					continue LOOP0;
				}
			}
			/**
			 * We found no real part of the equation. If the adjusted RHS is not 0, keep the
			 * equation even thouogh the LHS has vanished.
			 */
			if (Math.abs(rhsI0) > 0.5) {
				realEqutns.set(i0);
			}
		}
		final int m = realEqutns.cardinality();
		if (m == 0) {
			return "";
		}

		/** Name the real equations to retain their old names. */
		final String[] realEqutnNames = new String[m];
		for (int i0 = realEqutns.nextSetBit(0), i = 0; i0 >= 0; i0 = realEqutns.nextSetBit(i0 + 1)) {
			realEqutnNames[i++] = equtnNames0[i0];
		}

		final double[][] coefs = new double[m][n];
		final double[] rhs = new double[m];
		for (int i0 = realEqutns.nextSetBit(0), i = 0; i0 >= 0; i0 = realEqutns.nextSetBit(i0 + 1)) {
			rhs[i] = rhs0[i0];
			for (int j0 = 0, j = 0; j0 < n0; ++j0) {
				final double coef0 = coefs0[i0][j0];
				if (realVbls.get(j0)) {
					coefs[i][j++] = coef0;
				} else {
					rhs[i] -= guessedValues0[j0] * coef0;
				}
			}
			++i;
		}
		/**
		 * Find the column indices of the first nonzeros in each row, and the column
		 * index of the first nonzero of the entire LHS.
		 */
		int firstNonZero = n;
		final int[] firstNonZeros = new int[m];
		for (int i = 0; i < m; ++i) {
			firstNonZeros[i] = -1;
			for (int j = 0; j < n; ++j) {
				if (coefs[i][j] != 0d) {
					firstNonZeros[i] = j;
					firstNonZero = Math.min(firstNonZero, j);
					break;
				}
			}
		}

		/** Get the nominal eqtun names, compute the max, and then pad them. */
		int maxEqutnName = 0;
		final String[] equtnNamesToUse = new String[m];
		for (int i = 0; i < m; ++i) {
			final String equtnName = realEqutnNames[i];
			maxEqutnName = Math.max(maxEqutnName, equtnName.length());
			equtnNamesToUse[i] = equtnName;
		}
		for (int i = 0; i < m; ++i) {
			equtnNamesToUse[i] = StringUtilities.padLeft(equtnNamesToUse[i], maxEqutnName);
		}

		/**
		 * Column j needs an intro string (3*" ", " + ", or " - ") if some row i has a
		 * non-zero entry in column j, and this is not the first non-zero in row i.
		 */
		final boolean[] needIntroStrings = new boolean[n];
		for (int j = 0; j < n; ++j) {
			needIntroStrings[j] = false;
			for (int i = 0; i < m; ++i) {
				if ((coefs[i][j] != 0d) && (j > firstNonZeros[i])) {
					needIntroStrings[j] = true;
					break;
				}
			}
		}
		/**
		 * We need an extra blank if we are the first non-zero in our row and no other
		 * row needs a " + " or a " - " in our column.
		 */
		final boolean[][] needExtraBlanks = new boolean[m][n];
		for (int j = 0; j < n; ++j) {
			for (int i = 0; i < m; ++i) {
				needExtraBlanks[i][j] = j == firstNonZeros[i] && !needIntroStrings[j];
			}
		}
		/** Get the nominal strings. */
		final String[][] mtxStrings = new String[m][n];
		for (int j = 0; j < n; ++j) {
			for (int i = 0; i < m; ++i) {
				final double coef = coefs[i][j];
				if (coef == 0d) {
					mtxStrings[i][j] = "";
				} else {
					final StringBuilder s = new StringBuilder();
					if (j == firstNonZeros[i]) {
						if (needExtraBlanks[i][j]) {
							s.append(' ');
						}
						if (coef < 0d) {
							s.append('-');
						}
					}
					final double absCoef = Math.abs(coef);
					if (absCoef != 1d) {
						s.append(getNumericString(Math.abs(coef)));
					}
					s.append(realVblNames[j]);
					mtxStrings[i][j] = s.toString();
				}
			}
		}
		/**
		 * If for some i, all mtxStrings[i][j]="" for all j, then set the last
		 * mtxStrings[j] to "0". Also, mark this as a bad row.
		 */
		final BitSet badRows = new BitSet(m);
		badRows.clear();
		int n1 = n;
		LOOP1: for (int i = 0; i < m; ++i) {
			for (int j = 0; j < n; ++j) {
				if (mtxStrings[i][j].length() > 0) {
					continue LOOP1;
				}
			}
			if (mtxStrings[i].length == 0) {
				/** We need a column to put "0" into. */
				n1 = 1;
				for (int i1 = 0; i1 < m; ++i1) {
					mtxStrings[i1] = new String[n1];
					mtxStrings[i1][0] = "";
				}
			}
			mtxStrings[i][n1 - 1] = "0";
			badRows.set(i);
		}
		/** Henceforth, each mtxStrings[i] has n1 entries. */

		final int[] clmnMaxes = new int[n1];
		for (int j = 0; j < n1; ++j) {
			clmnMaxes[j] = 0;
			for (int i = 0; i < m; ++i) {
				clmnMaxes[j] = Math.max(clmnMaxes[j], mtxStrings[i][j].length());
			}
		}

		/** Pad the strings. */
		for (int i = 0; i < m; ++i) {
			for (int j = 0; j < n1; ++j) {
				final int clmnMax = clmnMaxes[j];
				if (clmnMax > 0) {
					mtxStrings[i][j] = StringUtilities.padLeft(mtxStrings[i][j], clmnMax);
				}
			}
		}

		/** Get the nominal rhs strings, compute the max, and then pad them. */
		int maxRhs = 0;
		final String[] rhsStrings = new String[m];
		for (int i = 0; i < m; ++i) {
			final String rhsString = getNumericString(rhs[i]);
			maxRhs = Math.max(maxRhs, rhsString.length());
			rhsStrings[i] = rhsString;
		}
		for (int i = 0; i < m; ++i) {
			rhsStrings[i] = StringUtilities.padLeft(rhsStrings[i], maxRhs);
		}

		/** Finally, write it out. */
		final StringBuilder s = new StringBuilder();
		for (int i = 0; i < m; ++i) {
			if (i > 0) {
				s.append('\n');
			}
			s.append(equtnNamesToUse[i]).append(". ");
			for (int j = 0; j < n1; ++j) {
				final int clmnMax = clmnMaxes[j];
				if (clmnMax > 0) {
					if (j < n) {
						final double coef = coefs[i][j];
						if (needIntroStrings[j]) {
							if (coef == 0d || j == firstNonZeros[i]) {
								s.append("   ");
							} else {
								s.append(coef > 0d ? " + " : " - ");
							}
						}
					}
					s.append(mtxStrings[i][j]);
				}
			}
			s.append(" = ").append(rhsStrings[i]);
			if (badRows.get(i)) {
				s.append("(Bad)");
			}
		}
		return s.toString();
	}

	final private static int _NSignificantDigits = 5;
	final private static double _ThresholdForCloseToInt = Math.pow(10d, -_NSignificantDigits - 1);
	final private static String _FFormat = "%" + "." + _NSignificantDigits + "f";

	public static String getNumericString(final double a) {
		if (Math.abs(Math.round(a) - a) <= _ThresholdForCloseToInt) {
			return String.format("%d", (int) Math.round(a));
		}
		return String.format(_FFormat, a);
	}

	public static LsStatus getStatus(final double[][] coefs0, final double[] guessedValues, final double[] rhs0) {
		final int m0 = coefs0.length, n0 = coefs0[0].length;
		final BitSet realVbls = new BitSet(n0);
		for (int j0 = 0; j0 < n0; ++j0) {
			realVbls.set(j0, !Double.isFinite(guessedValues[j0]));
		}
		final int n = realVbls.cardinality();
		final BitSet realEqutns = new BitSet(m0);
		for (int i0 = 0; i0 < m0; ++i0) {
			realEqutns.clear(i0);
			for (int j0 = realVbls.nextSetBit(0); j0 >= 0; j0 = realVbls.nextSetBit(j0 + 1)) {
				if (coefs0[i0][j0] != 0d) {
					realEqutns.set(i0);
					continue;
				}
			}
		}
		/** If some dead equation is violated, return fail. */
		for (int i0 = realEqutns.nextClearBit(0); i0 < m0; i0 = realEqutns.nextClearBit(i0 + 1)) {
			double dotProduct = 0d;
			for (int j0 = realVbls.nextClearBit(0); j0 < n0; j0 = realVbls.nextClearBit(j0 + 1)) {
				dotProduct += guessedValues[j0] * coefs0[i0][j0];
			}
			if (Math.abs(dotProduct - rhs0[i0]) > 1.0e-5) {
				return LsStatus.INFEASIBLE;
			}
		}
		if (n == 0) {
			return LsStatus.SOLVED;
		}
		/** Use PaqLu to check the feasibility of the rest. */
		final int m = realEqutns.cardinality();
		final double[][] coefs = new double[m][n];
		final double[][] rhsRows = new double[m][1];
		for (int i0 = realEqutns.nextSetBit(0), i = 0; i0 >= 0; i0 = realEqutns.nextSetBit(i0 + 1), ++i) {
			rhsRows[i][0] = rhs0[i0];
			for (int j0 = 0, j = 0; j0 < n0; ++j0) {
				final double coef = coefs0[i0][j0];
				if (realVbls.get(j0)) {
					coefs[i][j++] = coef;
				} else {
					rhsRows[i][0] -= coef * guessedValues[j0];
				}
			}
		}
		final PaqLu paqLu = new PaqLu(coefs, PaqLu.Pivoting.Full);
		final double[][] answerRows = paqLu.solve(rhsRows);
		for (int j = 0; j < n; ++j) {
			if (!Double.isFinite(answerRows[j][0])) {
				return LsStatus.INFEASIBLE;
			}
		}
		return LsStatus.FEASIBLE;
	}

}
