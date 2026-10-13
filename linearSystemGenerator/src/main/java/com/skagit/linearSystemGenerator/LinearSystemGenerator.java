package com.skagit.linearSystemGenerator;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;

import com.skagit.advMath.mitLinearAlgebra.PaqLu;
import com.skagit.advMath.mitLinearAlgebra.RandomMatrixBuilder;
import com.skagit.utils.CombinatoricTools;
import com.skagit.utils.PermutationTools;
import com.skagit.utils.ResourcesLister0;
import com.skagit.utils.ResourcesLister0.CpEntryAndRelativePath;
import com.skagit.utils.StaticUtilities;

public class LinearSystemGenerator {

	private static String[] _InsultsForInfeasible = { //
			"You are just simply so wrong.  Sad, just sad.",
			"Just why would you think this is a good set of values?  Nobody else does.",
			"The wrongness of this set of assignments boggles the mind.", "This is horrible!",
			"This was an easy problem and you blew it.  The anguish your parents must feel.",
			"Has humanity really regressed this far in just a single generation?", //
			"You're the second person in history to miss this problem.  The first was some kid in 1948 in SE Indiana.", //
			"I'm glad you're not related to me.  You aren't, are you?", //
			"Heavy sigh.  Do you have ANY basic algebra skills?", //
	};

	private static String[] _InsultsForSolved = { //
			"Wait, what, you got it?!  You?!.", "Ok, you just proved that wild guesses are not guaranteed to be wrong.", // ,
			"I was solving these problems when I was 7 so I'm surprised you got it right.",
			"You got it.  Obviously, that was too easy a problem.",
			"Congratulations; you're now in at least the 5th percentile of intelligence.", //
			"Yes!! We found a problem easy enough for you to solve!", //
			"Right.  I guess that Bà and Ông have had SOME influence.", //
			"You got it, but it took you Soooo long.", //
			"Such an easy problem.  Most people don't have to think that hard about it.", //
	};

	public String getHelpString() {
		final EnumSet<CommandType> enumSet = _activeLinearSystem == null ? CommandType._HelpForInit
				: CommandType._HelpForNonInit;
		final StringBuilder sb = new StringBuilder("English Example (Example(s) of what you enter):");
		for (final CommandType commandType : enumSet) {
			sb.append(String.format("\n\t%s", commandType._englishExample));
			final String exampleString = commandType._exampleCommand;
			if (exampleString.length() > 0) {
				sb.append(String.format(" (%s)", commandType._exampleCommand));
			}
		}
		return sb.toString();
	}

	private static String selectInsult(final Random r, final LsStatus lsStatus) {
		if (lsStatus == LsStatus.SOLVED) {
			return _InsultsForSolved[r.nextInt(_InsultsForSolved.length)];
		}
		if (lsStatus == LsStatus.INFEASIBLE) {
			return _InsultsForInfeasible[r.nextInt(_InsultsForInfeasible.length)];
		}
		return "";
	}

	public class LinearSystem {
		final String[] _equtnNames;
		final HashMap<String, Integer> _equtnNameToIdx;
		final String[] _vblNames;
		final HashMap<String, Integer> _vblNameToIdx;
		final int[] _orderToPrintEqutns;
		final int[] _orderToPrintVbls;
		final double[][] _coefs;
		final double[] _correctValues;
		final double[] _guessedValues;
		final double[] _rhs;

		public LinearSystem(final double[][] coefs, final double[] correctValues) {
			_coefs = coefs;
			_correctValues = correctValues.clone();
			final int m = coefs.length, n = coefs[0].length;
			_rhs = new double[m];
			_equtnNames = new String[m];
			_equtnNameToIdx = new HashMap<>();
			for (int i = 0; i < m; ++i) {
				final String equtnName = Statics.getNominalEqutnName(m, i);
				_equtnNames[i] = equtnName;
				_equtnNameToIdx.put(equtnName, i);
				_rhs[i] = 0d;
				for (int j = 0; j < n; ++j) {
					_rhs[i] += _coefs[i][j] * _correctValues[j];
				}
			}
			_orderToPrintEqutns = CombinatoricTools.getInitial(m);
			_guessedValues = new double[n];
			Arrays.fill(_guessedValues, Double.NaN);
			_vblNames = new String[n];
			_vblNameToIdx = new HashMap<>();
			for (int j = 0; j < n; ++j) {
				final String vblName = Statics.getNominalVblName(n, j);
				_vblNames[j] = vblName;
				_vblNameToIdx.put(vblName, j);
			}
			_orderToPrintVbls = CombinatoricTools.getInitial(n);
		}

