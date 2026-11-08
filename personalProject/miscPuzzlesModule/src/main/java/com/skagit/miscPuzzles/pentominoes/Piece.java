package com.skagit.miscPuzzles.pentominoes;

public class Piece implements Comparable<Piece> {

	private final int _idx;
	public final TypeOfPiece _typeOfPiece;

	public Piece(final int idx, final TypeOfPiece typeOfPiece) {
		_idx = idx;
		_typeOfPiece = typeOfPiece;
	}

	@Override
	public int compareTo(final Piece piece) {
		final int idx0 = _idx;
		final int idx1 = piece._idx;
		return idx0 < idx1 ? -1 : (idx0 > idx1 ? 1 : 0);
	}

	public String getString() {
		return String.format("%c(%s:%d): ", //
				_typeOfPiece._indicator, _typeOfPiece._descriptor, _typeOfPiece._nSquares);
	}

	@Override
	public String toString() {
		return getString();
	}
}
