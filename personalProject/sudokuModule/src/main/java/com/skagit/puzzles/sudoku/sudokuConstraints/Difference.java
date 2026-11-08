package com.skagit.puzzles.sudoku.sudokuConstraints;

import java.util.BitSet;

import com.skagit.puzzles.sudoku.Outcome;
import com.skagit.puzzles.sudoku.PossiblesReduction;
import com.skagit.puzzles.sudoku.Variable;
import com.skagit.puzzles.sudoku.sudokuNode.QueueOfVbles;

public class Difference extends Constraint {
	final private int _difference;

	public Difference(final String name, final Variable vble0, final Variable vble1, final int difference) {
		super(name);
		addVble(vble0);
		addVble(vble1);
		_difference = Math.abs(difference);
	}

	@Override
	public String getString() {
		return String.format("%s & %s differ by %d.", _vbles[0].getName(), _vbles[1].getName(), _difference);
	}

	@Override
	public Outcome deduce(final Variable vble0, final QueueOfVbles queueOfVbles) {
		final Variable vble1 = _vbles[vble0 == _vbles[0] ? 1 : 0];
		final BitSet possibles1 = vble1.getPossibles();
		final int len1 = possibles1.length();
		final BitSet ok = new BitSet(len1);
		final BitSet possibles0 = vble0.getPossibles();
		boolean eliminatedSomething = false;
		for (int bit1 = possibles1.nextSetBit(0); bit1 >= 0; bit1 = possibles1.nextSetBit(bit1 + 1)) {
			final int bit0a = bit1 - _difference;
			if (bit0a >= 0 && possibles0.get(bit0a)) {
				ok.set(bit1);
			} else {
				final int bit0b = bit1 + _difference;
				if (possibles0.get(bit0b)) {
					ok.set(bit1);
				} else {
					eliminatedSomething = true;
				}
			}
		}
		if (eliminatedSomething) {
			return queueOfVbles.addToQueue(new PossiblesReduction(vble1, ok, /* andNot= */false));
		}
		return Outcome.NO_DEDUCTION;
	}

}
