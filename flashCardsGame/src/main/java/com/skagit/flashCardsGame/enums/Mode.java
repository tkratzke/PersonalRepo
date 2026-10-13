package com.skagit.flashCardsGame.enums;

import java.util.EnumSet;

public enum Mode {
    NORMAL("Run a Sequence of Quizzes."), //
    STEP("Step through the cards without regard to correctness."), //
    REGULAR_SORT_AND_DUMP("Read in the cards, regular-sort them, and run no quizzes."), //
    STRANGE_SORT_AND_DUMP("Read in the cards, strange-sort them, and run no quizzes."), //
    SWITCH_AND_DUMP("Switch Clues and Answers, sort them, and run no quizzes."), //
    SUPPRESS_ANSWERS_AND_DUMP("Suppress the Answers, sort them, and run no quizzes."); //

    final public static Mode[] _Values = values();

    final public static EnumSet<Mode> _DumpCardsAndAbort = EnumSet.of(//
	    REGULAR_SORT_AND_DUMP, //
	    STRANGE_SORT_AND_DUMP, //
	    SWITCH_AND_DUMP, //
	    SUPPRESS_ANSWERS_AND_DUMP //
    );

    final public static EnumSet<Mode> _StrangeSort = EnumSet.of(//
	    STRANGE_SORT_AND_DUMP //
    );

    final String _explanation;

    Mode(final String explanation) {
	_explanation = String.format("%s: %s", name(), explanation);
    }
}
