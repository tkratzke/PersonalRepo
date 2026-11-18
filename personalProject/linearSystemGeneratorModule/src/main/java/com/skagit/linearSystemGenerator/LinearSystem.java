package com.skagit.linearSystemGenerator;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Random;

import com.skagit.advMath.mitLinearAlgebra.PaqLu;
import com.skagit.utils.MyFraction;
import com.skagit.utils.StringUtilities;

public class LinearSystem {
	final String[] _equtnNames;
	final HashMap<String, Integer> _equtnNameToIdx;
	final String[] _vblNames;
	final HashMap<String, Integer> _vblNameToIdx;
	final MyFraction[][] _coefs;
	final MyFraction[] _guessedValues;
	final MyFraction[] _rhs;
	final Difficulty _difficulty;
	LsStatus _lsStatus;
	boolean _haveSeenInfeasible;
	boolean _haveSeenDoTell;
	boolean _askingForHelp;
	boolean _askingToQuit;
	boolean _askingToGiveUp;

	public LinearSystem(final double[][] coefsDbl, final double[] rhsDbl, final Difficulty difficulty) {
		final MyFraction[][] coefs = LinearSystemStatics.toFractions(coefsDbl);
		final MyFraction[] rhs = LinearSystemStatics.toFractions(rhsDbl);
		/** permute rows and columns. */
		final int m = coefs.length, n = coefs[0].length;
		final String[] equtnNames = LinearSystemStatics.getNominalEqutnNames(m);
		final String[] vblNames = LinearSystemStatics.getNominalVblNames(n);

		final LinearSystem linearSystem = new LinearSystem(//
				equtnNames, //
				coefs, //
				rhs, //
				vblNames, //
				/* guessedValues= */null, //
				difficulty //
		);
		_equtnNames = linearSystem._equtnNames;
		_equtnNameToIdx = linearSystem._equtnNameToIdx;
		_vblNames = linearSystem._vblNames;
		_vblNameToIdx = linearSystem._vblNameToIdx;
		_coefs = linearSystem._coefs;
		_guessedValues = linearSystem._guessedValues;
		_rhs = linearSystem._rhs;
		_difficulty = difficulty;
		_lsStatus = linearSystem._lsStatus;
		_haveSeenInfeasible = linearSystem._haveSeenInfeasible;
		_haveSeenDoTell = linearSystem._haveSeenDoTell;
		_askingForHelp = linearSystem._askingForHelp;
		_askingToQuit = linearSystem._askingToQuit;
		_askingToGiveUp = linearSystem._askingToGiveUp;
	}

	public LinearSystem(final String[] equtnNames, final MyFraction[][] coefs, final MyFraction[] rhs,
			final String[] vblNames, MyFraction[] guessedValues, final Difficulty difficulty) {
		final int m = coefs.length, n = coefs[0].length;
		_equtnNames = equtnNames == null ? LinearSystemStatics.getNominalEqutnNames(m) : equtnNames.clone();
		_coefs = new MyFraction[m][];
		_rhs = new MyFraction[m];
		_equtnNameToIdx = new HashMap<>();
		for (int i = 0; i < m; ++i) {
			_coefs[i] = new MyFraction[n];
			for (int j = 0; j < n; ++j) {
				_coefs[i][j] = coefs[i][j].clone();
			}
			_rhs[i] = rhs[i].clone();
			_equtnNameToIdx.put(_equtnNames[i], i);
		}

		_vblNames = vblNames == null ? LinearSystemStatics.getNominalVblNames(n) : vblNames.clone();
		if (guessedValues == null) {
			guessedValues = new MyFraction[n];
			Arrays.fill(guessedValues, null);
		}
		_vblNameToIdx = new HashMap<>();
		_guessedValues = new MyFraction[n];
		for (int j = 0; j < n; ++j) {
			final MyFraction guessedValue = guessedValues[j];
			_guessedValues[j] = guessedValue == null ? null : guessedValue.clone();
			_vblNameToIdx.put(_vblNames[j], j);
		}
		_difficulty = difficulty;
		_lsStatus = LsStatus.FEASIBLE;
		_haveSeenInfeasible = false;
		_haveSeenDoTell = false;
		_askingForHelp = true;
		_askingToQuit = false;
		_askingToGiveUp = false;
	}

