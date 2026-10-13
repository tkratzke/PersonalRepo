package com.skagit.puzzles.sudoku.sudokuConstraints;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

import com.skagit.puzzles.sudoku.Sudoku;
import com.skagit.puzzles.sudoku.SudokuStatics;
import com.skagit.puzzles.sudoku.Variable;

public class GenerateKnightConstraints {

	final public static int _MaxValue = 9;

	public static ArrayList<AllDifferent> GenerateKnightMoveConstraints(final PrintStream ps, final Sudoku sudoku,
			final int[] forbiddenIndices) {
		final Variable[] vbles = sudoku.getVbles();
		final int nVble = vbles.length;
		final ArrayList<AllDifferent> allDifferentsList = new ArrayList<>();
		final StringBuffer sb = new StringBuffer();
		int count = 0;
		for (int k0 = 0; k0 < nVble; ++k0) {
			final Variable vble0 = vbles[k0];
			final int[] rc0 = vble0.getRc();
			final int r0 = rc0[0], c0 = rc0[1];
			final String name0 = vble0.getName();
			if (Arrays.binarySearch(forbiddenIndices, r0) >= 0 || Arrays.binarySearch(forbiddenIndices, c0) >= 0) {
				continue;
			}
			for (int k1 = k0 + 1; k1 < nVble; ++k1) {
				final Variable vble1 = vbles[k1];
				final int[] rc1 = vble1.getRc();
				final int r1 = rc1[0], c1 = rc1[1];
				if (Arrays.binarySearch(forbiddenIndices, r1) >= 0 || Arrays.binarySearch(forbiddenIndices, c1) >= 0) {
					continue;
				}
				final int rDiff = Math.abs(r0 - r1);
				if (rDiff == 0 || rDiff > 2) {
					continue;
				}
				final int cDiff = Math.abs(c0 - c1);
				if (cDiff == 0 || cDiff > 2) {
					continue;
				}
				if (rDiff * cDiff == 2) {
					final String name1 = vble1.getName();
					++count;
					sb.append(String.format("\n%s\t%s %s", name0 + name1, name0, name1));
				}
			}
		}
		sb.append(String.format("\n\nThere are %d Knight Moves", count));
		SudokuStatics.dumpString(ps, sb.toString());
		return allDifferentsList;
	}

}
