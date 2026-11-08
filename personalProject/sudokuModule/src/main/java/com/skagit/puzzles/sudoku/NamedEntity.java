package com.skagit.puzzles.sudoku;

public abstract class NamedEntity implements Comparable<NamedEntity> {
	abstract public String getName();

	abstract public String getString();

	@Override
	public String toString() {
		return getString();
	}

	@Override
	public int compareTo(final NamedEntity namedEntity) {
		return getName().compareTo(namedEntity.getName());
	}
}
