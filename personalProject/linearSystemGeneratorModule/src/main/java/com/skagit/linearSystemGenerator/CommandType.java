package com.skagit.linearSystemGenerator;

import java.util.EnumSet;

public enum CommandType {
	ADD(Operator.ADD, "B = B + 2A", "B += 2A"), //
	SUBTRACT(Operator.SUBTRACT, "B = B - 2A", "B -= 2A"), //
	ZERO(Operator.ZERO, "Use A to Zero out x in B", "A x :: B"), //
	MULTIPLY(Operator.MULTIPLY, "A = 2A", "A *= 2"), //
	DIVIDE(Operator.DIVIDE, "A = A/2", "A /= 2"), //
	SWITCH_EQUTNS(Operator.SWITCH, "Switch Equtns A and B", "A <-> B"), //
	SWITCH_VBLS(Operator.SWITCH, "Switch vbls x and vbl y", "x <-> y"), //
	SET(Operator.SET, "Set x to 7", "x = 7"), //
	UNSET(Operator.SET, "Unset x", "x ="), //
	QUIT(null, "Quit", "Quit"), //
	HELP(null, "Help", "Help");//

	final public Operator _operator;
	final public String _englishExample, _commandExample;

	final public static CommandType[] _CommandTypes = values();

	final public static EnumSet<CommandType> _None = EnumSet.noneOf(CommandType.class);

	final public static EnumSet<CommandType> _AllCommandTypes = EnumSet.of(//
			ADD, //
			SUBTRACT, //
			MULTIPLY, //
			DIVIDE, //
			SET, //
			UNSET, //
			ZERO, //
			SWITCH_EQUTNS, //
			SWITCH_VBLS, //
			QUIT, //
			HELP);

	CommandType(final Operator operator, final String englishExample, final String commandExample) {
		_operator = operator;
		_englishExample = englishExample;
		_commandExample = commandExample;
	}
}