		void doNew(final Command command) {
			final int m = (int) (command._d0);
			final int n = (int) (command._d1);
			final boolean upperTriangular = command._easy;
			_activeLinearSystem = generateLinearSystem(m, n, upperTriangular);
		}

		void doSetOrUnset(final Command command) {
			final int j = _vblNameToIdx.get(command._vbl0Name);
			_guessedValues[j] = command._d0;
		}

		boolean isSolved() {
			final int n = _coefs[0].length;
			for (int j = 0; j < n; ++j) {
				final double guessedValue = _guessedValues[j];
				if (!Double.isFinite(guessedValue) || (Math.abs(_correctValues[j]) - guessedValue) > 0.5) {
					return false;
				}
			}
			return true;
		}

		public String getString() {
			final StringBuilder s = new StringBuilder().append(Statics.getSystemString(_orderToPrintEqutns,
					_orderToPrintVbls, _equtnNames, _vblNames, _guessedValues, _coefs, _rhs));
			final int n = _coefs[0].length;
			for (int iPass = 0; iPass < 2; ++iPass) {
				boolean doneIntro = false;
				final boolean doGuesses = iPass == 0;
				for (int j = 0; j < n; ++j) {
					final double value = doGuesses ? _guessedValues[j] : _correctValues[j];
					if (Double.isFinite(value)) {
						if (!doneIntro) {
							s.append("\n" + (doGuesses ? "Guessed values:" : "Correct values:"));
							doneIntro = true;
						}
						s.append(String.format("\n%s = %s", _vblNames[j], Statics.getNumericString(value)));
					}
				}
			}
			return s.toString();
		}

		@Override
		public String toString() {
			return getString();
		}

		public int getEqutnIdx(final String equtnName) {
			final Integer integer = _equtnNameToIdx.get(equtnName.toUpperCase());
			return integer == null ? -1 : integer;
		}

		public int getVblIdx(final String vblName) {
			final Integer integer = _vblNameToIdx.get(vblName.toLowerCase());
			return integer == null ? -1 : integer;
		}

		public LsStatus getStatus() {
			return Statics.getStatus(_coefs, _guessedValues, _rhs);
		}
	}

	final int _loMult;
	final int _hiMult;
	final int _loNudge;
	final int _hiNudge;
	final RandomMatrixBuilder _rmb;
	final PrintStream _ps;
	final PrintStream _errPs;
	Scanner _sysInScanner;
	LinearSystem _activeLinearSystem;

	public LinearSystemGenerator(final int seed, final int loMult, final int hiMult, final int loNudge,
			final int hiNudge, final Scanner sysInScanner) {
		_loNudge = loNudge;
		_hiNudge = hiNudge;
		_loMult = loMult;
		_hiMult = hiMult;
		_rmb = new RandomMatrixBuilder(seed, loNudge, hiNudge, loMult, hiMult);
		_ps = System.out;
		_errPs = System.err;
		_sysInScanner = sysInScanner;
		_activeLinearSystem = null;
		try {
			final String content = java.nio.file.Files
					.readString(Paths.get(LinearSystemGenerator.class.getResource("Insults.txt").toURI()));
			_ps.printf("%s", content);
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
		}
	}

	public LinearSystem generateLinearSystem(final int m, final int n, final boolean upperTriangular) {
		final double[][] coefs = _rmb.buildMatrix(m, n, /* indClmnsArray= */null, upperTriangular);
		final Random r = _rmb.getRng();
		final int[] perm = PermutationTools.randomPermutation(m, r);
		for (int i = 0; i < m; ++i) {
			final double[] rowI = coefs[i];
			coefs[i] = coefs[perm[i]];
			coefs[perm[i]] = rowI;
		}
		final double[][] valuesMtx = _rmb.buildMatrix(1, n, /* indClmnsArray= */null, upperTriangular);
		final double[] values = valuesMtx[0];
		return new LinearSystem(coefs, values);
	}

