package com.skagit.linearSystemGenerator;

import java.io.PrintStream;
import java.util.EnumSet;
import java.util.Random;
import java.util.Scanner;

import com.skagit.advMath.mitLinearAlgebra.RandomMatrixBuilder;
import com.skagit.utils.StringUtilities;

public class LinearSystemGenerator {

	final private static String _Clmn0Hdr = "To do this";
	final private static int _Clmn0HdrLen = _Clmn0Hdr.length();
	final private static String _Clmn1Hdr = "Enter this";
	final private static int _Clmn1HdrLen = _Clmn1Hdr.length();
	final private static String _StringBetweenClmns = "   ";

	private static String getHelpString(final EnumSet<CommandType> toComment, final boolean forbidZeroOut) {
		/** extraWrinkles here means that we forbid the zero out command. */
		if (toComment.isEmpty()) {
			return "";
		}
		int clmn0Len = _Clmn0Hdr.length();
		int clmn1Len = _Clmn1Hdr.length();
		for (final CommandType commandType : toComment) {
			if (!forbidZeroOut || commandType._operator != Operator.ZERO) {
				clmn0Len = Math.max(clmn0Len, commandType._englishExample.length());
				clmn1Len = Math.max(clmn1Len, commandType._commandExample.length());
			}
		}
		final int ltPad0 = (clmn0Len - _Clmn0HdrLen) / 2;
		String fullLtHdr = (ltPad0 > 0 ? StringUtilities.padLeft("", ltPad0) : "") + _Clmn0Hdr;
		final int rtPad0 = clmn0Len - fullLtHdr.length();
		if (rtPad0 > 0) {
			fullLtHdr += StringUtilities.padRight("", rtPad0);
		}
		final StringBuilder s = new StringBuilder("  ").append(fullLtHdr).append(_StringBetweenClmns);
		final int ltPad1 = (clmn1Len - _Clmn1HdrLen) / 2;
		final String fullRtHdr = (ltPad1 > 0 ? StringUtilities.padLeft("", ltPad1) : "") + _Clmn1Hdr;
		s.append(fullRtHdr);
		for (final CommandType commandType : toComment) {
			if (!forbidZeroOut || commandType._operator != Operator.ZERO) {
				final String f = "\n  %-" + clmn0Len + "s%s%s";
				s.append(String.format(f, commandType._englishExample, _StringBetweenClmns,
						commandType._commandExample));
			}
		}
		return s.toString();
	}

