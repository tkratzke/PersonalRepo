package com.skagit.puzzles.sudoku.sudokuConstraints;

import java.util.ArrayList;
import java.util.BitSet;

public class EnumerateHooks {

	final public static int _MaxValue = 9;

	public static void main(final String[] args) {
		final int[] targets = { 1, 3, 5, 9, 15, 25, 27, 45 };
		final int nTargets = targets.length;

		for (int k0 = 0; k0 < nTargets; ++k0) {
			final int target = targets[k0];
			final StringBuilder sb = new StringBuilder(k0 == 0 ? "" : "\n");
			sb.append(String.format("If UTAH is %d, then:", target));
			final int maxX = Math.min(target, _MaxValue);
			for (int x = 1; x <= maxX; ++x) {
				final ArrayList<BitSet> solutions = new ArrayList<>();
				final BitSet solution0 = new BitSet(_MaxValue + 1);
				solution0.set(x);
				final int target1 = target - x;
				addToSolutions(solutions, target1, solution0, x - 1, /* minForRest= */1);
				boolean doneIntro = false;
				for (final BitSet solution1 : solutions) {
					if (!doneIntro) {
						doneIntro = true;
						sb.append(String.format(
								"\n\tIf you start with %d, you have the following possibilities for the next %d:", x,
								x - 1));
					}
					solution1.clear(x);
					sb.append(String.format("\n\t\t%s", solution1.toString()));
					solution1.set(x);
				}
			}
			System.out.print(sb.toString());
		}

		int grandTotal = 0;
		for (int iPass = 0; iPass < 2; ++iPass) {
			int cum = 0;
			if (iPass == 1) {
				System.out.printf("\n\n\nTotal # of combinations[%d]", grandTotal);
			}
			int k = 0;
			for (int k0 = 0; k0 < nTargets; ++k0) {
				final StringBuilder sb = iPass == 0 ? null : new StringBuilder();
				final int target = targets[k0];
				final int maxX = Math.min(target, _MaxValue);
				for (int x = 1; x <= maxX; ++x) {
					final BitSet solution0 = new BitSet(_MaxValue + 1);
					solution0.set(x);
					final int target1 = target - x;
					final ArrayList<BitSet> solutions = new ArrayList<>();
					addToSolutions(solutions, target1, solution0, x - 1, /* minForRest= */1);
					for (final BitSet solution1 : solutions) {
						solution1.clear(x);
						final BitSet complement = new BitSet(_MaxValue + 1);
						for (int bit = solution1.nextClearBit(1); bit <= _MaxValue; bit = solution1
								.nextClearBit(bit + 1)) {
							if (bit != x) {
								complement.set(bit);
							}
						}
						final int thisTotal = factorial(solution1.cardinality()) * factorial(complement.cardinality());
						if (iPass == 0) {
							grandTotal += thisTotal;
						} else {
							cum += thisTotal;
							sb.append(String.format("\n%2d.\t%2d-%d  %s  %s (%d: %.4f, %.4f)", ++k, target, x,
									solution1.toString(), complement.toString(), thisTotal,
									(double) thisTotal / grandTotal, (double) cum / grandTotal));
						}
						solution1.set(x);
					}
				}
				if (iPass == 1) {
					System.out.print((k0 == 0 ? "" : "\n") + sb.toString());
				}
			}
		}
	}

	private static int factorial(final int k) {
		int returnValue = 1;
		for (int kk = 2; kk <= k; ++kk) {
			returnValue *= kk;
		}
		return returnValue;
	}

	private static void addToSolutions(final ArrayList<BitSet> solutions, final int target, final BitSet current,
			final int nLeft, final int minForRest) {
		if (nLeft == 0) {
			if (target == 0) {
				solutions.add((BitSet) current.clone());
			}
			return;
		}
		final int maxK = Math.min(_MaxValue, target / nLeft);
		for (int k = current.nextClearBit(minForRest); k <= maxK; k = current.nextClearBit(k + 1)) {
			current.set(k);
			addToSolutions(solutions, target - k, current, nLeft - 1, /* minForRest= */k + 1);
			current.clear(k);
		}
	}

}