	PaqLu getPaqLu() {
		final int m = _coefs.length, n = _coefs[0].length;
		final BitSet realVbls = new BitSet(n);
		for (int j = 0; j < n; ++j) {
			realVbls.set(j, _guessedValues[j] == null);
		}
		final int nRealVbls = realVbls.cardinality();

		/** An equation is real if it has at least one nonzero coef with a real vble. */
		final BitSet realEqutns = new BitSet(m);
		realEqutns.clear();
		for (int i = 0; i < m; ++i) {
			for (int j = realVbls.nextSetBit(0); j >= 0; j = realVbls.nextSetBit(j + 1)) {
				if (_coefs[i][j]._num != 0) {
					realEqutns.set(i);
					break;
				}
			}
		}
		final int mRealEqutns = realEqutns.cardinality();

		final MyFraction[][] realCoefs = new MyFraction[mRealEqutns][];
		for (int i = realEqutns.nextSetBit(0), i0 = 0; i >= 0; i = realEqutns.nextSetBit(i + 1), ++i0) {
			realCoefs[i0] = new MyFraction[nRealVbls];
			for (int j = realVbls.nextSetBit(0), j0 = 0; j >= 0; j = realVbls.nextSetBit(j + 1), ++j0) {
				realCoefs[i0][j0] = _coefs[i][j];
			}
		}
		return new PaqLu(LinearSystemStatics.toDouble(realCoefs), PaqLu.Pivoting.Full);
	}