	private static String selectRemark(final Random r, final LsStatus lsStatus) {
		if (lsStatus == LsStatus.SOLVED) {
			return LinearSystemStatics._RemarksForSolved[r.nextInt(LinearSystemStatics._RemarksForSolved.length)];
		}
		if (lsStatus == LsStatus.INFEASIBLE) {
			return LinearSystemStatics._RemarksForInfeasible[r
					.nextInt(LinearSystemStatics._RemarksForInfeasible.length)];
		}
		return "";
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

	public LinearSystemGenerator(final long seed, final int loMult, final int hiMult, final int loNudge,
			final int hiNudge, final Scanner sysInScanner) {
		_loNudge = loNudge;
		_hiNudge = hiNudge;
		_loMult = loMult;
		_hiMult = hiMult;
		_rmb = new RandomMatrixBuilder(seed, loMult, hiMult, loNudge, hiNudge);
		_ps = System.out;
		_errPs = System.err;
		_sysInScanner = sysInScanner;
		_activeLinearSystem = null;
	}

	public LinearSystem generateLinearSystem(final Difficulty difficulty) {
		final Random r = _rmb.getRng();
		final double[][] coefsDbl = LinearSystemStatics.generateLinearSystemDbl(_rmb, difficulty);
		final int m = coefsDbl.length, n = coefsDbl[0].length;
		final double[] values = new double[n];
		final long lo = -((long) _loMult * _loMult) - Math.abs(_loNudge);
		final long hi = ((long) _hiMult * _hiMult) + Math.abs(_hiNudge) + 1L;
		for (int j = 0; j < n; ++j) {
			values[j] = lo + r.nextLong(hi - lo);
		}
		final double[] rhsDbl = new double[m];
		for (int i = 0; i < m; ++i) {
			rhsDbl[i] = 0d;
			for (int j = 0; j < n; ++j) {
				rhsDbl[i] += coefsDbl[i][j] * values[j];
			}
		}
		return new LinearSystem(coefsDbl, rhsDbl, difficulty);
	}

	public void mainLoop() {
		EnumSet<CommandType> toComment = CommandType._AllCommandTypes;
		boolean goForward = true;
		_activeLinearSystem = null;
		Difficulty difficulty = LinearSystemStatics._InitialDifficulty;
		final boolean forbidZeroOut = difficulty._singular;
		for (;;) {
			if (_activeLinearSystem == null) {
				_activeLinearSystem = generateLinearSystem(difficulty);
				goForward = true;
			}
			_ps.printf("%s", getHelpString(toComment, forbidZeroOut));
			if (toComment == CommandType._AllCommandTypes) {
				toComment = CommandType._None;
			}
			_ps.printf("\n%s\n", _activeLinearSystem.getString());
			final String line = _sysInScanner.nextLine();
			final Command command = new Command(_activeLinearSystem, line, forbidZeroOut);
			if (command._commandType == null) {
				_errPs.printf(">>>\"%s\"<<< has bad syntax.\n", line);
				toComment = CommandType._AllCommandTypes;
				continue;
			}
			if (command != null) {
				final CommandType commandType = command._commandType;
				switch (commandType) {
				case ADD:
				case SUBTRACT:
					_activeLinearSystem.addOrSubtract(command);
					break;
				case ZERO:
					/** Use A to Zero out x in B", "A x :: B */
					if (!_activeLinearSystem.zeroOut(command)) {
						final int iA = command._equtn0Idx;
						final String equtnAName = _activeLinearSystem.getEqutnName(iA);
						final int j = command._vbl0Idx;
						final String vblName = _activeLinearSystem.getVblName(j);
						_errPs.printf("""
								>>>
								Cannot use %s to zero out %s in any equtn.
								The multiplier of %s in %s is 0.0 and this
								would lead to a divide by zero.
								<<<
								""", //
								equtnAName, vblName, vblName, equtnAName);
						toComment = CommandType._AllCommandTypes;
					}
					break;
				case MULTIPLY:
				case DIVIDE:
					_activeLinearSystem.multiplyOrDivide(command);
					break;
				case SWITCH_EQUTNS:
					_activeLinearSystem.switchEqutns(command);
					break;
				case SWITCH_VBLS:
					_activeLinearSystem.switchVbls(command);
					break;
				case TELL:
				case SET:
				case UNSET:
					/** Both doTell and setOrUnset will restore they system if infeasible. */
					final LsStatus lsStatus;
					if (commandType == CommandType.TELL) {
						lsStatus = _activeLinearSystem.doTell(command);
						goForward = false;
					} else {
						lsStatus = _activeLinearSystem.setOrUnset(command);
						if (goForward && lsStatus == LsStatus.INFEASIBLE) {
							goForward = false;
						}
					}
					switch (lsStatus) {
					case FEASIBLE:
						break;
					case SOLVED:
						_ps.printf("\n%s\n", _activeLinearSystem.getString());
						_ps.printf("\n%s  Try a new problem:", selectRemark(_rmb.getRng(), lsStatus));
						difficulty = goForward ? difficulty.getHarder() : difficulty.getEasier();
						_activeLinearSystem = generateLinearSystem(difficulty);
						goForward = true;
						break;
					case INFEASIBLE:
					default:
						_errPs.print(selectRemark(_rmb.getRng(), lsStatus));
						goForward = false;
						break;
					}
					break;
				case HELP:
					toComment = CommandType._AllCommandTypes;
					continue;
				case QUIT:
				default:
					return;
				}
			}
		}
	}

	public static void main(final String[] args) {
		try (Scanner sysInScanner = new Scanner(System.in)) {
			final long seed = LinearSystemStatics._Seed;
			final int loMult = LinearSystemStatics._LoMult;
			final int hiMult = LinearSystemStatics._HiMult;
			final int loNudge = LinearSystemStatics._LoNudge;
			final int hiNudge = LinearSystemStatics._HiNudge;
			final LinearSystemGenerator linearSystemGenerator = new LinearSystemGenerator(seed, loMult, hiMult, loNudge,
					hiNudge, sysInScanner);
			linearSystemGenerator.mainLoop();
			linearSystemGenerator._ps.println();
			linearSystemGenerator._ps.println("Exiting Program");
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
