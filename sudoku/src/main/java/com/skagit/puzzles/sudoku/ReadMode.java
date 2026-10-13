package com.skagit.puzzles.sudoku;

public enum ReadMode {
	VARIABLES("Variables"), //
	POSSIBLES_REDUCTIONS("Possibles Reductions"), //
	ALL_DIFFERENTS("All Different Constraints"), //
	RED_GREEN("Red Green Constraints"), //
	KNIGHT_MOVE("Knight Move Constraints"), //
	QUOTIENT("Quotient Constraints"), DIFFERENCE("Difference Constraints"), //
	XARRAY("XArray Constraints");

	final String _name;

	ReadMode(final String name) {
		_name = name;
	}
}
