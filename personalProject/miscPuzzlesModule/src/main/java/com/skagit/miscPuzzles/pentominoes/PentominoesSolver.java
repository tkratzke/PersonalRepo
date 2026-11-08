package com.skagit.miscPuzzles.pentominoes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.skagit.utils.CombinatoricTools;
import com.skagit.utils.StringUtilities;

public class PentominoesSolver {
	public final Pentominoes _pentominoPuzzle;
	private final TreeSet<Piece> _availablePieces = new TreeSet<>();
	private final String[][] _coveredBys;
	/** Keeping track of solutions. */
	private final TreeMap<Integer, int[]> _histogram;
	private final Random _r;
	private int _nSolutions;
	private final int _solutionNumber;
	private int _kSolution;
	private String[][] _recordedSolution = null;
	private final long _seed;

	public PentominoesSolver(final Pentominoes pentominoPuzzle, final boolean seedIsSet, final long seed,
			final int solutionNumber) {
		_pentominoPuzzle = pentominoPuzzle;
		_availablePieces.addAll(Arrays.asList(_pentominoPuzzle._pieces));
		_coveredBys = new String[_pentominoPuzzle._nRows][_pentominoPuzzle._nClmns];
		_pentominoPuzzle.fillInInitialCoveredBys(_coveredBys);
		_histogram = new TreeMap<>();
		_recordedSolution = null;
		_nSolutions = _kSolution = 0;
		_recordedSolution = null;
		if (!seedIsSet) {
			_seed = (new Random()).nextLong() >>> 1;
		} else {
			_seed = seed;
		}
		_r = new Random(_seed);
		_solutionNumber = solutionNumber;
	}

	public void solve() {
		recursiveSolve(/* level= */0, /* count0= */null);
	}

	void recursiveSolve(final int level, final int[] count0) {
		final int nRows = _pentominoPuzzle._nRows;
		final int nClmns = _pentominoPuzzle._nClmns;
		int row = -1, clmn = -1;
		boolean foundNotCoveredSquare = false;
		OUTSIDE_LOOOP: for (row = 0; row < nRows; ++row) {
			for (clmn = 0; clmn < nClmns; ++clmn) {
				if (_coveredBys[row][clmn] == TypeOfPiece._NotCovered) {
					foundNotCoveredSquare = true;
					break OUTSIDE_LOOOP;
				}
			}
		}
		final int nAvailablePieces = _availablePieces.size();
		final int[] count = level > 0 ? count0 : new int[] { 0 };
		if (nAvailablePieces == 0 || !foundNotCoveredSquare) {
			/** We have a solution. */
			++count[0];
			final boolean recordThisOne;
			if (_solutionNumber >= 0) {
				recordThisOne = _nSolutions == _solutionNumber;
			} else {
				recordThisOne = _r.nextInt(_nSolutions + 1) == 0;
			}
			if (recordThisOne) {
				_kSolution = _nSolutions;
				_recordedSolution = StringUtilities.deepCopy(_coveredBys);
			}
			++_nSolutions;
		}

		final int[] topLeft = { row, clmn };
		final Piece[] availablePieces = _availablePieces.toArray(new Piece[nAvailablePieces]);
		for (int kPiece = 0; kPiece < nAvailablePieces; ++kPiece) {
			final Piece piece = availablePieces[kPiece];
			final TypeOfPiece.Orientation[] orientations = piece._typeOfPiece._orientations;
			final int nOrientations = orientations.length;
			for (int kOrientation = 0; kOrientation < nOrientations; ++kOrientation) {
				final TypeOfPiece.Orientation orientation = orientations[kOrientation];
				if (canPlace(orientation, topLeft)) {
					placeOrUnplaceOrientation(piece, orientation, topLeft, /* place= */true);
					if (level == 0) {
						count[0] = 0;
					}
					recursiveSolve(level + 1, count);
					if (level == 0) {
						final Integer theCount = count[0];
						final int[] nOrientationsWithThisCount = _histogram.get(theCount);
						if (nOrientationsWithThisCount == null) {
							_histogram.put(theCount, new int[] { 1 });
						} else {
							++nOrientationsWithThisCount[0];
						}
					}
					placeOrUnplaceOrientation(piece, orientation, topLeft, /* place= */false);
				}
			}
		}
	}

