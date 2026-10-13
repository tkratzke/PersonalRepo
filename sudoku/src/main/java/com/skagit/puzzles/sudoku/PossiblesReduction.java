package com.skagit.puzzles.sudoku;

import java.util.BitSet;

public class PossiblesReduction extends NamedEntity {
	public final Variable _vble;
	public final BitSet _possibles;
	public final boolean _andNot;

	public PossiblesReduction(final Variable variable, final BitSet possibles, final boolean andNot) {
		_vble = variable;
		_possibles = possibles;
		_andNot = andNot;
	}

	@Override
	public String getString() {
		return getName() + ": " + (_andNot ? "AndNot " : " ") + _possibles.toString();
	}

	@Override
	public String getName() {
		return _vble.getName();
	}

}
