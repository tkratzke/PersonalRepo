package com.skagit.linearSystemGenerator;

import java.io.PrintStream;
import java.util.Random;
import java.util.Scanner;

import com.skagit.advMath.mitLinearAlgebra.RandomMatrixBuilder;

public class LinearSystemGenerator {

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

	public void mainLoop() {
		_activeLinearSystem = getNextLinearSystem();
		for (;;) {
			if (_activeLinearSystem._askingToQuit) {
				return;
			}
			final Random r = _rmb.getRng();
			if (_activeLinearSystem._askingToGiveUp || _activeLinearSystem.isSolved()) {
				_ps.printf("\n%s\n", _activeLinearSystem.selectRemark(r));
				_activeLinearSystem = getNextLinearSystem();
			}
			final String helpString = _activeLinearSystem.getHelpString();
			if (helpString != null && helpString.length() > 0) {
				_ps.printf("%s\n", helpString);
				_activeLinearSystem.turnOffAskingForHelp();
			}
			_ps.printf("\n%s\n", _activeLinearSystem.getString());
			final String line = _sysInScanner.nextLine();
			final Command command = new Command(_activeLinearSystem, line);
			if (command._commandType == null) {
				_errPs.printf(">>>\"%s\"<<< has bad syntax.\n", line);
				_activeLinearSystem.turnOnAskingForHelp();
				continue;
			}
			if (command != null) {
				final CommandType commandType = command._commandType;
				switch (commandType) {
				case ADD:
				case SUBTRACT:
					_activeLinearSystem.doAddOrSubtract(command);
					break;
				case ZERO:
					/** Use A to Zero out x in B", "A x :: B */
					if (!_activeLinearSystem.doZeroOut(command)) {
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
					}
					break;
				case MULTIPLY:
				case DIVIDE:
					_activeLinearSystem.doMultiplyOrDivide(command);
					break;
				case SWITCH_EQUTNS:
					_activeLinearSystem.doSwitchEqutns(command);
					break;
				case SWITCH_VBLS:
					_activeLinearSystem.doSwitchVbls(command);
					break;
				case SET:
				case UNSET:
					_activeLinearSystem.doSetOrUnset(command);
					break;
				case TELL:
					_activeLinearSystem.doTell(command);
					break;
				case GIVE_UP:
					_activeLinearSystem.doGiveUp(command);
					break;
				case HELP:
					_activeLinearSystem.doHelp(command);
					break;
				case QUIT:
					_activeLinearSystem.doQuit(command);
					break;
				}
			}
		}
	}

	private LinearSystem getNextLinearSystem() {
		final Difficulty difficulty;
		if (_activeLinearSystem == null) {
			difficulty = LinearSystemStatics._InitialDifficulty;
		} else {
			difficulty = _activeLinearSystem.getEasier() ? _activeLinearSystem._difficulty.getEasier()
					: _activeLinearSystem._difficulty.getHarder();
		}
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
