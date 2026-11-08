package com.skagit.puzzles.sudoku.sudokuConstraints;

import java.util.Arrays;
import java.util.BitSet;

import com.skagit.puzzles.sudoku.Outcome;
import com.skagit.puzzles.sudoku.PossiblesReduction;
import com.skagit.puzzles.sudoku.Variable;
import com.skagit.puzzles.sudoku.sudokuNode.QueueOfVbles;
import com.skagit.utils.PermutationTools;

public class AllDifferent extends Constraint {

	public AllDifferent(final String name, final Variable[] allDifferentVbles) {
		super(name);
		for (final Variable vble : allDifferentVbles) {
			addVble(vble);
		}
	}

	@Override
	public Outcome deduce(final Variable vble, final QueueOfVbles queueOfVbles) {
		final int nVbles = _vbles.length;
		final BitSet coreBitSet = new BitSet(nVbles);
		final int seedIdx = Arrays.binarySearch(_vbles, vble);
		coreBitSet.set(seedIdx);
		Outcome outcome = Outcome.NO_DEDUCTION;
		NEXT_EXTENSION: for (BitSet extension = null;;) {
			if (extension == null) {
				extension = new BitSet(nVbles);
				extension.set(seedIdx);
			} else if (!PermutationTools.nextExtension(coreBitSet, extension, nVbles)) {
				break;
			}
			final int extensionCardinality = extension.cardinality();
			final BitSet unionOfPossibles = new BitSet();
			int unionOfPossiblesCardinality = 0;
			for (int k1 = extension.nextSetBit(0); k1 >= 0; k1 = extension.nextSetBit(k1 + 1)) {
				final Variable sibling = _vbles[k1];
				unionOfPossibles.or(sibling.getPossibles());
				unionOfPossiblesCardinality = unionOfPossibles.cardinality();
				if (unionOfPossibles.cardinality() > extensionCardinality) {
					continue NEXT_EXTENSION;
				}
			}
			if (unionOfPossiblesCardinality < extensionCardinality) {
				return Outcome.CONTRADICTION;
			}
			/**
			 * To get here, extensionCardinality must equal unionOfPossiblesCardinality. We
			 * can eliminate unionOfPossibles from each vble in the complement of extension.
			 * Does it do any good?
			 */
			for (int k1 = extension.nextClearBit(0); k1 < nVbles; k1 = extension.nextClearBit(k1 + 1)) {
				final Variable sibling = _vbles[k1];
				final Outcome thisOutcome = queueOfVbles
						.addToQueue(new PossiblesReduction(sibling, unionOfPossibles, /* andNot= */true));
				switch (thisOutcome) {
				case CONTRADICTION:
					return Outcome.CONTRADICTION;
				case MADE_DEDUCTION:
					outcome = Outcome.MADE_DEDUCTION;
					break;
				case NO_DEDUCTION:
				default:
					break;
				}
			}
		}
		return outcome;
	}

	@Override
	public String getString() {
		final StringBuilder s = new StringBuilder().append(getName()).append(".  Vbles:");
		for (final Variable vble : _vbles) {
			s.append(" ").append(vble.getName());
		}
		s.append(".");
		return s.toString();
	}
}
