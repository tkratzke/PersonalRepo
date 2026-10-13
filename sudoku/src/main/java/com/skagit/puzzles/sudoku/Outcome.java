package com.skagit.puzzles.sudoku;

public enum Outcome {
	CONTRADICTION("Contradiction"), MADE_DEDUCTION("Made Deduction"), NO_DEDUCTION("No Deduction");

	final String _name;

	Outcome(final String name) {
		_name = name;
	}
}