	public String getString() {
		final int m = _coefs.length;
		final int n = _coefs[0].length;

		/** A variable is real if it has no guess. */
		final BitSet realVbls = new BitSet(n);
		for (int j = 0; j < n; ++j) {
			realVbls.set(j, _guessedValues[j] == null);
		}
		final int nRealVbls = realVbls.cardinality();

		/**
		 * <pre>
		 * An equation is real if either of the two occurs:
		 * 1. It has at least one nonzero coef with a real vble, or
		 * 2. rhs is not 0.
		 * </pre>
		 */
		final BitSet realEqutns = new BitSet(m);
		realEqutns.clear();
		for (int i = 0; i < m; ++i) {
			for (int j = realVbls.nextSetBit(0); j >= 0; j = realVbls.nextSetBit(j + 1)) {
				if (_coefs[i][j]._num != 0) {
					realEqutns.set(i);
					break;
				}
			}
			if (!realEqutns.get(i)) {
				/** LHS of equtn i is 0. If RHS is not zero, then it is a real equtn. */
				realEqutns.set(i, _rhs[i]._num != 0);
			}
		}
		final int mRealEqutns = realEqutns.cardinality();

		final String[] realEqutnNames = new String[mRealEqutns];
		final MyFraction[][] realCoefs = new MyFraction[mRealEqutns][];
		final MyFraction[] realRhs = new MyFraction[mRealEqutns];
		for (int i = realEqutns.nextSetBit(0), i0 = 0; i >= 0; i = realEqutns.nextSetBit(i + 1), ++i0) {
			realEqutnNames[i0] = _equtnNames[i];
			realCoefs[i0] = new MyFraction[nRealVbls];
			for (int j = realVbls.nextSetBit(0), j0 = 0; j >= 0; j = realVbls.nextSetBit(j + 1), ++j0) {
				realCoefs[i0][j0] = _coefs[i][j];
			}
			realRhs[i0] = _rhs[i];
		}

		final String[] realVblNames = new String[nRealVbls];
		final MyFraction[] realGuessedValues = new MyFraction[nRealVbls];
		for (int j = realVbls.nextSetBit(0), j0 = 0; j >= 0; j = realVbls.nextSetBit(j + 1), ++j0) {
			realVblNames[j0] = _vblNames[j];
			realGuessedValues[j0] = _guessedValues[j];
		}
		final StringBuilder miscSb = new StringBuilder();
		if (mRealEqutns > 0 && nRealVbls > 0) {
			final PaqLu paqLu = new PaqLu(LinearSystemStatics.toDouble(realCoefs));
			miscSb.append(String.format("%s; Det[%f], Rank[%d] Difficulty%s", _lsStatus._s, paqLu.getDeterminant(),
					paqLu.getRank(), _difficulty.getString()));
		} else {
			miscSb.append(String.format("%s; Difficulty%s", _lsStatus._s, _difficulty.getString()));
		}

		/**
		 * Find the column indices of the first nonzeros in each row, and the column
		 * index of the first nonzero of the entire LHS.
		 */
		int firstNonZero = nRealVbls;
		final int[] firstNonZeros = new int[mRealEqutns];
		for (int i0 = 0; i0 < mRealEqutns; ++i0) {
			firstNonZeros[i0] = -1;
			for (int j0 = 0; j0 < nRealVbls; ++j0) {
				if (realCoefs[i0][j0]._num != 0d) {
					firstNonZeros[i0] = j0;
					firstNonZero = Math.min(firstNonZero, j0);
					break;
				}
			}
		}

		/** Get the max equtn name and then pad the equtnNames. */
		int maxEqutnName = 0;
		for (int i0 = 0; i0 < mRealEqutns; ++i0) {
			final String equtnName = realEqutnNames[i0];
			maxEqutnName = Math.max(maxEqutnName, equtnName.length());
		}
		for (int i0 = 0; i0 < mRealEqutns; ++i0) {
			realEqutnNames[i0] = StringUtilities.padLeft(realEqutnNames[i0], maxEqutnName);
		}

		/**
		 * Column j needs an intro string (3*" ", " + ", or " - ") if some row i has a
		 * non-zero entry in column j, and this is not the first non-zero in row i.
		 */
		final boolean[] needIntroStrings = new boolean[nRealVbls];
		for (int j0 = 0; j0 < nRealVbls; ++j0) {
			needIntroStrings[j0] = false;
			for (int i0 = 0; i0 < mRealEqutns; ++i0) {
				if ((realCoefs[i0][j0]._num != 0d) && (j0 > firstNonZeros[i0])) {
					needIntroStrings[j0] = true;
					break;
				}
			}
		}

		/**
		 * We need an extra blank if we are the first non-zero in our row and no other
		 * row needs a " + " or a " - " in our column.
		 */
		final boolean[][] needExtraBlanks = new boolean[mRealEqutns][nRealVbls];
		for (int j0 = 0; j0 < nRealVbls; ++j0) {
			for (int i0 = 0; i0 < mRealEqutns; ++i0) {
				needExtraBlanks[i0][j0] = j0 == firstNonZeros[i0] && !needIntroStrings[j0];
			}
		}
		/** Get the nominal strings. */
		final String[][] mtxStrings = new String[mRealEqutns][];
		for (int i0 = 0; i0 < mRealEqutns; ++i0) {
			mtxStrings[i0] = new String[nRealVbls];
			Arrays.fill(mtxStrings[i0], null);
		}
		for (int i0 = 0; i0 < mRealEqutns; ++i0) {
			for (int j0 = 0; j0 < nRealVbls; ++j0) {
				final MyFraction coef = realCoefs[i0][j0];
				final long coefNum = coef._num;
				if (coefNum == 0d) {
					mtxStrings[i0][j0] = "";
				} else {
					final StringBuilder s = new StringBuilder();
					if (j0 == firstNonZeros[i0]) {
						if (needExtraBlanks[i0][j0]) {
							s.append(' ');
						}
						if (coefNum < 0d) {
							s.append('-');
						}
					}
					final MyFraction absCoef = coef.abs();
					if (absCoef._num != 1L || absCoef._den != 1L) {
						s.append(absCoef.getString(/* printFloat= */false));
					}
					s.append(realVblNames[j0]);
					mtxStrings[i0][j0] = s.toString();
				}
			}
		}
		/**
		 * If for some i0, all mtxStrings[i0][j0]="" for all j0, and rhs[i0] != 0, then
		 * set the last mtxStrings[j0] to "0", and mark this as a bad row. At this point
		 * we know that if the former condition holds, then the latter cannot since we
		 * are restricted to realEqutns. Hence, we check only the former condition.
		 */
		final BitSet badRows = new BitSet(mRealEqutns);
		badRows.clear();
		LOOP1: for (int i0 = 0; i0 < mRealEqutns; ++i0) {
			for (int j0 = 0; j0 < nRealVbls; ++j0) {
				if (mtxStrings[i0][j0].length() > 0) {
					continue LOOP1;
				}
			}
			badRows.set(i0);
		}
		int nLhsClmns = nRealVbls;
		if (!badRows.isEmpty() && nRealVbls == 0) {
			/** We need a variable to put a 0 in. Use the last one. */
			for (int i0 = 0; i0 < mRealEqutns; ++i0) {
				mtxStrings[i0] = new String[1];
				mtxStrings[i0][0] = badRows.get(i0) ? "0" : "";
			}
			nLhsClmns = 1;
		}

		final int[] clmnMaxes = new int[nLhsClmns];
		for (int j0 = 0; j0 < nLhsClmns; ++j0) {
			clmnMaxes[j0] = 0;
			for (int i0 = 0; i0 < mRealEqutns; ++i0) {
				clmnMaxes[j0] = Math.max(clmnMaxes[j0], mtxStrings[i0][j0].length());
			}
		}

		/** Pad mtxStrings. */
		for (int i0 = 0; i0 < mRealEqutns; ++i0) {
			for (int j0 = 0; j0 < nLhsClmns; ++j0) {
				final int clmnMax = clmnMaxes[j0];
				if (clmnMax > 0) {
					mtxStrings[i0][j0] = StringUtilities.padLeft(mtxStrings[i0][j0], clmnMax);
				}
			}
		}

		/** Get the nominal rhs strings, compute the max, and then pad them. */
		int maxRhs = 0;
		final String[] rhsStrings = new String[mRealEqutns];
		for (int i0 = 0; i0 < mRealEqutns; ++i0) {
			final String rhsString = realRhs[i0].getString(/* printFloat= */false);
			maxRhs = Math.max(maxRhs, rhsString.length());
			rhsStrings[i0] = rhsString;
		}
		for (int i0 = 0; i0 < mRealEqutns; ++i0) {
			rhsStrings[i0] = StringUtilities.padLeft(rhsStrings[i0], maxRhs);
		}

		/** Finally, write it out. */
		final StringBuilder bigSb = new StringBuilder();
		for (int i0 = 0; i0 < mRealEqutns; ++i0) {
			if (i0 == 0) {
				bigSb.append(String.format("   The Equations and RHS (%s):\n", miscSb.toString()));
			} else {
				bigSb.append('\n');
			}
			bigSb.append(realEqutnNames[i0]).append("  ");
			for (int j0 = 0; j0 < nLhsClmns; ++j0) {
				final int clmnMax = clmnMaxes[j0];
				if (clmnMax > 0) {
					if (j0 < nRealVbls) {
						final MyFraction coef = realCoefs[i0][j0];
						if (needIntroStrings[j0]) {
							final long coefNum = coef._num;
							if (coefNum == 0d || j0 == firstNonZeros[i0]) {
								bigSb.append("   ");
							} else {
								bigSb.append(coefNum > 0d ? " + " : " - ");
							}
						}
					}
					bigSb.append(mtxStrings[i0][j0]);
				}
			}
			bigSb.append(" = ").append(rhsStrings[i0]);
			if (badRows.get(i0)) {
				bigSb.append("(Bad)");
			}
		}

		/** Print guesses if there are any. */
		final StringBuilder guessSb = new StringBuilder();
		boolean doneIntro = false;
		for (int j = 0; j < n; ++j) {
			final MyFraction guessedValue = _guessedValues[j];
			if (guessedValue != null) {
				if (!doneIntro) {
					guessSb.append("\nYour values so far:");
					doneIntro = true;
				}
				guessSb.append(
						String.format("\n%s = %s", _vblNames[j], guessedValue.getString(/* printFloats= */true)));
			}
		}
		bigSb.append(guessSb);
		return bigSb.toString();
	}

