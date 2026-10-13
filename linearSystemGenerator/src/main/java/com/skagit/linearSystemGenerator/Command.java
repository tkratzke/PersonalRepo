package com.skagit.linearSystemGenerator;

import com.skagit.utils.NumericalRoutines;

public class Command {
	public final CommandType _commandType;
	final String _equtn0Name, _equtn1Name;
	final String _vbl0Name, _vbl1Name;
	final double _d0, _d1;
	final Boolean _easy;

	public Command(final CommandType commandType, final String equtn0Name, final String equtn1Name,
			final String vbl0Name, final String vbl1Name, final Boolean easy, final double d0, final double d1) {
		_commandType = commandType;
		_equtn0Name = equtn0Name;
		_equtn1Name = equtn1Name;
		_vbl0Name = vbl0Name;
		_vbl1Name = vbl1Name;
		_easy = easy;
		_d0 = d0;
		_d1 = d1;
	}

	/** For syntax errors: */
	public Command(final String[] fields) {
		_commandType = CommandType.SYNTAX_ERROR;
		_equtn0Name = _equtn1Name = _vbl0Name = _vbl1Name = null;
		_easy = null;
		_d0 = _d1 = Double.NaN;
	}

	public String getString() {
		final StringBuilder s = new StringBuilder().append(String.format("%s", _commandType._englishExample));
		if (_equtn0Name != null) {
			s.append(String.format(", Equtn0=%s", _equtn0Name));
		}
		if (_equtn1Name != null) {
			s.append(String.format(", Equtn1=%s", _equtn1Name));
		}
		if (_vbl0Name != null) {
			s.append(String.format(", Vbl0=%s", _vbl0Name));
		}
		if (_vbl1Name != null) {
			s.append(String.format(", Vbl1=%s", _vbl1Name));
		}
		if (Double.isFinite(_d0)) {
			s.append(String.format(", d0=%s", NumericalRoutines.getString(_d0)));
		}
		if (Double.isFinite(_d1)) {
			s.append(String.format(", d1=%s", NumericalRoutines.getString(_d1)));
		}
		if (_easy != null) {
			s.append(String.format(", Easy=%s", Boolean.toString(_easy)));
		}
		return s.toString();
	}

	@Override
	public String toString() {
		return getString();
	}

