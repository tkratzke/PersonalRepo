package com.skagit.puzzles.sudoku;

import java.util.BitSet;

import com.skagit.puzzles.sudoku.sudokuConstraints.Constraint;
import com.skagit.puzzles.sudoku.sudokuNode.QueueOfVbles;

public class Variable extends NamedEntity {
	private final String _name;
	private final int[] _rc;
	final BitSet _domain;
	private final BitSet _possibles;
	private Constraint[] _myConstraints = new Constraint[0];

	/** For looking up by name: */
	Variable(final String name) {
		this(name, null, null);
	}

	Variable(final String name, final int[] rc, final BitSet domain) {
		_name = name;
		_rc = rc;
		_domain = domain;
		_possibles = (_domain == null ? null : (BitSet) _domain.clone());
	}

	public void addConstraint(final Constraint constraint) {
		_myConstraints = SudokuStatics.augmentArray(_myConstraints, constraint);
	}

	public Outcome propagate(final QueueOfVbles queueOfVbles) {
		Outcome returnValue = Outcome.NO_DEDUCTION;
		for (final Constraint constraint : _myConstraints) {
			final Outcome outcome = constraint.deduce(this, queueOfVbles);
			if (outcome == Outcome.CONTRADICTION) {
				return Outcome.CONTRADICTION;
			} else if (outcome == Outcome.MADE_DEDUCTION) {
				returnValue = Outcome.MADE_DEDUCTION;
			}
		}
		return returnValue;
	}

	@Override
	public String getName() {
		return _name;
	}

	public String getString(final boolean dumpAllDifferents) {
		String s = "";
		if (!dumpAllDifferents) {
			s += "Variable ";
		}
		s += getName();
		if (getRc() != null) {
			s += String.format(" at %d,%d", getRc()[0], getRc()[1]);
		}
		s += ". Possibles: " + _possibles.toString();
		if (!_domain.equals(_possibles)) {
			s += ", Domain: " + _domain.toString();
		}
		if (dumpAllDifferents) {
			s += ". AllDifferents:";
			for (final Constraint constraint : getMyConstraints()) {
				s += " " + constraint.getName();
			}
		}
		s += ".";
		return s;
	}

	@Override
	public String getString() {
		return getString(/* dumpAllDifferents= */true);
	}

	void reset() {
		_possibles.clear();
		_possibles.or(_domain);
	}

	public BitSet getPossibles() {
		return _possibles;
	}

	public Constraint[] getMyConstraints() {
		return _myConstraints;
	}

	public int[] getRc() {
		return _rc;
	}

}