	@Override
	public String toString() {
		return getString();
	}

	public int getEqutnIdx(final String equtnName) {
		final Integer integer = _equtnNameToIdx.get(equtnName.toUpperCase());
		return integer == null ? -1 : integer;
	}

	public int getVblIdx(final String vblName) {
		final Integer integer = _vblNameToIdx.get(vblName.toLowerCase());
		return integer == null ? -1 : integer;
	}

	public String getEqutnName(final int i) {
		return (i < 0 || i >= _equtnNames.length) ? null : _equtnNames[i];
	}

	public String getVblName(final int j) {
		return (j < 0 || j >= _vblNames.length) ? null : _vblNames[j];
	}

	private LsStatus computeLsStatus() {
		final int m = _coefs.length, n = _coefs[0].length;
		final BitSet realVbls = new BitSet(n);
		for (int j = 0; j < n; ++j) {
			realVbls.set(j, _guessedValues[j] == null);
		}
		final int nRealVbls = realVbls.cardinality();
		final BitSet realEqutns = new BitSet(m);
		NEXT_I: for (int i = 0; i < m; ++i) {
			realEqutns.clear(i);
			for (int j = realVbls.nextSetBit(0); j >= 0; j = realVbls.nextSetBit(j + 1)) {
				if (_coefs[i][j]._num != 0L) {
					realEqutns.set(i);
					continue NEXT_I;
				}
			}
		}
		/** If some dead equation is violated, return fail. */
		for (int i = realEqutns.nextClearBit(0); i < m; i = realEqutns.nextClearBit(i + 1)) {
			if (_rhs[i]._num != 0) {
				return LsStatus.INFEASIBLE;
			}
		}
		if (nRealVbls == 0) {
			return LsStatus.SOLVED;
		}
		/** Use PaqLu to check the feasibility of the rest. */
		final int mRealEqutns = realEqutns.cardinality();
		final double[][] paqLuCoefs = new double[mRealEqutns][nRealVbls];
		final double[][] rhsRows = new double[mRealEqutns][1];
		for (int i = realEqutns.nextSetBit(0), ix = 0; i >= 0; i = realEqutns.nextSetBit(i + 1), ++ix) {
			for (int j = realVbls.nextSetBit(0), jx = 0; j >= 0; j = realVbls.nextSetBit(j + 1), ++jx) {
				paqLuCoefs[ix][jx] = _coefs[i][j].toDouble();
			}
			rhsRows[ix][0] = _rhs[i].toDouble();
		}
		final PaqLu paqLu = new PaqLu(paqLuCoefs, PaqLu.Pivoting.Full);
		final double[][] answerRows = paqLu.solve(rhsRows);
		for (int j = 0; j < nRealVbls; ++j) {
			if (!Double.isFinite(answerRows[j][0])) {
				return LsStatus.INFEASIBLE;
			}
		}
		return LsStatus.FEASIBLE;
	}

