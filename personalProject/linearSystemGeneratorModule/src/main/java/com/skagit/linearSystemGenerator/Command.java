package com.skagit.linearSystemGenerator;

import com.skagit.utils.MyFraction;

public class Command {
	public final LinearSystem _linearSystem;
	public final CommandType _commandType;
	final int _equtn0Idx, _equtn1Idx;
	final int _vbl0Idx, _vbl1Idx;
	final MyFraction _frctn;

	public Command(final LinearSystem linearSystem, final CommandType commandType, final int equtn0Idx,
			final int equtn1Idx, final int vbl0Idx, final int vbl1Idx, final MyFraction frctn) {
		_linearSystem = linearSystem;
		_commandType = commandType;
		_equtn0Idx = equtn0Idx;
		_equtn1Idx = equtn1Idx;
		_vbl0Idx = vbl0Idx;
		_vbl1Idx = vbl1Idx;
		_frctn = frctn;
	}

	public String getString() {
		final StringBuilder s = new StringBuilder().append(String.format("%s", _commandType._englishExample));
		if (_equtn0Idx >= 0) {
			s.append(String.format(", Equtn0=%s", _linearSystem.getEqutnName(_equtn0Idx)));
		}
		if (_equtn1Idx >= 0) {
			s.append(String.format(", Equtn1=%s", _linearSystem.getEqutnName(_equtn1Idx)));
		}
		if (_vbl0Idx >= 0) {
			s.append(String.format(", Vbl0=%s", _linearSystem.getVblName(_vbl0Idx)));
		}
		if (_vbl1Idx >= 0) {
			s.append(String.format(", Vbl1=%s", _linearSystem.getVblName(_vbl1Idx)));
		}
		if (_frctn != null) {
			s.append(String.format(", d0=%s", _frctn.getString(/* printFloat= */false)));
		}
		return s.toString();
	}

	@Override
	public String toString() {
		return getString();
	}