	/**
	 * <pre>
	ZERO("Use Equtn A to zero out vbl x in Equtn B", "Use A to zero out x in B"), //
	NEW("New Easy (or Hard) problem with 3 Equtns and 4 vbls", "\"New: 3 4 Easy\" or \"New: 3 4 Hard\""), //
	ADD("Add 2 of Equtn A to B", "B += 2A"), //
	SUBTRACT("Subtract 2 of Equtn A from B", "B -= 2A"), //
	MULTIPLY("Multiply Equtn A by 2", "A *= 2"), //
	DIVIDE("Divide Equtn A by 2", "A /= 2"), //
	SWITCH("Switch Equtn A and Equtn B or vbl x and vbl y", "\"A <-> B\" or \"x <-> y\""), //
	SET("Set x to 7", "x = 7"), //
	UNSET("Unset x", "x ="), //
	QUIT("Quit", "Quit"), //
	HELP("Help", "Help"), //
	SYNTAX_ERROR(null, "x = 3y");//
	 * </pre>
	 */
	public static Command getCommand(String line, final LinearSystemGenerator.LinearSystem ls) {
		/** Strip the ending period if it's there. */
		line = line.trim();
		final int lineLen = line.length();
		if (lineLen == 0) {
			return getSimpleCommand(CommandType.SYNTAX_ERROR);
		}
		if (line.charAt(lineLen - 1) == '.') {
			line = line.substring(0, lineLen - 1);
		}
		final String[] fields = line.split("\\s+");
		final int nFields = fields.length;
		if (nFields == 0) {
			return getSimpleCommand(CommandType.SYNTAX_ERROR);
		}
		final String field0 = fields[0];
		if (nFields == 1) {
			final char c = Character.toLowerCase(field0.charAt(0));
			switch (c) {
			case 'h':
				return getSimpleCommand(CommandType.HELP);
			case 'q':
				return getSimpleCommand(CommandType.QUIT);
			default:
				return getSimpleCommand(CommandType.SYNTAX_ERROR);
			}
		}
		final String field1 = fields[1];
		if (nFields == 2) {
			/** x = */
			final NumberAndName numberAndName0 = new NumberAndName(ls, field0, /* isEqutn= */false);
			final String name0 = numberAndName0._nameString;
			if ((name0 != null) && "=".equals(field1)) {
				return new Command(CommandType.SET, null, null, name0, null, null, Double.NaN, Double.NaN);
			}
			return getSimpleCommand(CommandType.SYNTAX_ERROR);
		}
		final String field2 = fields[2];
		if ("=".equals(field1)) {
			/** x = 7 */
			final NumberAndName numberAndName0 = new NumberAndName(ls, field0, /* isEqutn= */false);
			final NumberAndName numberAndName2 = new NumberAndName(ls, field2, /* isEqutn= */false);
			final String name0 = numberAndName0._nameString;
			final String name2 = numberAndName2._nameString;
			if (name0 == null || name2 != null) {
				return getSimpleCommand(CommandType.SYNTAX_ERROR);
			}
			final double d0 = numberAndName0._d;
			final double d2 = numberAndName2._d;
			if (d0 != 1d || !Double.isFinite(d2)) {
				return getSimpleCommand(CommandType.SYNTAX_ERROR);
			}
			return new Command(CommandType.SET, null, null, name0, null, null, d2, Double.NaN);
		}
		if ((nFields == 3) && ("+=".equals(field1) || "-=".equals(field1))) {
			/** B += 2A and B -= 2A */
			final NumberAndName numberAndName0 = new NumberAndName(ls, field0, /* isEqutn= */true);
			final NumberAndName numberAndName2 = new NumberAndName(ls, field2, /* isEqutn= */true);
			final String name0 = numberAndName0._nameString;
			final String name2 = numberAndName2._nameString;
			if (name0 == null || name2 == null) {
				return getSimpleCommand(CommandType.SYNTAX_ERROR);
			}
			final double d0 = numberAndName0._d;
			final double d2 = numberAndName2._d;
			if (d0 != 1d || !Double.isFinite(d2)) {
				return getSimpleCommand(CommandType.SYNTAX_ERROR);
			}
			final CommandType commandType = "+=".equals(field1) ? CommandType.ADD : CommandType.SUBTRACT;
			return new Command(commandType, name0, name2, null, null, null, d2, Double.NaN);
		}
		if ((nFields == 3) && ("*=".equals(field1) || "/=".equals(field1))) {
			/** A *= 2 and A /= 2 */
			final NumberAndName numberAndName0 = new NumberAndName(ls, field0, /* isEqutn= */true);
			final NumberAndName numberAndName2 = new NumberAndName(ls, field2, /* isEqutn= */true);
			final String name0 = numberAndName0._nameString;
			final String name2 = numberAndName2._nameString;
			if (name0 == null || name2 != null) {
				return getSimpleCommand(CommandType.SYNTAX_ERROR);
			}
			final double d0 = numberAndName0._d;
			final double d2 = numberAndName2._d;
			if (d0 != 1d || !Double.isFinite(d2) || d2 == 0d) {
				return getSimpleCommand(CommandType.SYNTAX_ERROR);
			}
			final CommandType commandType = "*=".equals(field1) ? CommandType.MULTIPLY : CommandType.DIVIDE;
			return new Command(commandType, name0, name2, null, null, null, d2, Double.NaN);
		}
		if (nFields == 3 && "<->".equals(field1)) {
			/** A <-> B and x <-> y */
			NumberAndName numberAndName0 = new NumberAndName(ls, field0, /* isEqutn= */true);
			final String name0 = numberAndName0._nameString;
			final boolean isEqutn = name0 != null;
			if (!isEqutn) {
				numberAndName0 = new NumberAndName(ls, field0, /* isEqutn= */false);
			}
			if (name0 == null) {
				return getSimpleCommand(CommandType.SYNTAX_ERROR);
			}
			final NumberAndName numberAndName2 = new NumberAndName(ls, field2, isEqutn);
			final String name2 = numberAndName2._nameString;
			if (name2 == null) {
				return getSimpleCommand(CommandType.SYNTAX_ERROR);
			}
			final double d0 = numberAndName0._d;
			final double d2 = numberAndName2._d;
			if (d0 != 1d || d2 != 1d) {
				return getSimpleCommand(CommandType.SYNTAX_ERROR);
			}
			if (isEqutn) {
				return new Command(CommandType.SWITCH, name0, name2, null, null, null, Double.NaN, Double.NaN);
			}
			return new Command(CommandType.SWITCH, null, null, name0, name2, null, Double.NaN, Double.NaN);
		}
		/** New and Use are left. */
		final char c0 = Character.toLowerCase(field0.charAt(0));
		switch (c0) {
		case 'n':
			boolean easy = true;
			int m = -1, n = -1;
			for (int k = 1; k < nFields; ++k) {
				final String fieldK = fields[k].toLowerCase();
				final int fieldKLen = fieldK.length();
				/** Find if easy or hard. */
				for (int iPass = 0; iPass < 2; ++iPass) {
					final String s0 = iPass == 0 ? "easy" : "hard";
					if (fieldKLen > s0.length()) {
						continue;
					}
					if (s0.startsWith(fieldK.toLowerCase())) {
						easy = iPass == 0;
					}
					break;
				}
				final Integer gottenInteger = getInteger(fieldK);
				if (gottenInteger != null) {
					if (m < 0) {
						m = gottenInteger;
					} else {
						n = gottenInteger;
					}
				}
			}
			if (n < 0) {
				return getSimpleCommand(CommandType.SYNTAX_ERROR);
			}
			return new Command(CommandType.NEW, null, null, null, null, easy, m, n);
		case 'u':
			String equtn0Name = null;
			String vblName = null;
			String equtn1Name = null;
			for (int k = 1; k < nFields; ++k) {
				final String fieldK = fields[k];
				final boolean lookingForEqutn = equtn0Name == null || vblName != null;
				final NumberAndName numberAndName = new NumberAndName(ls, fieldK, /* isEqutn= */lookingForEqutn);
				final String name = numberAndName._nameString;
				if (name == null || numberAndName._d != 1d) {
					continue;
				}
				if (equtn0Name == null) {
					equtn0Name = name;
				} else if (vblName == null) {
					vblName = name;
				} else {
					equtn1Name = name;
					break;
				}
			}
			if (equtn1Name == null) {
				return getSimpleCommand(CommandType.SYNTAX_ERROR);
			}
			return new Command(CommandType.ZERO, equtn0Name, equtn1Name, vblName, null, null, Double.NaN, Double.NaN);
		default:
			return getSimpleCommand(CommandType.SYNTAX_ERROR);
		}
	}