	MyFraction[] completeASolution() {
		if (_lsStatus == LsStatus.SOLVED) {
			return _guessedValues;
		}
		final LsStatus oldLsStatus = _lsStatus;
		final boolean _oldHaveSeenInfeasible = _haveSeenInfeasible;
		final MyFraction[] oldGuessedValues = _guessedValues.clone();
		final int n = _coefs[0].length;
		for (int j = 0; j < n && _lsStatus == LsStatus.INFEASIBLE; ++j) {
			if (_guessedValues[j] != null) {
				doSetOrUnset(j, null);
			}
		}
		for (int j = 0; j < n; ++j) {
			if (_guessedValues[j] == null) {
				doSetOrUnset(j, findValueForVbl(j));
			}
		}
		final MyFraction[] answer = _guessedValues.clone();
		for (int j = 0; j < n; ++j) {
			doSetOrUnset(j, oldGuessedValues[j]);
		}
		_lsStatus = oldLsStatus;
		_haveSeenInfeasible = _oldHaveSeenInfeasible;
		return answer;
	}

	/** B += 2A or B -= 2A. */
	void doAddOrSubtract(final Command command) {
		final int iB = command._equtn0Idx;
		final int iA = command._equtn1Idx;
		final Operator operator = command._commandType._operator;
		final MyFraction frctn = command._frctn;
		final int n = _coefs[0].length;
		for (int j = 0; j < n; ++j) {
			final MyFraction frctnB = _coefs[iB][j];
			final MyFraction frctnA = _coefs[iA][j];
			if (operator == Operator.ADD) {
				frctnB.add(frctnA.multiply(frctn, /* operateOnSelf= */false), true);
			} else {
				frctnB.subtract(frctnA.multiply(frctn, /* operateOnSelf= */false), true);
			}
		}
		final MyFraction frctnB = _rhs[iB];
		final MyFraction frctnA = _rhs[iA];
		if (operator == Operator.ADD) {
			frctnB.add(frctnA.multiply(frctn, /* operateOnSelf= */false), true);
		} else {
			frctnB.subtract(frctnA.multiply(frctn, /* operateOnSelf= */false), true);
		}
	}

