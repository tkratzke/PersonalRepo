package com.skagit.linearSystemGenerator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Properties;
import java.util.Random;
import java.util.Scanner;

import com.skagit.advMath.mitLinearAlgebra.RandomMatrixBuilder;
import com.skagit.utils.ResourcesLister0;
import com.skagit.utils.StaticUtilities;
import com.skagit.utils.StringUtilities;

public class LinearSystemGenerator {

	final private static String[] _ForInfeasible;

	final private static String[] _ForSolved;

	final private static Difficulty _InitialDifficulty;

	static {
		final Class<?> clazz = StaticUtilities.getMyClass();
		final Package pckg = clazz.getPackage();
		final ArrayList<ResourcesLister0.CpEntryAndRelativePath> cpEntryAndRelativePaths0 = ResourcesLister0
				.getCpEntryAndRelativePaths(pckg, "Remarks.*\\.txt", true, true);
		final ArrayList<String> forInfeasible = new ArrayList<>();
		final ArrayList<String> forSolved = new ArrayList<>();
		boolean onRight = true;
		for (final ResourcesLister0.CpEntryAndRelativePath cpEntryAndRelativePath : cpEntryAndRelativePaths0) {
			String relativePath = cpEntryAndRelativePath._relativePath;
			relativePath = "/" + relativePath.replace('\\', '/');
			try (InputStream in = clazz.getResourceAsStream(relativePath);
					BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
				String line;
				while ((line = reader.readLine()) != null) {
					line = line.trim();
					final int lineLen = line.length();
					if (lineLen == 0 || line.charAt(0) == '#') {
						continue;
					}
					if ("For Infeasible".equalsIgnoreCase(line)) {
						onRight = false;
						continue;
					}
					if ("For Solved".equalsIgnoreCase(line)) {
						onRight = true;
						continue;
					}
					if (onRight) {
						forSolved.add(line);
					} else {
						forInfeasible.add(line);
					}
				}
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		_ForInfeasible = forInfeasible.toArray(new String[forInfeasible.size()]);
		_ForSolved = forSolved.toArray(new String[forSolved.size()]);
	}

	static {
		final Class<?> clazz = StaticUtilities.getMyClass();
		final Package pckg = clazz.getPackage();
		final ArrayList<ResourcesLister0.CpEntryAndRelativePath> cpEntryAndRelativePaths0 = ResourcesLister0
				.getCpEntryAndRelativePaths(pckg, "InitialDifficulty.properties", true, true);
		final Properties properties = new Properties();
		int m = -1;
		TypeOfProblem typeOfProblem = null;
		OuterLoop: for (final ResourcesLister0.CpEntryAndRelativePath cpEntryAndRelativePath : cpEntryAndRelativePaths0) {
			String relativePath = cpEntryAndRelativePath._relativePath;
			relativePath = "/" + relativePath.replace('\\', '/');
			try (InputStream in = clazz.getResourceAsStream(relativePath)) {
				properties.load(in);
				if (m <= 0) {
					m = Integer.parseInt((String) properties.get("m"));
				}
				if (typeOfProblem == null) {
					typeOfProblem = TypeOfProblem.valueOf(properties.getProperty("typeOfProblem"));
				}
				if (m > 0 && typeOfProblem != null) {
					break OuterLoop;
				}
			} catch (final IOException e) {
			}
		}
		if (m >= 0 && typeOfProblem != null) {
			_InitialDifficulty = new Difficulty(m, typeOfProblem);
		} else {
			_InitialDifficulty = new Difficulty(3, TypeOfProblem.TRI);
		}
	}

	final private static String _Clmn0Hdr = "To do this";
	final private static int _Clmn0HdrLen = _Clmn0Hdr.length();
	final private static String _Clmn1Hdr = "Enter this";
	final private static int _Clmn1HdrLen = _Clmn1Hdr.length();
	final private static String _StringBetweenClmns = "     ";

	private static String getHelpString(final EnumSet<CommandType> toComment, final TypeOfProblem typeOfProblem) {
		if (toComment.isEmpty()) {
			return "";
		}
		int clmn0Len = _Clmn0Hdr.length();
		int clmn1Len = _Clmn1Hdr.length();
		for (final CommandType commandType : toComment) {
			if (typeOfProblem != TypeOfProblem.FORBID_ZERO_OUT || commandType._operator != Operator.ZERO) {
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
		final StringBuilder s = new StringBuilder("\t").append(fullLtHdr).append(_StringBetweenClmns);
		final int ltPad1 = (clmn1Len - _Clmn1HdrLen) / 2;
		final String fullRtHdr = (ltPad1 > 0 ? StringUtilities.padLeft("", ltPad1) : "") + _Clmn1Hdr;
		s.append(fullRtHdr);
		for (final CommandType commandType : toComment) {
			if (typeOfProblem != TypeOfProblem.FORBID_ZERO_OUT || commandType._operator != Operator.ZERO) {
				final String f = "\n\t%-" + clmn0Len + "s%s%s";
				s.append(String.format(f, commandType._englishExample, _StringBetweenClmns,
						commandType._commandExample));
			}
		}
		return s.toString();
	}

	private static String selectRemark(final Random r, final LsStatus lsStatus) {
		if (lsStatus == LsStatus.SOLVED) {
			return _ForSolved[r.nextInt(_ForSolved.length)];
		}
		if (lsStatus == LsStatus.INFEASIBLE) {
			return _ForInfeasible[r.nextInt(_ForInfeasible.length)];
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
	}

	public LinearSystem generateLinearSystem(final Difficulty difficulty) {
		final int m = difficulty._nEquations;
		final int n;
		boolean upperTriangular = false;
		switch (difficulty._typeOfProblem) {
		case TRI:
			upperTriangular = true;
		case FULL:
			n = m;
			break;
		case ONE_EXTRA:
			n = m + 1;
			break;
		case TWO_EXTRA:
		case FORBID_ZERO_OUT:
		default:
			n = m + 2;
			break;
		}
		final double[][] coefs = _rmb.buildMatrix(m, n, /* indClmnsArray= */null, upperTriangular);
		final Random r = _rmb.getRng();
		final double[][] correctValuesMtx = _rmb.buildMatrix(1, n, /* indClmnsArray= */null, upperTriangular);
		final double[] correctValues = correctValuesMtx[0];
		return new LinearSystem(coefs, correctValues, r);
	}

	public void mainLoop() {
		EnumSet<CommandType> toComment = CommandType._AllCommandTypes;
		boolean goForward = true;
		_activeLinearSystem = null;
		Difficulty difficulty = _InitialDifficulty;
		for (;;) {
			if (_activeLinearSystem == null) {
				_activeLinearSystem = generateLinearSystem(difficulty);
				goForward = true;
			} else {
				_ps.print("\n");
			}
			_ps.printf("%s", getHelpString(toComment, difficulty._typeOfProblem));
			if (toComment == CommandType._AllCommandTypes) {
				toComment = CommandType._None;
			}
			_ps.printf("\n%s\n", _activeLinearSystem.getString());
			final String line = _sysInScanner.nextLine();
			final Command command = new Command(_activeLinearSystem, line, difficulty._typeOfProblem);
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
					goForward = false;
				case SET:
				case UNSET:
					if (commandType == CommandType.TELL) {
						_activeLinearSystem.doTell(command);
					} else {
						_activeLinearSystem.doSetOrUnset(command);
					}
					final LsStatus lsStatus = _activeLinearSystem.getStatus();
					switch (lsStatus) {
					case FEASIBLE:
						break;
					case SOLVED:
						_ps.printf("\n%s  Try a new problem:", selectRemark(_rmb.getRng(), lsStatus));
						difficulty = goForward ? difficulty.next() : difficulty.pvs();
						_activeLinearSystem = generateLinearSystem(difficulty);
						goForward = true;
						break;
					case INFEASIBLE:
					default:
						_errPs.printf("""
								%s  Back off some of your assignments by using
								something like \"x =\" for one of your guessed values.
								""", selectRemark(_rmb.getRng(), lsStatus));
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
			final LinearSystemGenerator lsg = new LinearSystemGenerator(10291953, -2, 8, -3, 7, sysInScanner);
			lsg.mainLoop();
			lsg._ps.println();
			lsg._ps.println("Exiting Program");
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
