package com.skagit.linearSystemGenerator;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Random;

import com.skagit.advMath.mitLinearAlgebra.PaqLu;
import com.skagit.utils.CombinatoricTools;
import com.skagit.utils.MyFraction;
import com.skagit.utils.StringUtilities;

public class LinearSystem {
	final String[] _equtnNames;
	final HashMap<String, Integer> _equtnNameToIdx;
	final String[] _vblNames;
	final HashMap<String, Integer> _vblNameToIdx;
	/** If _permForEqutns[0] = 3, then equation #3 is printed first. */
	final int[] _permForEqutns;
	final int[] _permForVbls;
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
				/* permForEqutns= */null, //
				rhs, //
				vblNames, //
				/* guessedValues= */null, //
				/* permForVbls= */null, //
				difficulty //
		);
		_equtnNames = linearSystem._equtnNames;
		_equtnNameToIdx = linearSystem._equtnNameToIdx;
		_vblNames = linearSystem._vblNames;
		_vblNameToIdx = linearSystem._vblNameToIdx;
		_permForEqutns = linearSystem._permForEqutns;
		_permForVbls = linearSystem._permForVbls;
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

	public LinearSystem(String[] equtnNames, final MyFraction[][] coefs, int[] permForEqutns, final MyFraction[] rhs,
			String[] vblNames, MyFraction[] guessedValues, int[] permForVbls, final Difficulty difficulty) {
		final int m = coefs.length, n = coefs[0].length;
		permForEqutns = permForEqutns == null ? CombinatoricTools.getInitial(m) : permForEqutns;
		permForVbls = permForVbls == null ? CombinatoricTools.getInitial(n) : permForVbls;
		equtnNames = equtnNames == null ? LinearSystemStatics.getNominalEqutnNames(m) : equtnNames;
		_coefs = new MyFraction[m][];
		_rhs = new MyFraction[m];
		_equtnNames = new String[m];
		_equtnNameToIdx = new HashMap<>();
		for (int i = 0; i < m; ++i) {
			_coefs[i] = new MyFraction[n];
			final int permedI = permForEqutns[i];
			_equtnNames[i] = equtnNames[permedI];
			for (int j = 0; j < n; ++j) {
				final int permedJ = permForVbls[j];
				_coefs[i][j] = coefs[permedI][permedJ].clone();
			}
			_rhs[i] = rhs[permedI].clone();
			_equtnNameToIdx.put(_equtnNames[i], i);
		}
		_permForEqutns = CombinatoricTools.getInitial(m);

		vblNames = vblNames == null ? LinearSystemStatics.getNominalVblNames(n) : vblNames;
		if (guessedValues == null) {
			guessedValues = new MyFraction[n];
			Arrays.fill(guessedValues, null);
		}
		_vblNameToIdx = new HashMap<>();
		_vblNames = new String[n];
		_guessedValues = new MyFraction[n];
		for (int j = 0; j < n; ++j) {
			final int permedForJ = permForVbls[j];
			_vblNames[j] = vblNames[permedForJ];
			final MyFraction guessedValue = guessedValues[permedForJ];
			_guessedValues[j] = guessedValue == null ? null : guessedValue.clone();
			_vblNameToIdx.put(_vblNames[j], j);
		}
		_permForVbls = CombinatoricTools.getInitial(n);
		_difficulty = difficulty;
		_lsStatus = LsStatus.FEASIBLE;
		_haveSeenInfeasible = false;
		_haveSeenDoTell = false;
		_askingForHelp = true;
		_askingToQuit = false;
		_askingToGiveUp = false;
	}

	PaqLu getPaqLu() {
		final LinearSystem permuted = permute();
		final MyFraction[][] coefs = permuted._coefs;
		final int m = coefs.length, n = coefs[0].length;
		final MyFraction[] guessedValues = permuted._guessedValues;
		final BitSet realVbls = new BitSet(n);
		realVbls.clear();
		for (int j = 0; j < n; ++j) {
			realVbls.set(j, guessedValues[j] == null);
		}
		final int nRealVbls = realVbls.cardinality();

		/** An equation is real if it has at least one nonzero coef with a real vble. */
		final BitSet realEqutns = new BitSet(m);
		realEqutns.clear();
		for (int i = 0; i < m; ++i) {
			for (int j = realVbls.nextSetBit(0); j >= 0; j = realVbls.nextSetBit(j + 1)) {
				if (coefs[i][j]._num != 0) {
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
				realCoefs[i0][j0] = coefs[i][j];
			}
		}
		return new PaqLu(LinearSystemStatics.toDouble(realCoefs), PaqLu.Pivoting.Full);
	}

	private LinearSystem permute() {
		final int m = _coefs.length;
		boolean noPerm = true;
		for (int i = 0; i < m; ++i) {
			if (_permForEqutns[i] != i) {
				noPerm = false;
				break;
			}
		}
		final int n = _coefs[0].length;
		if (!noPerm) {
			for (int j = 0; j < n; ++j) {
				if (_permForVbls[j] != j) {
					noPerm = false;
					break;
				}
			}
		}
		if (noPerm) {
			return this;
		}

		return new LinearSystem(_equtnNames, _coefs, _permForEqutns, _rhs, _vblNames, _guessedValues, _permForVbls,
				_difficulty);
	}

	public String getString() {
		final LinearSystem permuted = permute();
		final MyFraction[][] coefs = permuted._coefs;
		final int m = coefs.length;
		final int n = coefs[0].length;

		/** A variable is real if it has no guess. */
		final BitSet realVbls = new BitSet(n);
		final MyFraction[] guessedValues = permuted._guessedValues;
		realVbls.clear();
		for (int j = 0; j < n; ++j) {
			realVbls.set(j, guessedValues[j] == null);
		}
		final int nRealVbls = realVbls.cardinality();
		final String[] vblNames = permuted._vblNames;

		/**
		 * <pre>
		 * An equation is real if either of the two occurs:
		 * 1. It has at least one nonzero coef with a real vble, or
		 * 2. rhs is not 0.
		 * </pre>
		 */
		final MyFraction[] rhs = permuted._rhs;
		final BitSet realEqutns = new BitSet(m);
		realEqutns.clear();
		for (int i = 0; i < m; ++i) {
			for (int j = realVbls.nextSetBit(0); j >= 0; j = realVbls.nextSetBit(j + 1)) {
				if (coefs[i][j]._num != 0) {
					realEqutns.set(i);
					break;
				}
			}
			if (!realEqutns.get(i)) {
				/** LHS of equtn i is 0. If RHS is not zero, then it is a real equtn. */
				realEqutns.set(i, rhs[i]._num != 0);
			}
		}
		final int mRealEqutns = realEqutns.cardinality();
		final String[] equtnNames = permuted._equtnNames;

		final String[] realEqutnNames = new String[mRealEqutns];
		final MyFraction[][] realCoefs = new MyFraction[mRealEqutns][];
		final MyFraction[] realRhs = new MyFraction[mRealEqutns];
		for (int i = realEqutns.nextSetBit(0), i0 = 0; i >= 0; i = realEqutns.nextSetBit(i + 1), ++i0) {
			realEqutnNames[i0] = equtnNames[i];
			realCoefs[i0] = new MyFraction[nRealVbls];
			for (int j = realVbls.nextSetBit(0), j0 = 0; j >= 0; j = realVbls.nextSetBit(j + 1), ++j0) {
				realCoefs[i0][j0] = coefs[i][j];
			}
			realRhs[i0] = rhs[i];
		}

		final String[] realVblNames = new String[nRealVbls];
		final MyFraction[] realGuessedValues = new MyFraction[nRealVbls];
		for (int j = realVbls.nextSetBit(0), j0 = 0; j >= 0; j = realVbls.nextSetBit(j + 1), ++j0) {
			realVblNames[j0] = vblNames[j];
			realGuessedValues[j0] = guessedValues[j];
		}
		final StringBuilder miscSb = new StringBuilder();
		if (mRealEqutns > 0 && nRealVbls > 0) {
			if (mRealEqutns > 0 && nRealVbls > 0) {
				final PaqLu paqLu = new PaqLu(LinearSystemStatics.toDouble(realCoefs));
				miscSb.append(String.format("Det[%f], Rank[%d] Difficulty%s", paqLu.getDeterminant(), paqLu.getRank(),
						_difficulty.getString()));
			}
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
			mtxStrings[i0][nRealVbls - 1] = "0";
		}

		final int[] clmnMaxes = new int[nRealVbls];
		for (int j0 = 0; j0 < nRealVbls; ++j0) {
			clmnMaxes[j0] = 0;
			for (int i0 = 0; i0 < mRealEqutns; ++i0) {
				clmnMaxes[j0] = Math.max(clmnMaxes[j0], mtxStrings[i0][j0].length());
			}
		}

		/** Pad mtxStrings. */
		for (int i0 = 0; i0 < mRealEqutns; ++i0) {
			for (int j0 = 0; j0 < nRealVbls; ++j0) {
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
			for (int j0 = 0; j0 < nRealVbls; ++j0) {
				final int clmnMax = clmnMaxes[j0];
				if (clmnMax > 0) {
					final MyFraction coef = realCoefs[i0][j0];
					if (needIntroStrings[j0]) {
						final long coefNum = coef._num;
						if (coefNum == 0d || j0 == firstNonZeros[i0]) {
							bigSb.append("   ");
						} else {
							bigSb.append(coefNum > 0d ? " + " : " - ");
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

	/**
	 * <pre>
	 * A <-> B: If _orderToPrintEqutns[0] = 3, then equation #3 is printed first.
	 * </pre>
	 */
	void doSwitchEqutns(final Command command) {
		final int iA = command._equtn0Idx;
		final int iB = command._equtn1Idx;
		final int m = _coefs.length;
		int kA = -1, kB = -1;
		for (int i = 0; i < m; ++i) {
			final int ix = _permForEqutns[i];
			if (ix == iA) {
				/** A is printed i-th. */
				kA = i;
			} else if (ix == iB) {
				kB = i;
			}
			if (kA >= 0 && kB >= 0) {
				break;
			}
		}
		_permForEqutns[kA] = iB;
		_permForEqutns[kB] = iA;
	}

	void doSwitchVbls(final Command command) {
		final int jA = command._vbl0Idx;
		final int jB = command._vbl1Idx;
		final int n = _coefs[0].length;
		int kA = -1, kB = -1;
		for (int j = 0; j < n; ++j) {
			final int jx = _permForVbls[j];
			if (jx == jA) {
				/** x is printed j-th. */
				kA = j;
			} else if (jx == jB) {
				kB = j;
			}
			if (kA >= 0 && kB >= 0) {
				break;
			}
		}
		_permForVbls[kA] = jB;
		_permForVbls[kB] = jA;
	}

	void doTell(final Command command) {
		if (_lsStatus != LsStatus.FEASIBLE) {
			return;
		}
		final int m = _coefs.length, n = _coefs[0].length;
		final int j0 = command._vbl0Idx;

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
		final double d = xRows[keyJ][0];
		doSetOrUnset(j0, new MyFraction(d, LinearSystemStatics._ThresholdForFraction));
		_haveSeenDoTell = true;
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
		if (frctn != null) {
			_lsStatus = computeLsStatus();
			if (_lsStatus == LsStatus.INFEASIBLE) {
				_haveSeenInfeasible = true;
			}
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