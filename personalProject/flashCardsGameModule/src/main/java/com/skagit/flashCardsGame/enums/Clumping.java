package com.skagit.flashCardsGame.enums;

public enum Clumping {
    BY_CLUE("Cards are grouped by Clue."), //
    BY_ANSWER("Cards are grouped by Answer."), //
    NO_CLUMPING("Cards are not clumped by Clue or Answer.");

    final public static Clumping[] _Values = values();

    final String _explanation;

    Clumping(final String explanation) {
	_explanation = String.format("%s: %s", name(), explanation);
    }
};