	public Command(final LinearSystem linearSystem, String line, final TypeOfProblem typeOfProblem) {
		_linearSystem = linearSystem;
		/** Strip outside spaces and the ending period if it's there. */
		line = line.trim();
		final int lineLen = line.length();

		if (lineLen == 0) {
			/** Error. */
			_commandType = null;
			_equtn0Idx = _equtn1Idx = _vbl0Idx = _vbl1Idx = -1;
			_frctn = null;
			return;
		}
		if (line.charAt(lineLen - 1) == '.') {
			line = line.substring(0, lineLen - 1);
		}
		/**
		 * Do we have an operation, and if so, where in the string? Choose the operator
		 * with the longest _s, to distinguish between, eg, = and /=
		 */
		Operator operator = null;
		for (final Operator operator0 : Operator.values()) {
			final int operatorIdx0 = line.indexOf(operator0._s);
			if (operatorIdx0 >= 0) {
				final int oldLen = operator == null ? 0 : operator._s.length();
				final int thisLen = operator0._s.length();
				if (thisLen > oldLen) {
					operator = operator0;
				}
			}
		}
		if (operator == null) {
			/** No operator; must be help, quit, or an error. */
			final char c = Character.toLowerCase(line.charAt(0));
			final CommandType commandType = c == 'q' ? CommandType.QUIT : (c == 'h' ? CommandType.HELP : null);
			if (commandType != null) {
				_commandType = commandType;
				_equtn0Idx = _equtn1Idx = _vbl0Idx = _vbl1Idx = -1;
				_frctn = null;
				return;
			}
			/** Error. */
			_commandType = null;
			_equtn0Idx = _equtn1Idx = _vbl0Idx = _vbl1Idx = -1;
			_frctn = null;
			return;
		}

		if (operator == Operator.ZERO && typeOfProblem == TypeOfProblem.FORBID_ZERO_OUT) {
			/** Error. */
			_commandType = null;
			_equtn0Idx = _equtn1Idx = _vbl0Idx = _vbl1Idx = -1;
			_frctn = null;
			return;
		}

		/** We have an operator. */
		final int operatorIdx = line.indexOf(operator._s);
		final String beforeOperator = line.substring(0, operatorIdx).trim();
		final String afterOperator = line.substring(operatorIdx + operator._s.length()).trim();
		final int equtn0Idx = linearSystem.getEqutnIdx(beforeOperator);
		final int vbl0Idx = linearSystem.getVblIdx(beforeOperator);
		final boolean isEqutn0 = equtn0Idx >= 0;
		final boolean isVbl0 = vbl0Idx >= 0;
		/** beforeOperator should be a single equtn or vbl, unless operator is ZERO. */
		if (operator != Operator.ZERO && (isEqutn0 == isVbl0)) {
			/** Error. */
			_commandType = null;
			_equtn0Idx = _equtn1Idx = _vbl0Idx = _vbl1Idx = -1;
			_frctn = null;
			return;
		}

		/** If there is nothing past the operator, this must be "x =". */
		if (afterOperator.length() == 0) {
			if (operator == Operator.SET && vbl0Idx >= 0) {
				_commandType = CommandType.UNSET;
				_equtn0Idx = _equtn1Idx = -1;
				_vbl0Idx = vbl0Idx;
				_vbl1Idx = -1;
				_frctn = null;
				return;
			}
			/** Error. */
			_commandType = null;
			_equtn0Idx = _equtn1Idx = _vbl0Idx = _vbl1Idx = -1;
			_frctn = null;
			return;
		}

		/** If the rest is a number, it must be "x = 7, A *= 2, or A /= 2." */
		MyFraction frctn = new MyFraction(afterOperator);
		if (MyFraction.IsValid(frctn)) {
			switch (operator) {
			case SET:
				_commandType = CommandType.SET;
				_equtn0Idx = _equtn1Idx = -1;
				_vbl0Idx = vbl0Idx;
				_vbl1Idx = -1;
				_frctn = frctn;
				return;
			case MULTIPLY:
			case DIVIDE:
				if (equtn0Idx < 0) {
					/** Error. */
					_commandType = null;
					_equtn0Idx = _equtn1Idx = _vbl0Idx = _vbl1Idx = -1;
					_frctn = null;
					return;
				}
				_commandType = operator == Operator.MULTIPLY ? CommandType.MULTIPLY : CommandType.DIVIDE;
				_equtn0Idx = equtn0Idx;
				_equtn1Idx = _vbl0Idx = _vbl1Idx = -1;
				_frctn = frctn;
				return;
			case ADD:
			case SUBTRACT:
			case SWITCH:
			case ZERO:
			default:
				/** Error. */
				_commandType = null;
				_equtn0Idx = _equtn1Idx = _vbl0Idx = _vbl1Idx = -1;
				_frctn = null;
				return;
			}
		}

		/** Use A to Zero out x in B", "A x :: B */
		if (operator == Operator.ZERO) {
			final String[] beforeOperatorFields = beforeOperator.split("\\s+");
			if (beforeOperatorFields.length != 2) {
				/** Error. */
				_commandType = null;
				_equtn0Idx = _equtn1Idx = _vbl0Idx = _vbl1Idx = -1;
				_frctn = null;
				return;
			}
			/** The first field must be an equtn and the second field must be a vbl. */
			final int equtnIdxToUse = linearSystem.getEqutnIdx(beforeOperatorFields[0]);
			final int vblIdx = linearSystem.getVblIdx(beforeOperatorFields[1]);
			final String[] afterOperatorFields = afterOperator.split("\\s+");
			if (afterOperatorFields.length != 1) {
				/** Error. */
				_commandType = null;
				_equtn0Idx = _equtn1Idx = _vbl0Idx = _vbl1Idx = -1;
				_frctn = null;
				return;
			}
			final int equtnIdxToZeroOut = linearSystem.getEqutnIdx(afterOperator);
			if (equtnIdxToUse < 0 || vblIdx < 0 || equtnIdxToZeroOut < 0) {
				/** Error. */
				_commandType = null;
				_equtn0Idx = _equtn1Idx = _vbl0Idx = _vbl1Idx = -1;
				_frctn = null;
				return;
			}
			_commandType = CommandType.ZERO;
			_equtn0Idx = equtnIdxToUse;
			_equtn1Idx = equtnIdxToZeroOut;
			_vbl0Idx = vblIdx;
			_vbl1Idx = -1;
			_frctn = null;
			return;
		}

		/** Now the two switches. */
		if (operator == Operator.SWITCH) {
			final int equtn1Idx = linearSystem.getEqutnIdx(afterOperator);
			final int vbl1Idx = linearSystem.getVblIdx(afterOperator);
			final boolean isEqutn1 = equtn1Idx >= 0;
			final boolean isVbl1 = vbl1Idx >= 0;
			if (isEqutn1 == isVbl1 || isEqutn1 == isVbl0) {
				/** Error. */
				_commandType = null;
				_equtn0Idx = _equtn1Idx = _vbl0Idx = _vbl1Idx = -1;
				_frctn = null;
				return;
			}
			if (isEqutn1) {
				/** Switch equations. */
				_commandType = CommandType.SWITCH_EQUTNS;
				_equtn0Idx = equtn0Idx;
				_equtn1Idx = equtn1Idx;
				_vbl0Idx = _vbl1Idx = -1;
				_frctn = null;
				return;
			}
			/** Switch variables */
			_commandType = CommandType.SWITCH_VBLS;
			_equtn0Idx = _equtn1Idx = -1;
			_vbl0Idx = vbl0Idx;
			_vbl1Idx = vbl1Idx;
			_frctn = null;
			return;
		}

		/**
		 * We're left with the += and -=. Start by working backwards from the end to
		 * find an equation.
		 */
		frctn = null;
		int equtn1Idx = -1;
		for (int k = afterOperator.length() - 1; k >= 0; --k) {
			final String s = afterOperator.substring(k);
			equtn1Idx = linearSystem.getEqutnIdx(s);
			if (equtn1Idx >= 0) {
				frctn = k == 0 ? new MyFraction(1, 1) : new MyFraction(afterOperator.substring(0, k));
				break;
			}
		}
		if (equtn1Idx < 0 || !MyFraction.IsValid(frctn)) {
			/** Error. */
			_commandType = null;
			_equtn0Idx = _equtn1Idx = _vbl0Idx = _vbl1Idx = -1;
			_frctn = null;
			return;
		}
		if (operator == Operator.ADD) {
			_commandType = CommandType.ADD;
			_equtn0Idx = equtn0Idx;
			_equtn1Idx = equtn1Idx;
			_vbl0Idx = _vbl1Idx = -1;
			_frctn = frctn;
			return;
		}
		if (operator == Operator.SUBTRACT) {
			_commandType = CommandType.SUBTRACT;
			_equtn0Idx = equtn0Idx;
			_equtn1Idx = equtn1Idx;
			_vbl0Idx = _vbl1Idx = -1;
			_frctn = frctn;
			return;
		}
		/** Error. */
		_commandType = null;
		_equtn0Idx = _equtn1Idx = _vbl0Idx = _vbl1Idx = -1;
		_frctn = null;
	}

	public static void main(final String[] args) {
		final MyFraction myFraction = new MyFraction("-2 3 / 4");
		System.out.print(myFraction.getString(/* printFloat= */true));
	}
}
