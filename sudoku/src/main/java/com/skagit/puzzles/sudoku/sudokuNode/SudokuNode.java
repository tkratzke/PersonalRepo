package com.skagit.puzzles.sudoku.sudokuNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;

import com.skagit.puzzles.sudoku.Outcome;
import com.skagit.puzzles.sudoku.PossiblesReduction;
import com.skagit.puzzles.sudoku.Sudoku;
import com.skagit.puzzles.sudoku.SudokuStatics;
import com.skagit.puzzles.sudoku.Variable;

public class SudokuNode {

	public final Sudoku _sudoku;
	public final String _comment;
	public final int _level;
	public final SudokuState _incomingState;
	public final SudokuState _afterPropagationState;
	public SudokuNode _child0, _child1;

	public SudokuNode(final Sudoku sudoku, final String comment, final int level,
			final PossiblesReduction[] possiblesReductions) {
		_sudoku = sudoku;
		_comment = comment;
		_level = level;
		_incomingState = new SudokuState(sudoku);

		final QueueOfVbles queueOfVbles = new QueueOfVbles();

		final int nPossiblesReductions = possiblesReductions.length;
		for (int k = 0; k < nPossiblesReductions; ++k) {
			queueOfVbles.addToQueue(possiblesReductions[k]);
		}
		while (!queueOfVbles.isEmpty()) {
			final Variable variable = queueOfVbles.removeFirst();
			final Outcome outcome = variable.propagate(queueOfVbles);
			if (outcome == Outcome.CONTRADICTION) {
				_afterPropagationState = new SudokuState(sudoku);
				_child0 = _child1 = null;
				return;
			}
		}
		_afterPropagationState = new SudokuState(sudoku);

		/** Pick a vble to branch on. */
		final Variable[] vbles = _sudoku.getVbles();
		Variable vbleToBranchOn = null;
		int fewest = -1;
		final int nVbles = vbles.length;
		for (int k = 0; k < nVbles; ++k) {
			final Variable vble = vbles[k];
			final int thisCardinality = vble.getPossibles().cardinality();
			if (thisCardinality > 1 && (vbleToBranchOn == null || thisCardinality < fewest)) {
				vbleToBranchOn = vble;
				fewest = thisCardinality;
			}
		}

		if (vbleToBranchOn == null) {
			/** _afterPropagationState is a solution. */
			_child0 = _child1 = null;
			return;
		}

		final BitSet vblePossibles = vbleToBranchOn.getPossibles();
		final BitSet possibles0 = new BitSet(vblePossibles.length());
		final BitSet possibles1 = new BitSet(vblePossibles.length());
		for (int k = 0, bit = vblePossibles.nextSetBit(0); bit >= 0; bit = vblePossibles.nextSetBit(bit + 1)) {
			if (k++ % 2 == 0) {
				possibles0.set(bit);
			} else {
				possibles1.set(bit);
			}
		}
		_child0 = null;
		_child1 = null;
		for (int iPass = 0; iPass < 2; ++iPass) {
			final BitSet possibles = iPass == 0 ? possibles0 : possibles1;
			final String childComment = String.format("%s set to %s", vbleToBranchOn.getName(), possibles.toString());
			final PossiblesReduction possiblesReduction = new PossiblesReduction(vbleToBranchOn,
					iPass == 0 ? possibles0 : possibles1, /* andNot= */false);
			final PossiblesReduction[] possiblesReductions2 = new PossiblesReduction[] { possiblesReduction };
			final SudokuNode child = new SudokuNode(_sudoku, childComment, _level + 1, possiblesReductions2);
			if (iPass == 0) {
				_child0 = child;
				_afterPropagationState.restoreState(sudoku);
			} else {
				_child1 = child;
				_incomingState.restoreState(sudoku);
			}
		}
	}

	public void appendStringBuffer(final StringBuffer sb) {
		final char[] tabsArray = new char[_level];
		Arrays.fill(tabsArray, '\t');
		final String prefix = new String(tabsArray);
		final SudokuState currentState = new SudokuState(_sudoku);
		_afterPropagationState.restoreState(_sudoku);
		final String comment = _comment == null ? "Root. Given Reductions yielded " : (_comment + ", which yielded ");
		sb.append(String.format("%s%s", prefix, comment));
		sb.append(String.format("nKnown = %d", _afterPropagationState.getNKnown()));
		final boolean isSolved = _afterPropagationState.isSolved();
		final boolean isContradiction = _afterPropagationState.isContradiction();
		if (isSolved || isContradiction) {
			sb.append(isSolved ? ", a SOLUTION!" : ", but a CONTRADICTION.");
			final String[] linesOfPrintout = SudokuStatics.getLinesOfPrintout(_sudoku.getVbles());
			for (final String lineOfPrintout : linesOfPrintout) {
				sb.append(String.format("\n%s\t%s", prefix, lineOfPrintout));
			}
		}
		currentState.restoreState(_sudoku);
		sb.append("\n");
		if (_child0 != null) {
			_child0.appendStringBuffer(sb);
			_child1.appendStringBuffer(sb);
		}
	}

	public ArrayList<SudokuState> addToSolutions(ArrayList<SudokuState> solutions) {
		if (solutions == null) {
			solutions = new ArrayList<>();
		}
		final boolean isSolved = _afterPropagationState.isSolved();
		final boolean isContradiction = _afterPropagationState.isContradiction();
		if (isSolved || isContradiction) {
			if (isSolved) {
				solutions.add(_afterPropagationState);
			}
			return solutions;
		}
		_child0.addToSolutions(solutions);
		_child1.addToSolutions(solutions);
		return solutions;
	}
}
