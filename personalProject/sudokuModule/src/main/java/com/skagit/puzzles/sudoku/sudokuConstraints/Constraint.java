package com.skagit.puzzles.sudoku.sudokuConstraints;

import com.skagit.puzzles.sudoku.NamedEntity;
import com.skagit.puzzles.sudoku.Outcome;
import com.skagit.puzzles.sudoku.SudokuStatics;
import com.skagit.puzzles.sudoku.Variable;
import com.skagit.puzzles.sudoku.sudokuNode.QueueOfVbles;

public class Constraint extends NamedEntity {
	final String _name;
	protected Variable[] _vbles;

	public Constraint(final String name) {
		_name = name;
		_vbles = new Variable[0];
	}

	@Override
	public String getName() {
		return _name;
	}

	public void addVble(final Variable vble) {
		_vbles = SudokuStatics.augmentArray(_vbles, vble);
		vble.addConstraint(this);
	}

	public Outcome deduce(final Variable variable, final QueueOfVbles queueOfVbles) {
		return null;
	}

	@Override
	public String getString() {
		return null;
	}

}
