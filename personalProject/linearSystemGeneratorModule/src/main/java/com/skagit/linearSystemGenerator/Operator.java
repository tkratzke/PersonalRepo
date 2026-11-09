package com.skagit.linearSystemGenerator;

public enum Operator {
	ADD("+="), //
	SUBTRACT("-="), //
	MULTIPLY("*="), //
	DIVIDE("/="), //
	TELL("=?"), //
	SET("="), //
	ZERO("::"), //
	SWITCH("<->");

	final public String _s;

	Operator(final String s) {
		_s = s;
	}

}
