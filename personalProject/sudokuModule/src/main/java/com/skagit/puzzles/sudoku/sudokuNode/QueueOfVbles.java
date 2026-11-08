package com.skagit.puzzles.sudoku.sudokuNode;

import java.util.BitSet;
import java.util.Comparator;
import java.util.TreeSet;

import com.skagit.puzzles.sudoku.Outcome;
import com.skagit.puzzles.sudoku.PossiblesReduction;
import com.skagit.puzzles.sudoku.Variable;

public class QueueOfVbles {
	final TreeSet<Variable> _queueOfVbles;

	public QueueOfVbles() {
		_queueOfVbles = new TreeSet<>(new Comparator<Variable>() {

			@Override
			public int compare(final Variable vble0, final Variable vble1) {
				final int nPossibles0 = vble0.getPossibles().cardinality();
				final int nPossibles1 = vble1.getPossibles().cardinality();
				if (nPossibles0 != nPossibles1) {
					return nPossibles0 < nPossibles1 ? -1 : 1;
				}
				return vble0.compareTo(vble1);
			}
		});
	}

	public Outcome addToQueue(final PossiblesReduction possiblesReduction) {
		final Variable vble = possiblesReduction._vble;
		final BitSet vblePossibles = vble.getPossibles();
		final int nOldPossibles = vblePossibles.cardinality();
		final BitSet possibles = possiblesReduction._possibles;
		final boolean andNot = possiblesReduction._andNot;
		if (!_queueOfVbles.contains(vble)) {
			if (andNot) {
				vblePossibles.andNot(possibles);
			} else {
				vblePossibles.and(possibles);
			}
			final int nPossibles = vblePossibles.cardinality();
			if (nPossibles < nOldPossibles) {
				_queueOfVbles.add(vble);
				if (nPossibles == 0) {
					return Outcome.CONTRADICTION;
				}
				return Outcome.MADE_DEDUCTION;
			}
			return Outcome.NO_DEDUCTION;
		}

		boolean removed = false;
		for (int bit = vblePossibles.nextSetBit(0); bit >= 0; bit = vblePossibles.nextSetBit(bit + 1)) {
			if ((andNot && possibles.get(bit)) || (!andNot && !possibles.get(bit))) {
				if (!removed) {
					_queueOfVbles.remove(vble);
					removed = true;
				}
				vblePossibles.clear(bit);
			}
		}
		if (!removed) {
			return Outcome.NO_DEDUCTION;
		}
		_queueOfVbles.add(vble);
		return vblePossibles.isEmpty() ? Outcome.CONTRADICTION : Outcome.MADE_DEDUCTION;
	}

	public boolean isEmpty() {
		return _queueOfVbles.isEmpty();
	}

	public Variable removeFirst() {
		if (_queueOfVbles.isEmpty()) {
			return null;
		}
		final Variable first = _queueOfVbles.first();
		_queueOfVbles.remove(first);
		return first;
	}

}
