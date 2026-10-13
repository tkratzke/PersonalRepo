package com.skagit.miscPuzzles.pentominoes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.skagit.utils.StringUtilities;

public class Pentominoes {
	final public static String _GivenIndicator = "**";

	final private static int _MonthDayNRows = 7;
	final private static int[][] _MonthDayAlwaysGivens = { //
			{ 0, 6 }, { 1, 6 }, //
			{ 6, 3 }, { 6, 4 }, { 6, 5 }, { 6, 6 } //
	};
	final private static Piece[] _MonthDayPieces = { //
			new Piece(0, TypeOfPiece.SKID_MARK), //
			new Piece(1, TypeOfPiece.TWO_BY_THREE), //
			new Piece(2, TypeOfPiece.OFFSET_T), //
			new Piece(3, TypeOfPiece.UTAH), //
			new Piece(4, TypeOfPiece.Z), //
			new Piece(5, TypeOfPiece.CUP), //
			new Piece(6, TypeOfPiece.ELL), //
			new Piece(7, TypeOfPiece.CORNER) //
	};

	final private static int _MonthDayOfWeekDayNRows = 8;
	final private static int[][] _MonthDayOfWeekDayAlwaysGivens = { //
			{ 0, 6 }, { 1, 6 }, //
			{ 7, 0 }, { 7, 1 }, { 7, 2 }, { 7, 3 } //
	};
	final private static Piece[] _MonthDayOfWeekDayPieces = { //
			new Piece(0, TypeOfPiece.SKID_MARK), //
			new Piece(1, TypeOfPiece.TWO_BY_THREE), //
			new Piece(2, TypeOfPiece.OFFSET_T), //
			new Piece(3, TypeOfPiece.UTAH), //
			new Piece(4, TypeOfPiece.Z), //
			new Piece(5, TypeOfPiece.CUP), //
			new Piece(6, TypeOfPiece.ELL), //
			new Piece(7, TypeOfPiece.CORNER) //
	};

	final static Piece[] _FullSetOfRealPentominos = { new Piece(0, TypeOfPiece.I), //
			new Piece(0, TypeOfPiece.ELL), //
			new Piece(0, TypeOfPiece.CORNER), //
			new Piece(0, TypeOfPiece.T), //
			new Piece(0, TypeOfPiece.W), //
			new Piece(0, TypeOfPiece.CROSS), //
			new Piece(0, TypeOfPiece.FUNKY), //
			new Piece(0, TypeOfPiece.UTAH), //
			new Piece(0, TypeOfPiece.Z), //
			new Piece(0, TypeOfPiece.CUP), //
			new Piece(0, TypeOfPiece.SKID_MARK), //
			new Piece(0, TypeOfPiece.OFFSET_T) //
	};

	/** The last two are not templates, but rather complete problems. */
	static Pentominoes _RectangleWithHole = new Pentominoes( //
			/* nRows= */8, //
			/* givens= */ Arrays.asList(new int[][] { //
					{ 3, 3 }, { 3, 4 }, //
					{ 4, 3 }, { 4, 4 } //
			}), //
			_FullSetOfRealPentominos //
	);

	static Pentominoes _SimpleDemoProblem = new Pentominoes( //
			/* nRows= */4, //
			/* givens= */ Arrays.asList(new int[][] { //
					{ 3, 1 }, { 3, 2 } //
			}), //
			/* pieces= */new Piece[] { //
					new Piece(0, TypeOfPiece.OFFSET_T), //
					new Piece(1, TypeOfPiece.UTAH) //
			} //
	);
	static final Pentominoes RectangularPuzzleWithHole = new Pentominoes(8, //
			/* givens= */Arrays.asList(new int[][] { { 3, 3 }, { 3, 4 }, { 4, 3 }, { 4, 4 } }), //
			_FullSetOfRealPentominos //
	);

	static Pentominoes GetCalendarPuzzle(final LocalDate localDate, final boolean includeDayOfWeek) {
		final int dayOfMonthOrdinal = localDate.getDayOfMonth() - 1;
		final int monthOrdinal = localDate.getMonth().getValue() - 1;
		if (includeDayOfWeek) {
			final ArrayList<int[]> givens = new ArrayList<>(Arrays.asList(_MonthDayOfWeekDayAlwaysGivens));
			final int dayOfWeekOrdinal = localDate.getDayOfWeek().getValue();
			final int[] dayOfWeekPair;
			if (dayOfWeekOrdinal == 7) {
				dayOfWeekPair = new int[] { 6, 3 }; // Sunday.
			} else if (dayOfWeekOrdinal < 3) {
				dayOfWeekPair = new int[] { 6, 3 + dayOfWeekOrdinal }; // Mon, Tue, Wed
			} else {
				dayOfWeekPair = new int[] { 7, dayOfWeekOrdinal }; // Thu, Fri, Sat
			}
			givens.addAll(Arrays.asList(new int[][] { //
					{ monthOrdinal / 6, monthOrdinal % 6 }, //
					{ 2 + dayOfMonthOrdinal / 7, dayOfMonthOrdinal % 7 }, //
					dayOfWeekPair }));
			return new Pentominoes(_MonthDayOfWeekDayNRows, givens, _MonthDayOfWeekDayPieces);
		}
		final ArrayList<int[]> givens = new ArrayList<>(Arrays.asList(_MonthDayAlwaysGivens));
		givens.addAll(Arrays.asList(new int[][] { //
				{ monthOrdinal / 6, monthOrdinal % 6 }, //
				{ 2 + dayOfMonthOrdinal / 7, dayOfMonthOrdinal % 7 } //
		}));
		return new Pentominoes(_MonthDayNRows, givens, _MonthDayPieces);
	}

	final int _nRows, _nClmns;
	final int[][] _givens;
	final Piece[] _pieces;

	public Pentominoes(final int nRows, final List<int[]> givens, final Piece[] pieces) {
		_nRows = nRows;
		_givens = givens.toArray(new int[givens.size()][]);
		int nSquares = _givens.length;
		_pieces = pieces;
		final int nPieces = _pieces.length;
		for (int k = 0; k < nPieces; ++k) {
			nSquares += _pieces[k]._typeOfPiece._nSquares;
		}
		if (nSquares % _nRows != 0) {
			System.exit(33);
		}
		_nClmns = nSquares / _nRows;
	}

	public String getString(final boolean includePieces) {
		final String[][] coveredBys = getInitialCoveredBys();
		final StringBuilder s = new StringBuilder()
				.append(StringUtilities.getString(coveredBys, TypeOfPiece._IndicatorLength));
		if (includePieces) {
			s.append("\nPieces:");
			for (final Piece piece : _pieces) {
				s.append("\n").append(piece.getString());
			}
		}
		return s.toString();
	}

	String[][] getInitialCoveredBys() {
		final String[][] coveredBys = new String[_nRows][_nClmns];
		fillInInitialCoveredBys(coveredBys);
		return coveredBys;
	}

	void fillInInitialCoveredBys(final String[][] coveredBys) {
		for (int row = 0; row < _nRows; ++row) {
			coveredBys[row] = new String[_nClmns];
			Arrays.fill(coveredBys[row], TypeOfPiece._NotCovered);
		}
		for (final int[] ij : _givens) {
			final int row = ij[0], clmn = ij[1];
			coveredBys[row][clmn] = _GivenIndicator;
		}
	}

	@Override
	public String toString() {
		return getString(/* includePieces= */false);
	}

}
