package com.skagit.flashCardsGame.enums;

import java.util.EnumSet;

public enum ChangeType {
	NOTHING_TO_SOMETHING("Initial Quiz Summary:"), //
	RESTART("Restarting Current Quiz:"), //
	CRITICAL_ONLY_WIN("Re-do Original Quiz:"), //
	MOVE_ON_WIN("Moving on!:"), //
	LOSS("Critical Only:"), //
	PARAMETERS_CHANGED("User Changed Properties:"), //
	NO_CHANGE("");

	final public String _reasonForChangeString;

	ChangeType(final String reasonForChangeString) {
		_reasonForChangeString = reasonForChangeString;
	}

	final public static ChangeType[] _Values = values();

	final public static EnumSet<ChangeType> _NewQuizSet = EnumSet.of(//
			CRITICAL_ONLY_WIN, //
			MOVE_ON_WIN, //
			LOSS, //
			NOTHING_TO_SOMETHING, //
			PARAMETERS_CHANGED, //
			RESTART//
	);

	final public static EnumSet<ChangeType> _ReallyNewQuizSet = EnumSet.of(//
			MOVE_ON_WIN, //
			NOTHING_TO_SOMETHING, //
			PARAMETERS_CHANGED //
	);

}
