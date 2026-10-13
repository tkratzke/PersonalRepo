package com.skagit.puzzles.sudoku;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.TreeSet;

import org.apache.commons.io.FilenameUtils;

import com.skagit.puzzles.sudoku.sudokuConstraints.AllDifferent;
import com.skagit.puzzles.sudoku.sudokuConstraints.Constraint;
import com.skagit.puzzles.sudoku.sudokuConstraints.GenerateKnightConstraints;
import com.skagit.puzzles.sudoku.sudokuNode.SudokuNode;
import com.skagit.puzzles.sudoku.sudokuNode.SudokuState;

public class Sudoku {

	private final Variable[] _variables;
	final Constraint[] _constraints;
	final PossiblesReduction[] _possiblesReductions;

	Sudoku(final Sudoku sudoku, final PossiblesReduction[] possiblesReductions) {
		_variables = sudoku.getVbles();
		_constraints = sudoku._constraints;
		final int nPossiblesReductions = possiblesReductions.length;
		_possiblesReductions = new PossiblesReduction[nPossiblesReductions];
		for (int k = 0; k < nPossiblesReductions; ++k) {
			final PossiblesReduction possiblesReduction = possiblesReductions[k];
			_possiblesReductions[k] = new PossiblesReduction(possiblesReduction._vble, possiblesReduction._possibles,
					possiblesReduction._andNot);
		}
	}