	public void mainLoop() {
		boolean needHelpString = true;
		boolean firstTimeThrough = true;
		for (;;) {
			_ps.print(firstTimeThrough ? "" : "\n\n");
			firstTimeThrough = false;
			if (needHelpString) {
				_ps.printf("%s\n", getHelpString());
				needHelpString = false;
			}
			if (_activeLinearSystem != null) {
				_ps.printf("%s\n", _activeLinearSystem.getString());
			}
			final String line = _sysInScanner.nextLine();
			final Command command = Command.getCommand(line, _activeLinearSystem);
			if (command != null) {
				switch (command._commandType) {
				case ADD:
				case SUBTRACT:
					doAddOrSubtract(command);
					break;
				case ZERO:
					doZeroOut(command);
					break;
				case MULTIPLY:
				case DIVIDE:
					doMultiplyOrDivide(command);
					break;
				case SWITCH:
					doSwitch(command);
					break;
				case SET:
				case UNSET:
					final LsStatus lsStatus = doSet(command);
					switch (lsStatus) {
					case FEASIBLE:
						break;
					case SOLVED:
						_ps.printf("\n%s\nTry a new problem.", selectInsult(_rmb.getRng(), lsStatus));
						_activeLinearSystem = null;
						needHelpString = true;
						break;
					case INFEASIBLE:
					default:
						_errPs.printf("\n%s\nYou'll have to back off some of your assignments with Set x =",
								selectInsult(_rmb.getRng(), lsStatus));
						needHelpString = true;
						break;
					}
					break;
				case NEW:
					final int m = (int) Math.round(command._d0);
					final int n = (int) Math.round(command._d1);
					final boolean upperTriangular = command._easy;
					_activeLinearSystem = generateLinearSystem(m, n, upperTriangular);
					needHelpString = true;
					break;
				case SYNTAX_ERROR:
					_errPs.printf("\n>>>%s<<<\n\thas bad syntax.", line);
					needHelpString = true;
					break;
				case HELP:
					needHelpString = true;
					continue;
				case QUIT:
				default:
					return;
				}
			}
		}
	}

	private void doAddOrSubtract(final Command command) {
		final double[][] coefs = _activeLinearSystem._coefs;
		/** Example: Add 2A to B. */
		final int iA = _activeLinearSystem.getEqutnIdx(command._equtn0Name);
		final int iB = _activeLinearSystem.getEqutnIdx(command._equtn1Name);
		final CommandType commandType = command._commandType;
		final double d = commandType == CommandType.ADD ? command._d0 : -command._d0;
		final int n = coefs[0].length;
		for (int j = 0; j < n; ++j) {
			coefs[iB][j] += d * coefs[iA][j];
		}
		final double[] rhs = _activeLinearSystem._rhs;
		rhs[iB] += d * rhs[iA];
	}

	private void doZeroOut(final Command command) {
		final double[][] coefs = _activeLinearSystem._coefs;
		/** Example: "Zero out x in B by using A" */
		final int jX = _activeLinearSystem.getVblIdx(command._vbl0Name);
		final int iB = _activeLinearSystem.getEqutnIdx(command._equtn0Name);
		final int iA = _activeLinearSystem.getEqutnIdx(command._equtn1Name);
		final int n = coefs[0].length;
		final double multiplier = coefs[iB][jX] / coefs[iA][jX];
		coefs[iB][jX] = 0d;
		for (int j = 0; j < n; ++j) {
			if (j != jX) {
				coefs[iB][j] -= multiplier * coefs[iA][j];
			}
		}
		final double[] rhs = _activeLinearSystem._rhs;
		rhs[iB] -= multiplier * rhs[iA];
	}

	private void doMultiplyOrDivide(final Command command) {
		final double[][] coefs = _activeLinearSystem._coefs;
		/** Example: Multiply A by 2. */
		final int iA = _activeLinearSystem.getEqutnIdx(command._equtn0Name);
		final CommandType commandType = command._commandType;
		final double d = commandType == CommandType.MULTIPLY ? command._d0 : (1d / command._d0);
		final int n = coefs[0].length;
		for (int j = 0; j < n; ++j) {
			coefs[iA][j] *= d;
		}
		_activeLinearSystem._rhs[iA] *= d;
	}

	private void doSwitch(final Command command) {
		final double[][] coefs = _activeLinearSystem._coefs;
		/** Example: Switch A and B or Switch x and y. */
		if (command._equtn0Name != null) {
			/** We're switching rows. */
			final int m = coefs.length;
			final int i0 = _activeLinearSystem.getEqutnIdx(command._equtn0Name);
			final int i1 = _activeLinearSystem.getEqutnIdx(command._equtn1Name);
			int k0 = -1, k1 = -1;
			for (int i = 0; i < m; ++i) {
				final int ix = _activeLinearSystem._orderToPrintEqutns[i];
				if (ix == i0) {
					k0 = i;
				} else if (ix == i1) {
					k1 = i;
				}
				if (k0 >= 0 && k1 >= 0) {
					break;
				}
			}
			_activeLinearSystem._orderToPrintEqutns[k0] = i1;
			_activeLinearSystem._orderToPrintEqutns[k1] = i0;
		} else {
			/** We're switching columns. */
			final int n = coefs[0].length;
			final int j0 = _activeLinearSystem.getVblIdx(command._vbl0Name);
			final int j1 = _activeLinearSystem.getVblIdx(command._vbl1Name);
			int k0 = -1, k1 = -1;
			for (int j = 0; j < n; ++j) {
				final int jx = _activeLinearSystem._orderToPrintVbls[j];
				if (jx == j0) {
					k0 = j;
				} else if (jx == j1) {
					k1 = j;
				}
				if (k0 >= 0 && k1 >= 0) {
					break;
				}
			}
			_activeLinearSystem._orderToPrintVbls[k0] = j1;
			_activeLinearSystem._orderToPrintVbls[k1] = j0;
		}
	}