	static Command getSimpleCommand(final CommandType commandType) {
		return new Command(commandType, null, null, null, null, null, Double.NaN, Double.NaN);
	}

	static Integer getInteger(final String s) {
		try {
			return Integer.parseInt(s);
		} catch (final NumberFormatException e) {
		}
		return null;
	}

	private static class NumberAndName {
		final double _d;
		final String _nameString;

		NumberAndName(final LinearSystemGenerator.LinearSystem ls, final String field, final boolean isEqutn) {
			/** We assume there is no white space in field. */
			/** Find the maximum tail that is a legitimate equation or variable. */
			final int len = field.length();
			for (int k = 0; k < len; ++k) {
				final String nameString = field.substring(k);
				if ((isEqutn && ls.getEqutnIdx(nameString) < 0) || (!isEqutn && ls.getVblIdx(nameString) < 0)) {
					continue;
				}
				/** We have nameString. */
				final String numberString = field.substring(0, k);
				if (k == 0) {
					_nameString = nameString;
					_d = 1d;
				} else {
					_d = getNumber(numberString);
					_nameString = !Double.isFinite(_d) ? null : nameString;
				}
				return;
			}
			/** We never got a name. How about just a number? */
			_nameString = null;
			_d = getNumber(field);
		}
	}

	private static double getNumber(final String s) {
		try {
			final double d = Integer.parseInt(s);
			return d;
		} catch (final NumberFormatException e) {
		}
		/** Not a number. We allow / and *. */
		final String[] fields = s.split("[/\\*]");
		if (fields.length != 2) {
			return Double.NaN;
		}
		final String field0 = fields[0];
		final String field1 = fields[1];
		double d0 = Double.NaN;
		double d1 = Double.NaN;
		try {
			d0 = Integer.parseInt(field0);
			d1 = Integer.parseInt(field1);
		} catch (final NumberFormatException e) {
			return Double.NaN;
		}
		final char operator = s.charAt(field0.length());
		if (operator == '/') {
			return d1 == 0d ? Double.NaN : (d0 / d1);
		}
		return d0 * d1;
	}

}
