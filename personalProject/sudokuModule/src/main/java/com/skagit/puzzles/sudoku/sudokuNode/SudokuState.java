package com.skagit.puzzles.sudoku.sudokuNode;

import java.util.BitSet;

import com.skagit.puzzles.sudoku.Sudoku;
import com.skagit.puzzles.sudoku.Variable;

public class SudokuState {
	final private BitSet[] _vblePossiblesS;

	final private int _nKnown;
	final private boolean _isSolved, _isContradiction;

	public SudokuState(final Sudoku sudoku) {
		final Variable[] vbles = sudoku.getVbles();
		final int nVbles = vbles.length;
		int nKnown = 0;
		boolean contradiction = false;
		_vblePossiblesS = new BitSet[nVbles];
		for (int k = 0; k < nVbles; ++k) {
			final BitSet vblePossibles = vbles[k].getPossibles();
			final BitSet vbleState = _vblePossiblesS[k] = new BitSet(vblePossibles.length());
			int nPossibles = 0;
			for (int bit = vblePossibles.nextSetBit(0); bit >= 0; bit = vblePossibles.nextSetBit(bit + 1)) {
				vbleState.set(bit);
				++nPossibles;
			}
			switch (nPossibles) {
			case 0:
				contradiction = true;
				break;
			case 1:
				++nKnown;
				break;
			default:
			}
		}
		_nKnown = nKnown;
		_isSolved = getNKnown() == nVbles;
		_isContradiction = contradiction;
	}

	public void restoreState(final Sudoku sudoku) {
		final Variable[] vbles = sudoku.getVbles();
		final int nVbles = vbles.length;
		for (int k = 0; k < nVbles; ++k) {
			final BitSet vbleState = _vblePossiblesS[k];
			final BitSet vblePossibles = vbles[k].getPossibles();
			vblePossibles.clear();
			for (int bit = vbleState.nextSetBit(0); bit >= 0; bit = vbleState.nextSetBit(bit + 1)) {
				vblePossibles.set(bit);
			}
		}
	}

	public int getNVbles() {
		return _vblePossiblesS.length;
	}

	public int getNKnown() {
		return _nKnown;
	}

	public boolean isSolved() {
		return _isSolved;
	}

	public boolean isContradiction() {
		return _isContradiction;
	}

}