	boolean canPlace(final TypeOfPiece.Orientation orientation, final int[] topLeft) {
		final int nRows = _pentominoPuzzle._nRows;
		final int nClmns = _pentominoPuzzle._nClmns;
		final int row0 = topLeft[0], clmn0 = topLeft[1];
		/** Simple fast tests: */
		if (orientation._bitSets.length + row0 > nRows) {
			return false;
		}
		if (orientation._minClmnOffset + clmn0 < 0) {
			return false;
		}
		if (orientation._maxClmnOffset + clmn0 >= nClmns) {
			return false;
		}
		/** Check square by square of orientation to see if it's vacant. */
		final BitSet[] bitSetsOfOrientation = orientation._bitSets;
		final int nBitSetsOfOrientation = bitSetsOfOrientation.length;
		final BitSet bitSetOfOrientation0 = bitSetsOfOrientation[0];
		final int clmnOfOrientation0 = bitSetOfOrientation0.nextSetBit(0);
		for (int kBitSetOfOrientation = 0; kBitSetOfOrientation < nBitSetsOfOrientation; ++kBitSetOfOrientation) {
			final BitSet bitSetOfOrientation = bitSetsOfOrientation[kBitSetOfOrientation];
			final int row1 = row0 + kBitSetOfOrientation;
			for (int clmnOfOrientation = bitSetOfOrientation.nextSetBit(0); //
					clmnOfOrientation >= 0; //
					clmnOfOrientation = bitSetOfOrientation.nextSetBit(clmnOfOrientation + 1)) //
			{
				final int clmn1 = clmn0 + (clmnOfOrientation - clmnOfOrientation0);
				if (_coveredBys[row1][clmn1] != TypeOfPiece._NotCovered) {
					return false;
				}
			}
		}
		return true;
	}

	void placeOrUnplaceOrientation(final Piece piece, final TypeOfPiece.Orientation orientation, final int[] topLeft,
			final boolean place) {
		final BitSet[] bitSetsOfOrientation = orientation._bitSets;
		final int nBitSetsOfOrientation = bitSetsOfOrientation.length;
		final BitSet bitSetOfOrientation0 = bitSetsOfOrientation[0];
		final int clmnOfOrientation0 = bitSetOfOrientation0.nextSetBit(0);
		final int row0 = topLeft[0], clmn0 = topLeft[1];
		if (place) {
			_availablePieces.remove(piece);
		} else {
			_availablePieces.add(piece);
		}
		final String indicator = place ? piece._typeOfPiece._indicator : TypeOfPiece._NotCovered;
		for (int kBitSetOfOrientation = 0; kBitSetOfOrientation < nBitSetsOfOrientation; ++kBitSetOfOrientation) {
			final BitSet bitSetOfOrientation = bitSetsOfOrientation[kBitSetOfOrientation];
			final int row1 = row0 + kBitSetOfOrientation;
			for (int clmnOfOrientation = bitSetOfOrientation.nextSetBit(0); //
					clmnOfOrientation >= 0; //
					clmnOfOrientation = bitSetOfOrientation.nextSetBit(clmnOfOrientation + 1)) //
			{
				final int clmn1 = clmn0 + (clmnOfOrientation - clmnOfOrientation0);
				_coveredBys[row1][clmn1] = indicator;
			}
		}
	}