	Sudoku(final File f) {
		final TreeSet<PossiblesReduction> possiblesReductionsSet = new TreeSet<>();
		final ArrayList<Constraint> constraintsList = new ArrayList<>();
		final ArrayList<Variable> listOfVbles = new ArrayList<>();
		try (final BufferedReader bufferedReader = new BufferedReader(new FileReader(f))) {
			ReadMode readMode = null;
			for (;;) {
				String line;
				try {
					line = bufferedReader.readLine();
				} catch (final IOException e) {
					break;
				}
				if (line == null) {
					break;
				}
				final String[] tabFields = line.split("\\t+");
				if (line.length() == 0 || line.charAt(0) == '#' || tabFields.length == 0) {
					continue;
				}
				/** Do we change ReadModes? */
				final String lineLc = line.toLowerCase();
				final ReadMode oldReadMode = readMode;
				for (final ReadMode readModeOnLine : ReadMode.values()) {
					if (lineLc.startsWith(readModeOnLine._name.toLowerCase())) {
						readMode = readModeOnLine;
						break;
					}
				}
				if (readMode == null) {
					continue;
				}
				/**
				 * If we changed ReadModes then, if this is the first ReadMode, it must be
				 * VARIABLES. Also, a change in ReadModes indicates that we are done with the
				 * input line.
				 */
				if (oldReadMode != readMode) {
					if (oldReadMode == null && readMode != ReadMode.VARIABLES) {
						System.exit(30);
					}
					continue;
				}
				if (readMode == ReadMode.VARIABLES) {
					final String vbleName = tabFields[0];
					final String rcField = tabFields[1];
					final String domainField = tabFields[2];
					final int vbleIdx = Collections.binarySearch(listOfVbles,
							new Variable(vbleName, /* rc= */null, /* domain= */null));
					if (vbleIdx >= 0) {
						System.exit(50);
					}
					final String[] rcFields = rcField.split(" ");
					final int r = Integer.parseInt(rcFields[0]);
					final int c = Integer.parseInt(rcFields[1]);
					final int[] rc = { r, c };
					final String[] domainFields = domainField.split(" ");
					final int nDomainFields = domainFields.length;
					final BitSet domain = new BitSet();
					for (int k = 0; k < nDomainFields; ++k) {
						final int value = Integer.parseInt(domainFields[k]);
						if (value < 1) {
							continue;
						}
						domain.set(value);
					}
					final Variable variable = new Variable(vbleName, rc, domain);
					listOfVbles.add(variable);
					Collections.sort(listOfVbles);
				} else if (readMode == ReadMode.POSSIBLES_REDUCTIONS) {
					final String vbleName = tabFields[0];
					final String thesePossiblesField = tabFields[1];
					final Variable vbleForLookUp = new Variable(vbleName, /* rc= */null, /* domain= */null);
					final int vbleIdx = Collections.binarySearch(listOfVbles, vbleForLookUp);
					final Variable variable = listOfVbles.get(vbleIdx);
					final BitSet thesePossibles = new BitSet(variable._domain.length());
					final String[] thesePossibleFields = thesePossiblesField.split(" ");
					final int nThesePossibleFields = thesePossibleFields.length;
					for (int k = 0; k < nThesePossibleFields; ++k) {
						final int possible = Integer.parseInt(thesePossibleFields[k]);
						thesePossibles.set(possible);
					}
					final PossiblesReduction possiblesReduction = new PossiblesReduction(variable, thesePossibles,
							/* andNot= */false);
					if (!possiblesReductionsSet.add(possiblesReduction)) {
						final PossiblesReduction incumbent = possiblesReductionsSet.ceiling(possiblesReduction);
						incumbent._possibles.and(thesePossibles);
					}
				} else if (readMode == ReadMode.ALL_DIFFERENTS) {
					final String constraintName = tabFields[0];
					final Constraint vacuousConstraint = new Constraint(constraintName);
					if (Collections.binarySearch(constraintsList, vacuousConstraint) >= 0) {
						System.exit(60);
					}
					final String vblesField = tabFields[1];
					final String[] vbleFields = vblesField.split(" ");
					final int nVbleFields = vbleFields.length;
					final Variable[] allDifferentVbles = new Variable[nVbleFields];
					for (int k = 0; k < nVbleFields; ++k) {
						final String vbleName = vbleFields[k];
						final Variable vbleForLookUp = new Variable(vbleName, /* rc= */null, /* domain= */null);
						final int vbleIdx = Collections.binarySearch(listOfVbles, vbleForLookUp);
						allDifferentVbles[k] = listOfVbles.get(vbleIdx);
					}
					final AllDifferent allDifferentConstraint = new AllDifferent(constraintName, allDifferentVbles);
					constraintsList.add(allDifferentConstraint);
					Collections.sort(constraintsList);
				} else if (readMode == ReadMode.RED_GREEN) {
				} else if (readMode == ReadMode.KNIGHT_MOVE) {
				} else if (readMode == ReadMode.QUOTIENT) {
				} else if (readMode == ReadMode.DIFFERENCE) {
				}
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
		_variables = listOfVbles.toArray(new Variable[listOfVbles.size()]);
		_possiblesReductions = possiblesReductionsSet.toArray(new PossiblesReduction[possiblesReductionsSet.size()]);
		_constraints = constraintsList.toArray(new Constraint[constraintsList.size()]);
	}

	public String getString() {
		final String[] linesOfPrintout = SudokuStatics.getLinesOfPrintout(getVbles());
		final StringBuilder s = new StringBuilder();
		final int nLines = linesOfPrintout.length;
		for (int k = 0; k < nLines; ++k) {
			s.append(k == 0 ? "" : "\n").append(linesOfPrintout[k]);
		}
		return s.toString();
	}

	@Override
	public String toString() {
		return getString();
	}

	public final void reset() {
		for (final Variable vble : getVbles()) {
			vble.reset();
		}
	}

	public final SudokuNode solve() {
		final SudokuNode sudokuNode = new SudokuNode(/* sudoku= */this, /* comment= */"Root", /* level= */0,
				_possiblesReductions);
		reset();
		return sudokuNode;
	}

	public int getNVbles() {
		return _variables.length;
	}

	public Variable[] getVbles() {
		return _variables;
	}

	public static void mainAim(final String[] args) {
		/** Set the input and output files. */
		final File fIn = new File("./SudokuData/Sudoku-AIM.txt");
		final String absoluteInPath = fIn.getAbsolutePath();
		final String extension = FilenameUtils.getExtension(absoluteInPath);
		final String coreString = absoluteInPath.substring(0, absoluteInPath.length() - 1 - extension.length());
		final File fOut = new File(coreString + "OUT." + extension);

		/** Read in the problem and the possibles reductions. */
		final Sudoku sudoku = new Sudoku(fIn);

		try (PrintStream ps = new PrintStream(fOut)) {
			GenerateKnightConstraints.GenerateKnightMoveConstraints(ps, sudoku,
					/* forbiddenIndices= */new int[] { 0, 10 });
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static void mainBrian(final String[] args) {
		/** Set the input and output files. */
		final File fIn = new File("./SudokuData/Sudoku-Brian.txt");
		final String absoluteInPath = fIn.getAbsolutePath();
		final String extension = FilenameUtils.getExtension(absoluteInPath);
		final String coreString = absoluteInPath.substring(0, absoluteInPath.length() - 1 - extension.length());
		final File fOut = new File(coreString + "OUT." + extension);

		/** Read in the problem and the possibles reductions. */
		final Sudoku sudoku = new Sudoku(fIn);
		final PossiblesReduction[] possiblesReductions = sudoku._possiblesReductions;
		final int nPossiblesReductions = possiblesReductions.length;

		try (PrintStream ps = new PrintStream(fOut)) {
			int nNecessary = 0, nUnnecessary = 0;
			for (int k0 = 0; k0 < nPossiblesReductions; ++k0) {
				final StringBuffer sb = new StringBuffer();
				final PossiblesReduction[] thesePossiblesReductions = new PossiblesReduction[nPossiblesReductions - 1];
				for (int k1 = 0, k2 = 0; k1 < nPossiblesReductions; ++k1) {
					final PossiblesReduction possiblesReduction = possiblesReductions[k1];
					if (k1 != k0) {
						thesePossiblesReductions[k2++] = new PossiblesReduction(possiblesReduction._vble,
								possiblesReduction._possibles, /* andNot= */false);
					}
				}
				sb.append(k0 == 0 ? "" : "\n");
				final PossiblesReduction skippedPossiblesReduction = possiblesReductions[k0];
				final Variable variable = skippedPossiblesReduction._vble;
				final BitSet possibles = skippedPossiblesReduction._possibles;
				sb.append(String.format("%2d.  \"Variable %s must be in %s;\"", k0 + 1, variable.getName(),
						possibles.toString()));
				final Sudoku sudokuSmall = new Sudoku(sudoku, thesePossiblesReductions);
				final SudokuNode rootNodeSmall = sudokuSmall.solve();
				final ArrayList<SudokuState> solutionsSmall = rootNodeSmall.addToSolutions(/* solutions= */null);
				final int nSolutionsSmall = solutionsSmall.size();
				final boolean unNecessaryPossiblesReduction = nSolutionsSmall == 1;
				if (unNecessaryPossiblesReduction) {
					++nUnnecessary;
					sb.append(String.format(" %2d%s UNNECESSARY possibles reduction!", nUnnecessary,
							SudokuStatics.getNumberSuffix(nUnnecessary)));
				} else {
					++nNecessary;
					sb.append(String.format(" %2d%s necessary possibles reduction; %d solutions.", nNecessary,
							SudokuStatics.getNumberSuffix(nNecessary), nSolutionsSmall));
				}
				SudokuStatics.dumpString(ps, sb.toString());
			}
			final StringBuffer sb = new StringBuffer();
			final SudokuNode rootNode = sudoku.solve();
			sb.append("\n\n");
			sb.append(String.format("Full set of Possibles Reductions:\n"));
			rootNode.appendStringBuffer(sb);
			SudokuStatics.dumpString(ps, sb.toString());
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