	/** Use A to Zero out x in B", "A x :: B */
	boolean doZeroOut(final Command command) {
		final int iA = command._equtn0Idx;
		final int jX = command._vbl0Idx;
		if (_coefs[iA][jX]._num == 0) {
			return false;
		}
		final int iB = command._equtn1Idx;
		final MyFraction multiplier = _coefs[iB][jX].divide(_coefs[iA][jX], /* operateOnSelf= */false);
		final int n = _coefs[0].length;
		for (int j = 0; j < n; ++j) {
			final MyFraction frctnB = _coefs[iB][j];
			final MyFraction frctnA = _coefs[iA][j];
			frctnB.subtract(frctnA.multiply(multiplier, /* operateOnSelf= */false), /* operateOnSelf= */true);
		}
		final MyFraction frctnB = _rhs[iB];
		final MyFraction frctnA = _rhs[iA];
		frctnB.subtract(frctnA.multiply(multiplier, /* operateOnSelf= */false), /* operateOnSelf= */true);
		return true;
	}

	/** A *= 2 or A /= 2. */
	void doMultiplyOrDivide(final Command command) {
		final int iA = command._equtn0Idx;
		final Operator operator = command._commandType._operator;
		final MyFraction frctn = command._frctn;
		final int n = _coefs[0].length;
		for (int j = 0; j < n; ++j) {
			final MyFraction frctnA = _coefs[iA][j];
			if (operator == Operator.MULTIPLY) {
				frctnA.multiply(frctn, /* operateOnSelf= */true);
			} else {
				frctnA.divide(frctn, /* operateOnSelf= */true);
			}
		}
		final MyFraction frctnA = _rhs[iA];
		if (operator == Operator.MULTIPLY) {
			frctnA.multiply(frctn, /* operateOnSelf= */true);
		} else {
			frctnA.divide(frctn, /* operateOnSelf= */true);
		}
	}

