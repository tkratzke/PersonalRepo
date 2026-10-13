package com.skagit.linearSystemGenerator;

import java.util.EnumSet;

public enum CommandType {
	ADD("Add 2 of Equtn A to B", "B += 2A"), //
	SUBTRACT("Subtract 2 of Equtn A from B", "B -= 2A"), //
	ZERO("Use Equtn A to zero out x in Equtn B", "Use A to zero out x in B"), //
	MULTIPLY("Multiply Equtn A by 2", "A *= 2"), //
	DIVIDE("Divide Equtn A by 2", "A /= 2"), //
	SWITCH("Switch Equtn A and Equtn B or vbl x and vbl y", "\"A <-> B\" or \"x <-> y\""), //
	SET("Set x to 7", "x = 7"), //
	UNSET("Unset x", "x ="), //
	NEW("New Easy (or Hard) problem with 3 Equtns and 4 Vbls", "\"New: 3 4 Easy\" or \"New: 3 3 Hard\""), //
	QUIT("Quit", "Quit"), //
	HELP("Help", "Help"), //
	SYNTAX_ERROR(null, "x = 3y");//

	final public String _englishExample, _exampleCommand;

	final public static CommandType[] _CommandTypes = values();

	final public static EnumSet<CommandType> _HelpForInit = EnumSet.of(//
			QUIT, //
			NEW);

	final public static EnumSet<CommandType> _HelpForNonInit = EnumSet.of(//
			ADD, //
			SUBTRACT, //
			MULTIPLY, //
			DIVIDE, //
			ZERO, //
			SWITCH, //
			SET, //
			UNSET, //
			NEW, //
			QUIT, //
			HELP);

	CommandType(final String englishExample, final String exampleCommand) {
		_englishExample = englishExample;
		_exampleCommand = exampleCommand;
	}
}