	public String getString() {
		final StringBuilder s = new StringBuilder().append(String.format("seed/nSolutions/kSolution[%d/%d/%d]\n%s", //
				_seed, getNSolutionsInHistogram(), _kSolution,
				StringUtilities.getString(_recordedSolution, TypeOfPiece._IndicatorLength)));
		s.append("\n\nCounts:");
		for (final Integer count : _histogram.keySet()) {
			final int nOrientationsWithCount = _histogram.get(count)[0];
			s.append(String.format("\n%d Initial Orientations have %d solutions(%d).", nOrientationsWithCount, count,
					nOrientationsWithCount * count));
		}
		return s.toString();
	}

	private int getNSolutionsInHistogram() {
		int nSolutions = 0;
		for (final Integer count : _histogram.keySet()) {
			final int nOrientationsWithCount = _histogram.get(count)[0];
			nSolutions += count * nOrientationsWithCount;
		}
		return nSolutions;
	}

	@Override
	public String toString() {
		return getString();
	}

	static void addToIntPairSet(final Set<int[]> intPairSet, final String s0) {
		final String[] stringsOfSides = s0.trim().split(":");
		final int nStringsOfSides = stringsOfSides == null ? 0 : stringsOfSides.length;
		if (nStringsOfSides == 2) {
			final String sLeft = stringsOfSides[0];
			final String sRight = stringsOfSides[1];
			final BitSet bitSetLeft = CombinatoricTools.stringToBitset(sLeft);
			final BitSet bitSetRight = CombinatoricTools.stringToBitset(sRight);
			for (int kLeft = bitSetLeft.nextSetBit(0); kLeft >= 0; kLeft = bitSetLeft.nextSetBit(kLeft + 1)) {
				for (int kRight = bitSetRight.nextSetBit(0); kRight >= 0; kRight = bitSetRight.nextSetBit(kRight + 1)) {
					intPairSet.add(new int[] { kLeft, kRight });
				}
			}
		}
	}

	private static void addToPieceList(final ArrayList<Piece> pieceList, final String s) {
		final String[] fields0 = s.trim().split("[,\\s]+");
		final int nFields = fields0.length;
		for (int k = 0; k < nFields; ++k) {
			final TypeOfPiece typeOfPiece = TypeOfPiece.getTypeOfPiece(fields0[k]);
			if (typeOfPiece != null) {
				pieceList.add(new Piece(pieceList.size(), typeOfPiece));
			}
		}
	}