	/** A <-> B */
	void doSwitchEqutns(final Command command) {
		final int i0 = command._equtn0Idx;
		final int i1 = command._equtn1Idx;
		final String equtnName0 = _equtnNames[i0];
		final String equtnName1 = _equtnNames[i1];
		_equtnNames[i0] = equtnName1;
		_equtnNames[i1] = equtnName0;
		_equtnNameToIdx.put(equtnName1, i0);
		_equtnNameToIdx.put(equtnName0, i1);
		final MyFraction[] coefs = _coefs[i0];
		_coefs[i0] = _coefs[i1];
		_coefs[i1] = coefs;
		final MyFraction rhs = _rhs[i0];
		_rhs[i0] = _rhs[i1];
		_rhs[i1] = rhs;
	}

	/** x <-> y */
	void doSwitchVbls(final Command command) {
		final int j0 = command._vbl0Idx;
		final int j1 = command._vbl1Idx;
		final int m = _coefs.length;
		final String vblName0 = _vblNames[j0];
		final String vblName1 = _vblNames[j1];
		_vblNames[j0] = vblName1;
		_vblNames[j1] = vblName0;
		_vblNameToIdx.put(vblName1, j0);
		_vblNameToIdx.put(vblName0, j1);
		for (int i = 0; i < m; ++i) {
			final MyFraction coef = _coefs[i][j0];
			_coefs[i][j0] = _coefs[i][j1];
			_coefs[i][j1] = coef;
		}
		final MyFraction guessedValue = _guessedValues[j0];
		_guessedValues[j0] = _guessedValues[j1];
		_guessedValues[j1] = guessedValue;
	}

	void doTell(final Command command) {
		final int j = command._vbl0Idx;
		final MyFraction frctn = findValueForVbl(j);
		if (frctn != null) {
			doSetOrUnset(j, frctn);
		}
		_haveSeenDoTell = true;
	}

	private MyFraction findValueForVbl(final int j0) {
		if (_lsStatus != LsStatus.FEASIBLE) {
			return null;
		}
		if (_guessedValues[j0] != null) {
			return _guessedValues[j0];
		}
		final int m = _coefs.length, n = _coefs[0].length;

		doSetOrUnset(j0, null);
		final BitSet realVbls = new BitSet(n);
		realVbls.clear();
		for (int j = 0; j < n; ++j) {
			realVbls.set(j, _guessedValues[j] == null);
		}
		final int nRealVbls = realVbls.cardinality();
		int keyJ = -1;
		for (int jOrig = realVbls.nextSetBit(0), j = 0; jOrig >= 0; jOrig = realVbls.nextSetBit(jOrig + 1), ++j) {
			if (jOrig == j0) {
				keyJ = j;
			}
		}

		final BitSet realEqutns = new BitSet(m);
		realEqutns.clear();
		for (int i = 0; i < m; ++i) {
			for (int j = realVbls.nextSetBit(0); j >= 0; j = realVbls.nextSetBit(j + 1)) {
				if (_coefs[i][j]._num != 0) {
					realEqutns.set(i);
					break;
				}
			}
		}
		final int mRealEqutns = realEqutns.cardinality();

		final MyFraction[][] realCoefs = new MyFraction[mRealEqutns][];
		final double[][] rhsRows = new double[mRealEqutns][];
		for (int iReal = realEqutns.nextSetBit(0), i = 0; iReal >= 0; iReal = realEqutns.nextSetBit(iReal + 1), ++i) {
			realCoefs[i] = new MyFraction[nRealVbls];
			for (int jOrig = realVbls.nextSetBit(0), j = 0; jOrig >= 0; jOrig = realVbls.nextSetBit(jOrig + 1), ++j) {
				realCoefs[i][j] = _coefs[iReal][jOrig];
			}
			rhsRows[i] = new double[1];
			rhsRows[i][0] = _rhs[iReal].toDouble();
		}
		final PaqLu paqLu = new PaqLu(LinearSystemStatics.toDouble(realCoefs), PaqLu.Pivoting.Full);
		final double[][] xRows = paqLu.solve(rhsRows);
		return new MyFraction(xRows[keyJ][0], LinearSystemStatics._ThresholdForFraction);
	}