	private LsStatus doSet(final Command command) {
		final int j = _activeLinearSystem.getVblIdx(command._vbl0Name);
		_activeLinearSystem._guessedValues[j] = command._d0;
		return _activeLinearSystem.getStatus();
	}

	public static LsStatus getStatus(final double[][] coefs0, final double[] guessedValues, final double[] rhs0) {
		final int m0 = coefs0.length, n0 = coefs0[0].length;
		final BitSet realVbls = new BitSet(n0);
		for (int j0 = 0; j0 < n0; ++j0) {
			realVbls.set(j0, !Double.isFinite(guessedValues[j0]));
		}
		final int n = realVbls.cardinality();
		final BitSet realEqutns = new BitSet(m0);
		for (int i0 = 0; i0 < m0; ++i0) {
			realEqutns.clear(i0);
			for (int j0 = realVbls.nextSetBit(0); j0 >= 0; j0 = realVbls.nextSetBit(j0 + 1)) {
				if (coefs0[i0][j0] != 0d) {
					realEqutns.set(i0);
					continue;
				}
			}
		}
		/** If some dead equation is violated, return fail. */
		for (int i0 = realEqutns.nextClearBit(0); i0 < m0; i0 = realEqutns.nextClearBit(i0 + 1)) {
			double dotProduct = 0d;
			for (int j0 = realVbls.nextClearBit(0); j0 < n0; j0 = realVbls.nextClearBit(j0 + 1)) {
				dotProduct += guessedValues[j0] * coefs0[i0][j0];
			}
			if (Math.abs(dotProduct - rhs0[i0]) > 1.0e-5) {
				return LsStatus.INFEASIBLE;
			}
		}
		/** If all variables are set, it's solved. */
		if (realVbls.cardinality() == n0) {
			return LsStatus.SOLVED;
		}
		/** Use PaqLu to check to see if it's still possible. */
		final int m = realEqutns.cardinality();
		final double[][] coefs = new double[m][n];
		final double[][] rhsRows = new double[m][1];
		for (int i0 = realEqutns.nextSetBit(0), i = 0; i0 >= 0; i0 = realEqutns.nextSetBit(i0 + 1), ++i) {
			rhsRows[i][0] = rhs0[i0];
			for (int j0 = 0, j = 0; j0 < n0; ++j0) {
				final double coef = coefs0[i0][j0];
				if (realVbls.get(j0)) {
					coefs[i][j++] = coef;
				} else {
					rhsRows[i][0] -= coef * guessedValues[j0];
				}
			}
		}
		final PaqLu paqLu = new PaqLu(coefs, PaqLu.Pivoting.Full);
		final double[][] answerRows = paqLu.solve(rhsRows);
		for (int i = 0; i < m; ++i) {
			if (Double.isNaN(answerRows[i][0])) {
				return LsStatus.INFEASIBLE;
			}
		}
		return LsStatus.FEASIBLE;
	}

	public static void main(final String[] args) {
		final String regExp = ".*\\.txt";
		@SuppressWarnings("rawtypes")
		final Class clazz = StaticUtilities.getMyClass();
		final Package thisPackage = clazz.getPackage();
		final Collection<CpEntryAndRelativePath> list = ResourcesLister0.getCpEntryAndRelativePaths(thisPackage, regExp,
				true, true);
		for (final CpEntryAndRelativePath resourceNamePlus : list) {
			System.out.println(resourceNamePlus.getString());
		}
		/**
		 * <pre>
		 * try (Scanner sysInScanner = new Scanner(System.in)) {
		 * 	final LinearSystemGenerator lsg = new LinearSystemGenerator(10291953, -2, 8, -3, 7, sysInScanner);
		 * 	lsg.mainLoop();
		 * 	lsg._ps.println();
		 * 	lsg._ps.println("Exiting Program");
		 * } catch (final Exception e) {
		 * 	e.printStackTrace();
		 * }
		 * </pre>
		 */
	}
}