	public static void main(final String[] args) {
		final String demoOnlyLc = "demoOnly".toLowerCase();
		boolean demoOnly = false;
		final String nRowsLc = "nRows=".toLowerCase();
		int nRows = -1;
		final String doRectangleWithHoleLc = "doRectangleWithHole".toLowerCase();
		boolean doRectangleWithHole = false;
		final String doRectangleLc = "doRectangle".toLowerCase();
		boolean doRectangle = false;
		final String doCalendarPuzzleLc = "doCalendarPuzzle".toLowerCase();
		boolean doCalendarPuzzle = false;
		final String includeDayOfWeekLc = "includeDayOfWeek".toLowerCase();
		boolean includeDayOfWeek = false;
		final String dateStringLc = "date=".toLowerCase();
		LocalDate localDate = LocalDate.now(ZoneId.systemDefault());
		final String seedLc = "seed=";
		long seed = 0;
		boolean seedIsSet = false;
		final String solutionNumberLc = "solutionNumber=";
		int solutionNumber = -1;
		final String printOutPiecesLc = "printOutPieces".toLowerCase();
		boolean printOutPieces = false;

		final String givenLc = "given=".toLowerCase();
		final TreeSet<int[]> givensSet = new TreeSet<>(CombinatoricTools._ByAllInOrder);
		final String piecesLc = "pieces=".toLowerCase();
		final ArrayList<Piece> piecesList = new ArrayList<>();

		for (int iArg = 0; iArg < args.length; ++iArg) {
			final String arg = args[iArg];
			final String argLc = arg.toLowerCase();
			if (argLc.startsWith(demoOnlyLc)) {
				demoOnly = true;
			} else if (argLc.startsWith(nRowsLc)) {
				try {
					nRows = Integer.parseInt(arg.substring(nRowsLc.length()));
				} catch (final NumberFormatException var31) {
				}
			} else if (argLc.startsWith(doRectangleWithHoleLc)) {
				doRectangleWithHole = !doRectangleWithHole;
			} else if (argLc.startsWith(doRectangleLc)) {
				doRectangle = !doRectangle;
			} else if (argLc.startsWith(doCalendarPuzzleLc)) {
				doCalendarPuzzle = !doCalendarPuzzle;
			} else if (argLc.equals(includeDayOfWeekLc)) {
				includeDayOfWeek = !includeDayOfWeek;
			} else if (argLc.startsWith(dateStringLc)) {
				try {
					localDate = LocalDate.parse(arg.substring(dateStringLc.length()), DateTimeFormatter.ISO_LOCAL_DATE);
				} catch (final DateTimeParseException dateTimeParseException) {
				}
			} else if (argLc.startsWith(seedLc)) {
				try {
					seed = Long.parseLong(arg.substring(seedLc.length()));
					seedIsSet = true;
				} catch (final NumberFormatException e) {
				}
			} else if (argLc.startsWith(solutionNumberLc)) {
				try {
					solutionNumber = Integer.parseInt(arg.substring(solutionNumberLc.length()));
				} catch (final NumberFormatException e) {
				}
			} else if (argLc.equals(printOutPiecesLc)) {
				printOutPieces = !printOutPieces;
			} else if (argLc.startsWith(givenLc)) {
				addToIntPairSet(givensSet, arg.substring(givenLc.length()));
			} else if (argLc.startsWith(piecesLc)) {
				addToPieceList(piecesList, arg.substring(piecesLc.length()));
			}
		}

		final Piece[] pieces = (Piece[]) piecesList.toArray(new Piece[piecesList.size()]);
		final ArrayList<int[]> givens = new ArrayList<>(givensSet);
		Pentominoes pentominoPuzzle = null;
		if (demoOnly) {
			pentominoPuzzle = Pentominoes._SimpleDemoProblem;
		} else if (doRectangleWithHole) {
			pentominoPuzzle = Pentominoes._RectangleWithHole;
		} else if (doRectangle) {
			pentominoPuzzle = new Pentominoes(nRows, new ArrayList<>(), Pentominoes._FullSetOfRealPentominos);
		} else if (doCalendarPuzzle) {
			pentominoPuzzle = Pentominoes.GetCalendarPuzzle(localDate, includeDayOfWeek);
		} else {
			pentominoPuzzle = new Pentominoes(nRows, givens, pieces);
		}
		final PentominoesSolver pentominoPuzzleSolver = new PentominoesSolver(pentominoPuzzle, seedIsSet, seed,
				solutionNumber);
		pentominoPuzzleSolver.solve();
		for (int iPass = 0; iPass < 2; ++iPass) {
			PrintStream ps = null;
			if (iPass == 0) {
				ps = System.out;
			} else {
				final String dateString = localDate.toString();
				try {
					ps = new PrintStream(
							new File(String.format("%s Seed[%d].txt", dateString, pentominoPuzzleSolver._seed)));
				} catch (final FileNotFoundException var29) {
				}
			}
			if (ps != null) {
				ps.printf("%s", pentominoPuzzleSolver.getString());
				if (printOutPieces) {
					ps.printf("\n\nThese are the active pieces:");
					final Piece[] activePieces = pentominoPuzzle._pieces;
					final int nPieces = activePieces.length;
					for (int k = 0; k < nPieces; ++k) {
						ps.printf("%s%s", k > 0 ? "\n\n" : "\n", activePieces[k].getString());
					}
				}
				if (iPass == 1) {
					ps.close();
				}
			}
		}
	}
}