	void doSetOrUnset(final Command command) {
		doSetOrUnset(command._vbl0Idx, command._frctn);
	}

	void doGiveUp(final Command command) {
		_askingToGiveUp = true;
	}

	void doHelp(final Command command) {
		turnOnAskingForHelp();
	}

	void doQuit(final Command command) {
		_askingToQuit = true;
	}

	private void doSetOrUnset(final int j, final MyFraction frctn) {
		final int m = _coefs.length;
		final MyFraction delta = new MyFraction(0L, 1L);
		final MyFraction oldGuess = _guessedValues[j];
		if (oldGuess != null) {
			delta.add(oldGuess, /* operateOnSelf= */true);
		}
		if (frctn != null) {
			delta.subtract(frctn, /* operateOnSelf= */true);
		}
		for (int i = 0; i < m; ++i) {
			_rhs[i].add(delta.multiply(_coefs[i][j], /* operateOnSelf= */false), /* operateOnSelf= */true);
		}
		_guessedValues[j] = frctn;
		_lsStatus = computeLsStatus();
		if (_lsStatus == LsStatus.INFEASIBLE) {
			_haveSeenInfeasible = true;
		}
	}

	void turnOnAskingForHelp() {
		_askingForHelp = true;
	}

	void turnOffAskingForHelp() {
		_askingForHelp = false;
	}

	String selectRemark(final Random r) {
		final boolean positive = !_askingToGiveUp && !_haveSeenDoTell && !_haveSeenInfeasible;
		if (positive) {
			return LinearSystemStatics._RemarksForSolved[r.nextInt(LinearSystemStatics._RemarksForSolved.length)];
		}
		return LinearSystemStatics._RemarksForInfeasible[r.nextInt(LinearSystemStatics._RemarksForInfeasible.length)];
	}

	boolean isSolved() {
		return _lsStatus == LsStatus.SOLVED;
	}

	final private static String _Clmn0Hdr = "To do this";
	final private static int _Clmn0HdrLen = _Clmn0Hdr.length();
	final private static String _Clmn1Hdr = "Enter this";
	final private static int _Clmn1HdrLen = _Clmn1Hdr.length();
	final private static String _StringBetweenClmns = "   ";

	String getHelpString() {
		if (!_askingForHelp) {
			return "";
		}
		int clmn0Len = _Clmn0Hdr.length();
		int clmn1Len = _Clmn1Hdr.length();
		for (final CommandType commandType : CommandType._StandardCommandTypes) {
			clmn0Len = Math.max(clmn0Len, commandType._englishExample.length());
			clmn1Len = Math.max(clmn1Len, commandType._commandExample.length());
		}
		final int ltPad0 = (clmn0Len - _Clmn0HdrLen) / 2;
		String fullLtHdr = (ltPad0 > 0 ? StringUtilities.padLeft("", ltPad0) : "") + _Clmn0Hdr;
		final int rtPad0 = clmn0Len - fullLtHdr.length();
		if (rtPad0 > 0) {
			fullLtHdr += StringUtilities.padRight("", rtPad0);
		}
		final StringBuilder s = new StringBuilder("  ").append(fullLtHdr).append(_StringBetweenClmns);
		final int ltPad1 = (clmn1Len - _Clmn1HdrLen) / 2;
		final String fullRtHdr = (ltPad1 > 0 ? StringUtilities.padLeft("", ltPad1) : "") + _Clmn1Hdr;
		s.append(fullRtHdr);
		for (final CommandType commandType : CommandType._StandardCommandTypes) {
			final String f = "\n  %-" + clmn0Len + "s%s%s";
			s.append(String.format(f, commandType._englishExample, _StringBetweenClmns, commandType._commandExample));
		}
		return s.toString();
	}

	public boolean getEasier() {
		return _haveSeenDoTell || _haveSeenInfeasible || _askingToGiveUp;
	}

}